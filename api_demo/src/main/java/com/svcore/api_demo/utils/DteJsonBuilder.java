package com.svcore.api_demo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Customer;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.EmitterConfig;
import com.svcore.api_demo.models.entities.InvalidationEvent;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.SaleDetail;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;

import tools.jackson.databind.json.JsonMapper;

/**
 * Arma los documentos JSON que se firman y transmiten a Hacienda: DTE (Factura v1, Crédito Fiscal v3,
 * Nota de Crédito v3), evento de contingencia (v3) y evento de invalidación (v2).
 * <p>
 * La estructura sigue los esquemas JSON publicados por el Ministerio de Hacienda (normativa DTE v1.1).
 * Antes de pasar a producción hay que validar la salida contra los archivos de esquema oficiales vigentes.
 */
@Component
public class DteJsonBuilder {

	private static final BigDecimal IVA_RATE = new BigDecimal("0.13");
	private static final BigDecimal IVA_FACTOR = new BigDecimal("1.13");
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	/** Catálogo de tributos: 20 = IVA 13 %. */
	private static final String IVA_CODE = "20";
	private static final String IVA_DESCRIPTION = "Impuesto al Valor Agregado 13%";
	/** Catálogo de unidades de medida: 59 = unidad. */
	private static final int UNIT = 59;
	/** Catálogo de tipo de ítem: 1 = bienes. */
	private static final int ITEM_GOODS = 1;
	/** Catálogo de tipo de documento de identificación: 36 = NIT, 37 = otro. */
	private static final String DOC_NIT = "36";
	private static final String DOC_OTHER = "37";
	/** Tipo de generación del documento relacionado: 2 = electrónico. */
	private static final int RELATED_ELECTRONIC = 2;

	private final JsonMapper mapper = JsonMapper.builder().build();

	/** Versión del esquema JSON según el tipo de DTE. */
	public static int schemaVersion(DteType type) {
		return type == DteType.FACTURA ? 1 : 3;
	}

	// ------------------------------------------------------------------
	// DTE
	// ------------------------------------------------------------------

	public String buildDte(Dte dte, EmitterConfig emitter) {
		DteType type = dte.getType();
		Sale sale = dte.getSale();

		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("identificacion", identification(dte));
		doc.put("documentoRelacionado", type == DteType.NOTA_CREDITO ? relatedDocuments(dte) : null);
		doc.put("emisor", emitter(emitter, type));
		doc.put("receptor", type == DteType.FACTURA ? consumerReceiver(dte, sale.getCustomer())
				: taxpayerReceiver(dte, sale.getCustomer()));
		if (type != DteType.NOTA_CREDITO) {
			doc.put("otrosDocumentos", null);
		}
		doc.put("ventaTercero", null);
		doc.put("cuerpoDocumento", body(dte, sale));
		doc.put("resumen", summary(type, sale));
		doc.put("extension", null);
		doc.put("apendice", null);
		return write(doc);
	}

	private Map<String, Object> identification(Dte dte) {
		ContingencyEvent event = dte.getContingencyEvent();
		boolean contingency = dte.getOperationType() == 2 && event != null;

		Map<String, Object> id = new LinkedHashMap<>();
		id.put("version", schemaVersion(dte.getType()));
		id.put("ambiente", dte.getEnvironment());
		id.put("tipoDte", dte.getType().getCode());
		id.put("numeroControl", dte.getControlNumber());
		id.put("codigoGeneracion", dte.getGenerationCode());
		id.put("tipoModelo", dte.getModelType());
		id.put("tipoOperacion", dte.getOperationType());
		id.put("tipoContingencia", contingency ? event.getContingencyType() : null);
		id.put("motivoContin", contingency && event.getContingencyType() == 5 ? event.getReason() : null);
		id.put("fecEmi", dte.getIssuedAt().format(DATE));
		id.put("horEmi", dte.getIssuedAt().format(TIME));
		id.put("tipoMoneda", "USD");
		return id;
	}

	private List<Map<String, Object>> relatedDocuments(Dte dte) {
		Dte related = dte.getRelatedDte();
		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("tipoDocumento", related.getType().getCode());
		doc.put("tipoGeneracion", RELATED_ELECTRONIC);
		doc.put("numeroDocumento", related.getGenerationCode());
		doc.put("fechaEmision", related.getIssuedAt().format(DATE));
		return List.of(doc);
	}

	private Map<String, Object> emitter(EmitterConfig c, DteType type) {
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("nit", digits(c.getNit()));
		e.put("nrc", digits(c.getNrc()));
		e.put("nombre", c.getBusinessName());
		e.put("codActividad", c.getEconomicActivityCode());
		e.put("descActividad", c.getEconomicActivity());
		e.put("nombreComercial", c.getTradeName());
		e.put("tipoEstablecimiento", c.getEstablishmentType());
		e.put("direccion", address(c.getDepartmentCode(), c.getMunicipalityCode(), c.getAddressDetail()));
		e.put("telefono", c.getPhone());
		e.put("correo", c.getEmail());
		if (type == DteType.FACTURA || type == DteType.CREDITO_FISCAL) {
			e.put("codEstableMH", c.getEstablishmentCode());
			e.put("codEstable", c.getEstablishmentCode());
			e.put("codPuntoVentaMH", c.getPointOfSaleCode());
			e.put("codPuntoVenta", c.getPointOfSaleCode());
		}
		return e;
	}

	/** Receptor de la Factura: puede ser Consumidor Final sin datos. Usa la copia guardada en el DTE. */
	private Map<String, Object> consumerReceiver(Dte dte, Customer customer) {
		Map<String, Object> r = new LinkedHashMap<>();
		boolean identified = customer != null;
		r.put("tipoDocumento", identified && dte.getReceiverNit() != null ? DOC_NIT : null);
		r.put("numDocumento", identified ? digits(dte.getReceiverNit()) : null);
		r.put("nrc", identified ? digits(dte.getReceiverNrc()) : null);
		r.put("nombre", identified ? dte.getReceiverName() : null);
		r.put("codActividad", identified ? customer.getEconomicActivityCode() : null);
		r.put("descActividad", identified ? customer.getEconomicActivity() : null);
		r.put("direccion", identified ? address(customer.getDepartmentCode(), customer.getMunicipalityCode(),
				dte.getReceiverAddress()) : null);
		r.put("telefono", identified ? customer.getPhone() : null);
		r.put("correo", identified ? dte.getReceiverEmail() : null);
		return r;
	}

	/** Receptor del Crédito Fiscal y la Nota de Crédito: contribuyente con NIT y NRC. */
	private Map<String, Object> taxpayerReceiver(Dte dte, Customer customer) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("nit", digits(dte.getReceiverNit()));
		r.put("nrc", digits(dte.getReceiverNrc()));
		r.put("nombre", dte.getReceiverName());
		r.put("codActividad", customer != null ? customer.getEconomicActivityCode() : null);
		r.put("descActividad", customer != null ? customer.getEconomicActivity() : null);
		r.put("nombreComercial", null);
		r.put("direccion", address(customer != null ? customer.getDepartmentCode() : null,
				customer != null ? customer.getMunicipalityCode() : null, dte.getReceiverAddress()));
		r.put("telefono", customer != null ? customer.getPhone() : null);
		r.put("correo", dte.getReceiverEmail());
		return r;
	}

	private List<Map<String, Object>> body(Dte dte, Sale sale) {
		DteType type = dte.getType();
		List<Map<String, Object>> items = new ArrayList<>();
		int number = 1;
		for (SaleDetail detail : sale.getDetails()) {
			// En la Factura los precios incluyen IVA; en el Crédito Fiscal y la Nota de Crédito van sin IVA
			boolean includesIva = type == DteType.FACTURA;
			BigDecimal unitPrice = includesIva
					? detail.getUnitPrice().multiply(IVA_FACTOR).setScale(4, RoundingMode.HALF_UP)
					: detail.getUnitPrice();
			BigDecimal taxed = includesIva
					? detail.getLineTotal().multiply(IVA_FACTOR).setScale(4, RoundingMode.HALF_UP)
					: detail.getLineTotal();

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("numItem", number++);
			item.put("tipoItem", ITEM_GOODS);
			item.put("numeroDocumento", type == DteType.NOTA_CREDITO ? dte.getRelatedDte().getGenerationCode() : null);
			item.put("cantidad", detail.getQuantity());
			item.put("codigo", detail.getProduct().getCode());
			item.put("codTributo", null);
			item.put("uniMedida", UNIT);
			item.put("descripcion", detail.getProduct().getName());
			item.put("precioUni", unitPrice);
			item.put("montoDescu", ZERO);
			item.put("ventaNoSuj", ZERO);
			item.put("ventaExenta", ZERO);
			item.put("ventaGravada", taxed);
			item.put("tributos", includesIva ? null : List.of(IVA_CODE));
			if (type != DteType.NOTA_CREDITO) {
				item.put("psv", ZERO);
				item.put("noGravado", ZERO);
			}
			if (includesIva) {
				item.put("ivaItem", detail.getLineTotal().multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP));
			}
			items.add(item);
		}
		return items;
	}

	private Map<String, Object> summary(DteType type, Sale sale) {
		BigDecimal subtotal = sale.getSubtotal();
		BigDecimal tax = sale.getTax();
		BigDecimal total = sale.getTotal();
		// En la Factura la venta gravada ya incluye el IVA
		BigDecimal taxedTotal = type == DteType.FACTURA ? total : subtotal;
		PaymentMethod payment = sale.getPaymentMethod();

		Map<String, Object> r = new LinkedHashMap<>();
		r.put("totalNoSuj", ZERO);
		r.put("totalExenta", ZERO);
		r.put("totalGravada", taxedTotal);
		r.put("subTotalVentas", taxedTotal);
		r.put("descuNoSuj", ZERO);
		r.put("descuExenta", ZERO);
		r.put("descuGravada", ZERO);
		if (type != DteType.NOTA_CREDITO) {
			r.put("porcentajeDescuento", ZERO);
		}
		r.put("totalDescu", ZERO);
		r.put("tributos", type == DteType.FACTURA ? null : List.of(ivaTribute(tax)));
		r.put("subTotal", taxedTotal);
		if (type != DteType.FACTURA) {
			r.put("ivaPerci1", ZERO);
		}
		r.put("ivaRete1", ZERO);
		r.put("reteRenta", ZERO);
		r.put("montoTotalOperacion", total);
		if (type != DteType.NOTA_CREDITO) {
			r.put("totalNoGravado", ZERO);
			r.put("totalPagar", total);
		}
		r.put("totalLetras", NumberToWords.amount(total));
		if (type == DteType.FACTURA) {
			r.put("totalIva", tax);
		}
		if (type != DteType.NOTA_CREDITO) {
			r.put("saldoFavor", ZERO);
		}
		r.put("condicionOperacion", payment.getOperationCondition());
		if (type != DteType.NOTA_CREDITO) {
			r.put("pagos", List.of(payment(payment, total)));
			r.put("numPagoElectronico", null);
		}
		return r;
	}

	private Map<String, Object> ivaTribute(BigDecimal tax) {
		Map<String, Object> t = new LinkedHashMap<>();
		t.put("codigo", IVA_CODE);
		t.put("descripcion", IVA_DESCRIPTION);
		t.put("valor", tax);
		return t;
	}

	private Map<String, Object> payment(PaymentMethod method, BigDecimal total) {
		boolean credit = method.getOperationCondition() == 2;
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("codigo", method.getCode());
		p.put("montoPago", total);
		p.put("referencia", null);
		// Plazo: 01 = días
		p.put("plazo", credit ? "01" : null);
		p.put("periodo", credit ? 30 : null);
		return p;
	}

	// ------------------------------------------------------------------
	// Evento de contingencia
	// ------------------------------------------------------------------

	public String buildContingencyEvent(ContingencyEvent event, List<Dte> dtes, EmitterConfig c) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime end = event.getEndedAt() != null ? event.getEndedAt() : now;

		Map<String, Object> id = new LinkedHashMap<>();
		id.put("version", 3);
		id.put("ambiente", c.getEnvironment());
		id.put("codigoGeneracion", event.getGenerationCode());
		id.put("fTransmision", now.format(DATE));
		id.put("hTransmision", now.format(TIME));

		Map<String, Object> emitter = new LinkedHashMap<>();
		emitter.put("nit", digits(c.getNit()));
		emitter.put("nombre", c.getBusinessName());
		emitter.put("nombreResponsable", "Sistema SV-Core");
		emitter.put("tipoDocResponsable", DOC_NIT);
		emitter.put("numeroDocResponsable", digits(c.getNit()));
		emitter.put("tipoEstablecimiento", c.getEstablishmentType());
		emitter.put("codEstableMH", c.getEstablishmentCode());
		emitter.put("codPuntoVenta", c.getPointOfSaleCode());
		emitter.put("telefono", c.getPhone());
		emitter.put("correo", c.getEmail());

		List<Map<String, Object>> detail = new ArrayList<>();
		int number = 1;
		for (Dte dte : dtes) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("noItem", number++);
			item.put("codigoGeneracion", dte.getGenerationCode());
			item.put("tipoDoc", dte.getType().getCode());
			detail.add(item);
		}

		Map<String, Object> reason = new LinkedHashMap<>();
		reason.put("fInicio", event.getStartedAt().format(DATE));
		reason.put("fFin", end.format(DATE));
		reason.put("hInicio", event.getStartedAt().format(TIME));
		reason.put("hFin", end.format(TIME));
		reason.put("tipoContingencia", event.getContingencyType());
		reason.put("motivoContingencia", event.getReason());

		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("identificacion", id);
		doc.put("emisor", emitter);
		doc.put("detalleDTE", detail);
		doc.put("motivo", reason);
		return write(doc);
	}

	// ------------------------------------------------------------------
	// Evento de invalidación
	// ------------------------------------------------------------------

	public String buildInvalidationEvent(InvalidationEvent event, Dte dte, EmitterConfig c, User responsible) {
		LocalDateTime now = LocalDateTime.now();

		Map<String, Object> id = new LinkedHashMap<>();
		id.put("version", 2);
		id.put("ambiente", dte.getEnvironment());
		id.put("codigoGeneracion", event.getGenerationCode());
		id.put("fecAnula", now.format(DATE));
		id.put("horAnula", now.format(TIME));

		Map<String, Object> emitter = new LinkedHashMap<>();
		emitter.put("nit", digits(c.getNit()));
		emitter.put("nombre", c.getBusinessName());
		emitter.put("tipoEstablecimiento", c.getEstablishmentType());
		emitter.put("nomEstablecimiento", c.getEstablishmentName());
		emitter.put("codEstableMH", c.getEstablishmentCode());
		emitter.put("codEstable", c.getEstablishmentCode());
		emitter.put("codPuntoVentaMH", c.getPointOfSaleCode());
		emitter.put("codPuntoVenta", c.getPointOfSaleCode());
		emitter.put("telefono", c.getPhone());
		emitter.put("correo", c.getEmail());

		boolean identified = dte.getReceiverNit() != null;
		Map<String, Object> document = new LinkedHashMap<>();
		document.put("tipoDte", dte.getType().getCode());
		document.put("codigoGeneracion", dte.getGenerationCode());
		document.put("selloRecibido", dte.getReceptionStamp());
		document.put("numeroControl", dte.getControlNumber());
		document.put("fecEmi", dte.getIssuedAt().format(DATE));
		document.put("montoIva", dte.getSale().getTax());
		document.put("codigoGeneracionR", null);
		document.put("tipoDocumento", identified ? DOC_NIT : null);
		document.put("numDocumento", identified ? digits(dte.getReceiverNit()) : null);
		document.put("nombre", identified ? dte.getReceiverName() : null);
		document.put("telefono", null);
		document.put("correo", dte.getReceiverEmail());

		// La demo no guarda el documento de identidad del usuario: se usa su nombre de usuario como referencia
		Map<String, Object> reason = new LinkedHashMap<>();
		reason.put("tipoAnulacion", event.getInvalidationType());
		reason.put("motivoAnulacion", event.getReason());
		reason.put("nombreResponsable", responsible.getFullName());
		reason.put("tipDocResponsable", DOC_OTHER);
		reason.put("numDocResponsable", responsible.getUsername());
		reason.put("nombreSolicita", identified ? dte.getReceiverName() : responsible.getFullName());
		reason.put("tipDocSolicita", identified ? DOC_NIT : DOC_OTHER);
		reason.put("numDocSolicita", identified ? digits(dte.getReceiverNit()) : responsible.getUsername());

		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("identificacion", id);
		doc.put("emisor", emitter);
		doc.put("documento", document);
		doc.put("motivo", reason);
		return write(doc);
	}

	// ------------------------------------------------------------------

	private Map<String, Object> address(String department, String municipality, String detail) {
		if (department == null && municipality == null && detail == null) return null;
		Map<String, Object> a = new LinkedHashMap<>();
		a.put("departamento", department);
		a.put("municipio", municipality);
		a.put("complemento", detail);
		return a;
	}

	/** Hacienda espera NIT y NRC solo con dígitos. */
	private static String digits(String value) {
		return value == null ? null : value.replaceAll("\\D", "");
	}

	private String write(Map<String, Object> document) {
		return mapper.writeValueAsString(document);
	}
}
