package com.svcore.api_demo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.svcore.api_demo.models.entities.Customer;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.EmitterConfig;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.SaleDetail;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;
import com.svcore.api_demo.models.enums.TaxpayerType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DteJsonBuilderTest {

	private final DteJsonBuilder builder = new DteJsonBuilder();
	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void facturaExpresaLosPreciosConIvaIncluido() {
		JsonNode json = build(DteType.FACTURA, null);

		assertEquals("01", json.at("/identificacion/tipoDte").asString());
		assertEquals(1, json.at("/identificacion/version").asInt());
		assertEquals(1, json.at("/identificacion/tipoModelo").asInt());
		assertEquals("2026-06-20", json.at("/identificacion/fecEmi").asString());
		assertEquals("10:42:18", json.at("/identificacion/horEmi").asString());
		// 34.99 x 1.13 = 39.5387
		assertAmount("39.5387", json.at("/cuerpoDocumento/0/precioUni"));
		assertAmount("9.10", json.at("/cuerpoDocumento/0/ivaItem"));
		assertAmount("79.08", json.at("/resumen/totalPagar"));
		assertAmount("9.10", json.at("/resumen/totalIva"));
		assertEquals("SETENTA Y NUEVE 08/100 USD", json.at("/resumen/totalLetras").asString());
		assertTrue(json.at("/receptor/nombre").isNull(), "Consumidor Final sin datos");
		assertEquals("0614010120001", json.at("/emisor/nit").asString().substring(0, 13));
	}

	@Test
	void creditoFiscalSeparaElIvaEnTributos() {
		Customer customer = new Customer();
		customer.setName("Almacenes MORENA, S.A.");
		customer.setTaxpayerType(TaxpayerType.CONTRIBUYENTE);
		customer.setNit("0614-280590-102-3");
		customer.setNrc("220145-3");
		customer.setEconomicActivityCode("47190");

		JsonNode json = build(DteType.CREDITO_FISCAL, customer);

		assertEquals("03", json.at("/identificacion/tipoDte").asString());
		assertEquals(3, json.at("/identificacion/version").asInt());
		assertAmount("34.99", json.at("/cuerpoDocumento/0/precioUni"));
		assertEquals("20", json.at("/cuerpoDocumento/0/tributos/0").asString());
		assertAmount("69.98", json.at("/resumen/totalGravada"));
		assertAmount("9.10", json.at("/resumen/tributos/0/valor"));
		assertEquals("2201453", json.at("/receptor/nrc").asString(), "NRC solo con dígitos");
		assertEquals("06142805901023", json.at("/receptor/nit").asString());
		assertEquals("05", json.at("/resumen/pagos/0/codigo").asString());
	}

	private static void assertAmount(String expected, JsonNode node) {
		assertEquals(0, new BigDecimal(expected).compareTo(node.decimalValue()),
				() -> "Se esperaba " + expected + " y llegó " + node);
	}

	private JsonNode build(DteType type, Customer customer) {
		Product product = new Product();
		product.setCode("HL-U509-R");
		product.setName("Sport-100 Helmet");

		Sale sale = new Sale();
		sale.setDocType(type);
		sale.setCustomer(customer);
		sale.setPaymentMethod(PaymentMethod.TRANSFERENCIA);
		SaleDetail detail = new SaleDetail();
		detail.setProduct(product);
		detail.setQuantity(2);
		detail.setUnitPrice(new BigDecimal("34.99"));
		detail.setLineTotal(new BigDecimal("69.98"));
		sale.addDetail(detail);
		sale.setSubtotal(new BigDecimal("69.98"));
		sale.setDiscount(new BigDecimal("0.00"));
		sale.setTax(new BigDecimal("9.10"));
		sale.setTotal(new BigDecimal("79.08"));

		Dte dte = new Dte();
		dte.setSale(sale);
		dte.setType(type);
		dte.setEnvironment("00");
		dte.setControlNumber("DTE-" + type.getCode() + "-M001P002-000000000000001");
		dte.setGenerationCode("A1F29C30-4E7B-88D1-2C5A-9F0E1B33AAAA");
		dte.setIssuedAt(LocalDateTime.of(2026, 6, 20, 10, 42, 18));
		if (customer != null) {
			dte.setReceiverName(customer.getName());
			dte.setReceiverNit(customer.getNit());
			dte.setReceiverNrc(customer.getNrc());
		}

		return mapper.readTree(builder.buildDte(dte, emitter()));
	}

	private static EmitterConfig emitter() {
		EmitterConfig c = new EmitterConfig();
		c.setBusinessName("Adventure Works, S.A. de C.V.");
		c.setTradeName("Adventure Works");
		c.setNit("0614-010120-001-2");
		c.setNrc("123456-7");
		c.setEconomicActivityCode("47630");
		c.setEconomicActivity("Venta al por menor de artículos deportivos");
		c.setEstablishmentType("02");
		c.setEstablishmentName("Sucursal Central");
		c.setDepartmentCode("06");
		c.setMunicipalityCode("14");
		c.setAddressDetail("San Salvador");
		c.setPhone("2200-0000");
		c.setEmail("facturacion@adventureworks.demo");
		c.setEstablishmentCode("M001");
		c.setPointOfSaleCode("P002");
		c.setEnvironment("00");
		return c;
	}
}
