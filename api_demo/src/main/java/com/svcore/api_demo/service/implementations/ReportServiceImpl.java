package com.svcore.api_demo.service.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.DailySalesDTO;
import com.svcore.api_demo.models.dtos.IvaBookRowDTO;
import com.svcore.api_demo.models.dtos.PaymentSalesDTO;
import com.svcore.api_demo.models.dtos.SalesSummaryDTO;
import com.svcore.api_demo.models.dtos.TopProductDTO;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.SaleDetail;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;
import com.svcore.api_demo.repositories.DteRepository;
import com.svcore.api_demo.service.ReportService;
import com.svcore.api_demo.utils.CsvUtils;

import lombok.RequiredArgsConstructor;

/**
 * Reportes sobre los documentos emitidos. Reglas:
 * los DTE anulados no suman; las notas de crédito restan; los rechazados y en contingencia sí son ventas
 * registradas (la venta ocurrió aunque el sello esté pendiente).
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private static final long MAX_RANGE_DAYS = 366;

	private final DteRepository dteRepository;

	@Override
	@Transactional(readOnly = true)
	public SalesSummaryDTO summary(LocalDate from, LocalDate to) {
		LocalDate start = start(from);
		LocalDate end = end(to);
		List<Dte> dtes = load(start, end);

		long sales = 0, creditNotes = 0, invalidated = 0;
		BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO, total = BigDecimal.ZERO;
		BigDecimal invalidatedTotal = BigDecimal.ZERO;
		Map<String, Long> byStatus = new LinkedHashMap<>();
		Map<String, Long> byType = new LinkedHashMap<>();

		for (Dte dte : dtes) {
			byStatus.merge(dte.getStatus().name(), 1L, Long::sum);
			byType.merge(dte.getType().getShortName(), 1L, Long::sum);
			Sale sale = dte.getSale();
			if (dte.getStatus() == DteStatus.ANULADO) {
				invalidated++;
				invalidatedTotal = invalidatedTotal.add(sale.getTotal());
				continue;
			}
			int sign = sign(dte);
			if (sign < 0) creditNotes++; else sales++;
			subtotal = subtotal.add(sale.getSubtotal().multiply(BigDecimal.valueOf(sign)));
			tax = tax.add(sale.getTax().multiply(BigDecimal.valueOf(sign)));
			total = total.add(sale.getTotal().multiply(BigDecimal.valueOf(sign)));
		}
		return new SalesSummaryDTO(start, end, sales, creditNotes, invalidated, subtotal, tax, total, invalidatedTotal,
				byStatus, byType);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DailySalesDTO> salesByDay(LocalDate from, LocalDate to) {
		Map<LocalDate, BigDecimal[]> days = new TreeMap<>();
		Map<LocalDate, Long> counts = new TreeMap<>();
		for (Dte dte : valid(load(start(from), end(to)))) {
			LocalDate day = dte.getIssuedAt().toLocalDate();
			BigDecimal[] sums = days.computeIfAbsent(day, d -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
			BigDecimal sign = BigDecimal.valueOf(sign(dte));
			sums[0] = sums[0].add(dte.getSale().getSubtotal().multiply(sign));
			sums[1] = sums[1].add(dte.getSale().getTax().multiply(sign));
			sums[2] = sums[2].add(dte.getSale().getTotal().multiply(sign));
			counts.merge(day, 1L, Long::sum);
		}
		List<DailySalesDTO> result = new ArrayList<>();
		days.forEach((day, sums) -> result.add(new DailySalesDTO(day, counts.get(day), sums[0], sums[1], sums[2])));
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentSalesDTO> salesByPayment(LocalDate from, LocalDate to) {
		Map<PaymentMethod, long[]> counts = new EnumMap<>(PaymentMethod.class);
		Map<PaymentMethod, BigDecimal> totals = new EnumMap<>(PaymentMethod.class);
		for (Dte dte : valid(load(start(from), end(to)))) {
			if (sign(dte) < 0) continue; // las notas de crédito no son cobros
			PaymentMethod method = dte.getSale().getPaymentMethod();
			counts.computeIfAbsent(method, m -> new long[1])[0]++;
			totals.merge(method, dte.getSale().getTotal(), BigDecimal::add);
		}
		List<PaymentSalesDTO> result = new ArrayList<>();
		totals.forEach((method, total) -> result.add(
				new PaymentSalesDTO(method, method.getLabel(), counts.get(method)[0], total)));
		result.sort(Comparator.comparing(PaymentSalesDTO::getTotal).reversed());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TopProductDTO> topProducts(LocalDate from, LocalDate to, int limit) {
		Map<UUID, TopProductDTO> products = new LinkedHashMap<>();
		for (Dte dte : valid(load(start(from), end(to)))) {
			int sign = sign(dte);
			for (SaleDetail detail : dte.getSale().getDetails()) {
				TopProductDTO row = products.computeIfAbsent(detail.getProduct().getId(), id -> new TopProductDTO(id,
						detail.getProduct().getCode(), detail.getProduct().getName(), 0, BigDecimal.ZERO));
				row.setQuantity(row.getQuantity() + (long) sign * detail.getQuantity());
				row.setSubtotal(row.getSubtotal().add(detail.getLineTotal().multiply(BigDecimal.valueOf(sign))));
			}
		}
		return products.values().stream()
				.sorted(Comparator.comparing(TopProductDTO::getSubtotal).reversed())
				.limit(Math.min(Math.max(1, limit), 100))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<IvaBookRowDTO> ivaBook(LocalDate from, LocalDate to, DteType type) {
		return load(start(from), end(to)).stream()
				.filter(dte -> type == null || dte.getType() == type)
				.map(dte -> {
					Sale sale = dte.getSale();
					BigDecimal sign = BigDecimal.valueOf(dte.getStatus() == DteStatus.ANULADO ? 0 : sign(dte));
					return new IvaBookRowDTO(dte.getIssuedAt(), dte.getType().getShortName(), dte.getControlNumber(),
							dte.getGenerationCode(), dte.getReceptionStamp(), dte.getReceiverName(), dte.getReceiverNit(),
							dte.getReceiverNrc(), sale.getSubtotal().multiply(sign), sale.getTax().multiply(sign),
							sale.getTotal().multiply(sign), dte.getStatus());
				})
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public String ivaBookCsv(LocalDate from, LocalDate to, DteType type) {
		List<String> header = List.of("Fecha", "Tipo", "Número de control", "Código de generación", "Sello de recepción",
				"Cliente", "NIT", "NRC", "Gravado", "IVA", "Total", "Estado");
		List<List<Object>> rows = ivaBook(from, to, type).stream()
				.map(r -> List.<Object>of(r.getIssuedAt().toString().replace('T', ' '), r.getType(), r.getControlNumber(),
						r.getGenerationCode(), nullToEmpty(r.getReceptionStamp()), r.getCustomerName(),
						nullToEmpty(r.getCustomerNit()), nullToEmpty(r.getCustomerNrc()), r.getSubtotal().toPlainString(),
						r.getTax().toPlainString(), r.getTotal().toPlainString(), r.getStatus().name()))
				.toList();
		return CsvUtils.document(header, rows);
	}

	// ------------------------------------------------------------------

	private List<Dte> load(LocalDate start, LocalDate end) {
		if (end.isBefore(start)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la inicial");
		}
		if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "El rango máximo de un reporte es de un año");
		}
		return dteRepository.findForReport(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
	}

	private static List<Dte> valid(List<Dte> dtes) {
		return dtes.stream().filter(dte -> dte.getStatus() != DteStatus.ANULADO).toList();
	}

	/** Las notas de crédito restan. */
	private static int sign(Dte dte) {
		return dte.getType() == DteType.NOTA_CREDITO ? -1 : 1;
	}

	private static LocalDate start(LocalDate from) {
		return from != null ? from : LocalDate.now().withDayOfMonth(1);
	}

	private static LocalDate end(LocalDate to) {
		return to != null ? to : LocalDate.now();
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
