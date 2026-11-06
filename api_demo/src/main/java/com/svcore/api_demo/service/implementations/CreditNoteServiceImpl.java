package com.svcore.api_demo.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.CreditNoteDTO;
import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.SaleDetail;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.MovementType;
import com.svcore.api_demo.repositories.DteRepository;
import com.svcore.api_demo.repositories.InventoryMovementRepository;
import com.svcore.api_demo.repositories.ProductRepository;
import com.svcore.api_demo.repositories.SaleRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.CreditNoteService;
import com.svcore.api_demo.service.DteService;
import com.svcore.api_demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreditNoteServiceImpl implements CreditNoteService {

	private static final BigDecimal IVA_RATE = new BigDecimal("0.13");

	private final DteRepository dteRepository;
	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final InventoryMovementRepository movementRepository;
	private final DteService dteService;
	private final UserService userService;
	private final AuditService auditService;

	@Override
	@Transactional
	public DteViewDTO create(CreditNoteDTO info) {
		Dte original = dteRepository.findById(info.getDteId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
		if (original.getType() != DteType.CREDITO_FISCAL) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"La Nota de Crédito solo aplica a Comprobantes de Crédito Fiscal");
		}
		if (original.getStatus() != DteStatus.SELLADO) {
			throw new BusinessException(HttpStatus.CONFLICT, "El Crédito Fiscal debe estar sellado por Hacienda");
		}

		Sale originalSale = original.getSale();
		Map<UUID, SaleDetail> soldByProduct = new HashMap<>();
		originalSale.getDetails().forEach(d -> soldByProduct.put(d.getProduct().getId(), d));
		Map<UUID, Integer> alreadyCredited = creditedQuantities(original);

		User user = userService.getCurrentUser();
		Sale note = new Sale();
		note.setSeller(user);
		note.setCustomer(originalSale.getCustomer());
		note.setCashRegister(originalSale.getCashRegister());
		note.setDocType(DteType.NOTA_CREDITO);
		note.setPaymentMethod(originalSale.getPaymentMethod());
		note.setNotes(info.getReason().trim());

		Map<UUID, Integer> requested = new LinkedHashMap<>();
		info.getItems().forEach(item -> requested.merge(item.getProductId(), item.getQuantity(), Integer::sum));

		for (Map.Entry<UUID, Integer> item : requested.entrySet()) {
			SaleDetail sold = soldByProduct.get(item.getKey());
			if (sold == null) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "Un producto de la nota no está en el Crédito Fiscal");
			}
			int available = sold.getQuantity() - alreadyCredited.getOrDefault(item.getKey(), 0);
			if (item.getValue() > available) {
				throw new BusinessException(HttpStatus.CONFLICT, String.format(
						"Solo puedes acreditar hasta %d unidad(es) de %s", available, sold.getProduct().getName()));
			}
			// Se acredita al mismo precio con el que se vendió
			SaleDetail detail = new SaleDetail();
			detail.setProduct(sold.getProduct());
			detail.setQuantity(item.getValue());
			detail.setUnitPrice(sold.getUnitPrice());
			detail.setLineTotal(sold.getUnitPrice().multiply(BigDecimal.valueOf(item.getValue())));
			note.addDetail(detail);
		}

		BigDecimal subtotal = note.getDetails().stream().map(SaleDetail::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
		BigDecimal tax = subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
		note.setSubtotal(subtotal);
		note.setDiscount(BigDecimal.ZERO.setScale(2));
		note.setTax(tax);
		note.setTotal(subtotal.add(tax));
		note.setSaleNumber(saleRepository.findMaxSaleNumber() + 1);
		saleRepository.save(note);

		if (info.isReturnToStock()) {
			for (SaleDetail detail : note.getDetails()) {
				Product product = productRepository.findByIdForUpdate(detail.getProduct().getId()).orElseThrow();
				product.setStock(product.getStock() + detail.getQuantity());
				movementRepository.save(new InventoryMovement(product, detail.getQuantity(),
						MovementType.ENTRADA_DEVOLUCION, note, user));
			}
		}

		DteViewDTO dte = dteService.issue(note, original);
		auditService.record(AuditAction.NOTA_CREDITO_EMITIDA, "dte", dte.getId(),
				"Nota de crédito sobre " + original.getControlNumber() + " por " + note.getTotal());
		return dte;
	}

	/** Cantidades ya acreditadas por notas de crédito vigentes (no anuladas). */
	private Map<UUID, Integer> creditedQuantities(Dte original) {
		Map<UUID, Integer> credited = new HashMap<>();
		for (Dte note : dteRepository.findAllByRelatedDteAndStatusNot(original, DteStatus.ANULADO)) {
			note.getSale().getDetails().forEach(d -> credited.merge(d.getProduct().getId(), d.getQuantity(), Integer::sum));
		}
		return credited;
	}
}
