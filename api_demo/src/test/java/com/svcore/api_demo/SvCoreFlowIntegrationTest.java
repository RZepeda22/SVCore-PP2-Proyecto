package com.svcore.api_demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.svcore.api_demo.service.DteService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sprint 8: recorrido completo de la API contra MySQL real (Testcontainers), por HTTP y con JWT reales.
 * Las pruebas se ejecutan en orden porque cada una continúa el estado de la anterior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SvCoreFlowIntegrationTest {

	private static final String PASSWORD = "Prueba-SvCore-2026";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static String seller;
	private static String admin;
	private static String stockKeeper;
	private static String invoiceId;
	private static String rejectedCcfId;
	private static String contingencyCcfId;
	private static BigDecimal creditNoteTotal;
	private static BigDecimal expectedNetTotal = BigDecimal.ZERO;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DteService dteService;

	// ------------------------------------------------------------------
	// Sprint 1: autenticación, roles, usuarios y configuración
	// ------------------------------------------------------------------

	@Test
	@Order(1)
	void seguridadPorRoles() throws Exception {
		call(HttpMethod.GET, "/api/product/all", null, null, 401);
		JsonNode bad = call(HttpMethod.POST, "/api/auth/login", null,
				Map.of("identifier", "ricardo", "password", "incorrecta"), 401);
		assertEquals("Usuario o contraseña incorrectos", bad.get("message").asString());

		seller = login("ricardo");
		admin = login("marco");
		stockKeeper = login("josemaria");

		call(HttpMethod.POST, "/api/sale/save", stockKeeper, Map.of(), 403);
		call(HttpMethod.GET, "/api/report/summary", seller, null, 403);
		call(HttpMethod.GET, "/api/audit/verify", seller, null, 403);
		call(HttpMethod.POST, "/api/dte/invalidate", seller, Map.of(), 403);
		call(HttpMethod.GET, "/api/user/me", stockKeeper, null, 200);
	}

	@Test
	@Order(2)
	void usuariosConPoliticaDeContrasenas() throws Exception {
		JsonNode weak = call(HttpMethod.POST, "/api/user/save", admin, Map.of("username", "ana.lopez",
				"email", "ana@svcore.demo", "fullName", "Ana López", "password", "debil", "roles", List.of("VEND")), 400);
		assertTrue(weak.get("message").asString().contains("una mayúscula"));

		JsonNode created = call(HttpMethod.POST, "/api/user/save", admin, Map.of("username", "ana.lopez",
				"email", "ana@svcore.demo", "fullName", "Ana López", "password", "Caja#Segura2026", "roles",
				List.of("VEND")), 201);
		assertEquals("ana.lopez", created.at("/data/username").asString());
		call(HttpMethod.POST, "/api/user/save", admin, Map.of("username", "ana.lopez", "email", "otra@svcore.demo",
				"fullName", "Ana", "password", "Caja#Segura2026", "roles", List.of("VEND")), 409);

		// Configuración fiscal (CU-09)
		JsonNode config = call(HttpMethod.GET, "/api/config/emitter", seller, null, 200);
		assertEquals("00", config.at("/data/environment").asString());
		Map<String, Object> invalid = JSON.convertValue(config.get("data"), Map.class);
		invalid.put("environment", "02");
		call(HttpMethod.PUT, "/api/config/emitter", admin, invalid, 400);
	}

	// ------------------------------------------------------------------
	// Sprints 2-3: catálogo e inventario
	// ------------------------------------------------------------------

	@Test
	@Order(3)
	void catalogoEInventario() throws Exception {
		JsonNode category = call(HttpMethod.POST, "/api/category/save", stockKeeper,
				Map.of("code", "nutrition", "name", "Nutrición"), 201);
		JsonNode product = call(HttpMethod.POST, "/api/product/save", stockKeeper, Map.of("code", "EB-1000",
				"name", "Energy Bar", "price", new BigDecimal("1.50"), "minStock", 10,
				"categoryId", category.at("/data/id").asString(), "initialStock", 5), 201);
		String productId = product.at("/data/id").asString();
		assertEquals(5, product.at("/data/stock").asInt());

		JsonNode adjusted = call(HttpMethod.POST, "/api/inventory/adjustment", stockKeeper,
				Map.of("productId", productId, "countedQuantity", 3, "reason", "Conteo físico mensual"), 201);
		assertEquals(3, adjusted.at("/data/stock").asInt());

		JsonNode kardex = call(HttpMethod.GET, "/api/inventory/movements?productId=" + productId, stockKeeper, null, 200);
		assertEquals(2, kardex.get("data").size());
		assertEquals("AJUSTE", kardex.at("/data/0/type").asString());
		assertEquals(-2, kardex.at("/data/0/quantity").asInt());
		assertEquals("INVENTARIO_INICIAL", kardex.at("/data/1/type").asString());

		JsonNode alerts = call(HttpMethod.GET, "/api/inventory/alerts", stockKeeper, null, 200);
		assertTrue(alerts.get("data").toString().contains("EB-1000"));

		call(HttpMethod.POST, "/api/product/save", seller, Map.of(), 403);
	}

	// ------------------------------------------------------------------
	// Sprints 4-6: venta, DTE firmado, rechazo y contingencia
	// ------------------------------------------------------------------

	@Test
	@Order(4)
	void facturaSelladaConJsonYFirma() throws Exception {
		String helmet = productId("HL-U509-R");
		int stockBefore = stock("HL-U509-R");

		JsonNode dte = sale(null, "FACTURA", "EFECTIVO", helmet, 2, 201);
		invoiceId = dte.at("/data/id").asString();
		assertEquals("SELLADO", dte.at("/data/status").asString());
		assertEquals("DTE-01-M001P002-000000000000001", dte.at("/data/controlNumber").asString());
		assertEquals("SETENTA Y NUEVE 08/100 USD", dte.at("/data/totalInWords").asString());
		assertEquals(stockBefore - 2, stock("HL-U509-R"));

		JsonNode json = raw("/api/dte/json?dteId=" + invoiceId, seller);
		assertEquals("01", json.at("/identificacion/tipoDte").asString());
		assertEquals(0, new BigDecimal("79.08").compareTo(json.at("/resumen/totalPagar").decimalValue()));

		JsonNode signature = call(HttpMethod.GET, "/api/dte/verify-signature?dteId=" + invoiceId, seller, null, 200);
		assertTrue(signature.at("/data/valid").asBoolean());
		assertTrue(signature.at("/data/matchesStoredJson").asBoolean());
		assertEquals("RS512", signature.at("/data/algorithm").asString());
	}

	@Test
	@Order(5)
	void creditoFiscalRechazadoSeCorrigeYSeSella() throws Exception {
		String jorge = customerId("Jorge");
		JsonNode dte = sale(jorge, "CREDITO_FISCAL", "EFECTIVO", productId("CB-3456"), 1, 201);
		rejectedCcfId = dte.at("/data/id").asString();
		assertEquals("RECHAZADO", dte.at("/data/status").asString());
		assertEquals("003", dte.at("/data/rejectionCode").asString());

		call(HttpMethod.PATCH, "/api/customer/edit?customerId=" + jorge, seller, Map.of("name", "Comercial San Jorge",
				"taxpayerType", "CONTRIBUYENTE", "nrc", "334455-1", "nit", "0614-010199-105-2"), 200);
		JsonNode retried = call(HttpMethod.POST, "/api/dte/retry?dteId=" + rejectedCcfId, seller, null, 200);
		assertEquals("SELLADO", retried.at("/data/status").asString());
		assertEquals(2, retried.at("/data/attempts").asInt());
		assertEquals("3344551", raw("/api/dte/json?dteId=" + rejectedCcfId, seller).at("/receptor/nrc").asString());
		expectedNetTotal = expectedNetTotal.add(retried.at("/data/total").decimalValue());
	}

	@Test
	@Order(6)
	void contingenciaConEventoYTransmisionEnLote() throws Exception {
		call(HttpMethod.PUT, "/api/config/hacienda-simulator?available=false", admin, null, 200);

		JsonNode ccf = sale(customerId("MORENA"), "CREDITO_FISCAL", "TRANSFERENCIA", productId("BK-M68B-42"), 2, 201);
		contingencyCcfId = ccf.at("/data/id").asString();
		assertEquals("CONTINGENCIA", ccf.at("/data/status").asString());
		JsonNode json = raw("/api/dte/json?dteId=" + contingencyCcfId, seller);
		assertEquals(2, json.at("/identificacion/tipoModelo").asInt(), "Modelo diferido");
		assertEquals(2, json.at("/identificacion/tipoOperacion").asInt(), "Transmisión por contingencia");
		assertEquals(1, json.at("/identificacion/tipoContingencia").asInt());

		// Con la contingencia abierta, la siguiente venta va directo a la cola sin hacer esperar a la caja
		JsonNode invoice = sale(null, "FACTURA", "EFECTIVO", productId("WB-H098"), 1, 201);
		assertEquals("CONTINGENCIA", invoice.at("/data/status").asString());
		assertEquals(0, invoice.at("/data/attempts").asInt());

		call(HttpMethod.PUT, "/api/config/hacienda-simulator?available=true", admin, null, 200);
		JsonNode retried = call(HttpMethod.POST, "/api/dte/retry?dteId=" + contingencyCcfId, seller, null, 200);
		assertEquals("SELLADO", retried.at("/data/status").asString());
		assertNotNull(retried.at("/data/contingencyEventStamp").asString());

		dteService.retryPending(); // la cola automática transmite el resto del lote
		JsonNode sealedInvoice = call(HttpMethod.GET, "/api/dte/?dteId=" + invoice.at("/data/id").asString(), seller,
				null, 200);
		assertEquals("SELLADO", sealedInvoice.at("/data/status").asString());
		assertEquals("CERRADO", jdbc.queryForObject("SELECT status FROM contingency_event", String.class));

		expectedNetTotal = expectedNetTotal.add(retried.at("/data/total").decimalValue())
				.add(sealedInvoice.at("/data/total").decimalValue());
	}

	// ------------------------------------------------------------------
	// Sprint 7: nota de crédito, invalidación y reportes
	// ------------------------------------------------------------------

	@Test
	@Order(7)
	void notaDeCreditoPorDevolucion() throws Exception {
		String bike = productId("BK-M68B-42");
		int stockBefore = stock("BK-M68B-42");

		JsonNode note = call(HttpMethod.POST, "/api/dte/credit-note", seller, Map.of("dteId", contingencyCcfId,
				"reason", "Devolución de una bicicleta", "items", List.of(Map.of("productId", bike, "quantity", 1))), 201);
		assertEquals("NOTA_CREDITO", note.at("/data/type").asString());
		assertEquals("SELLADO", note.at("/data/status").asString());
		assertEquals(stockBefore + 1, stock("BK-M68B-42"));
		creditNoteTotal = note.at("/data/total").decimalValue();

		JsonNode json = raw("/api/dte/json?dteId=" + note.at("/data/id").asString(), seller);
		assertEquals("05", json.at("/identificacion/tipoDte").asString());
		assertEquals(note.at("/data/relatedGenerationCode").asString(),
				json.at("/documentoRelacionado/0/numeroDocumento").asString());

		call(HttpMethod.POST, "/api/dte/credit-note", seller, Map.of("dteId", contingencyCcfId, "reason", "Otra",
				"items", List.of(Map.of("productId", bike, "quantity", 2))), 409);
		call(HttpMethod.POST, "/api/dte/invalidate", admin, Map.of("dteId", contingencyCcfId, "invalidationType", 2,
				"reason", "Prueba"), 409);
		expectedNetTotal = expectedNetTotal.subtract(creditNoteTotal);
	}

	@Test
	@Order(8)
	void invalidacionReincorporaElInventario() throws Exception {
		int stockBefore = stock("HL-U509-R");

		JsonNode invalidated = call(HttpMethod.POST, "/api/dte/invalidate", admin, Map.of("dteId", invoiceId,
				"invalidationType", 2, "reason", "El cliente desistió de la compra"), 200);
		assertEquals("ANULADO", invalidated.at("/data/status").asString());
		assertNotNull(invalidated.at("/data/invalidationStamp").asString());
		assertEquals(stockBefore + 2, stock("HL-U509-R"));
		call(HttpMethod.POST, "/api/dte/invalidate", admin, Map.of("dteId", invoiceId, "invalidationType", 2,
				"reason", "Otra vez"), 409);

		// Sin conexión con Hacienda no se puede invalidar
		call(HttpMethod.PUT, "/api/config/hacienda-simulator?available=false", admin, null, 200);
		call(HttpMethod.POST, "/api/dte/invalidate", admin, Map.of("dteId", rejectedCcfId, "invalidationType", 1,
				"reason", "Error en datos"), 503);
		call(HttpMethod.PUT, "/api/config/hacienda-simulator?available=true", admin, null, 200);
		assertEquals("SELLADO", call(HttpMethod.GET, "/api/dte/?dteId=" + rejectedCcfId, seller, null, 200)
				.at("/data/status").asString());
	}

	@Test
	@Order(9)
	void reportesNetosDelPeriodo() throws Exception {
		JsonNode summary = call(HttpMethod.GET, "/api/report/summary", admin, null, 200).get("data");
		assertEquals(3, summary.get("sales").asInt());
		assertEquals(1, summary.get("creditNotes").asInt());
		assertEquals(1, summary.get("invalidated").asInt());
		assertEquals(0, expectedNetTotal.compareTo(summary.get("total").decimalValue()),
				"Ventas - notas de crédito, sin anulados: " + summary);

		JsonNode payments = call(HttpMethod.GET, "/api/report/sales-by-payment", admin, null, 200).get("data");
		assertTrue(payments.size() >= 2);
		JsonNode top = call(HttpMethod.GET, "/api/report/top-products?limit=3", admin, null, 200).get("data");
		assertEquals("BK-M68B-42", top.at("/0/code").asString());
		assertEquals(1, top.at("/0/quantity").asInt(), "2 vendidas - 1 devuelta");

		MvcResult csv = mvc.perform(MockMvcRequestBuilders.get("/api/report/iva-book/csv")
				.header("Authorization", "Bearer " + admin)).andReturn();
		assertEquals(200, csv.getResponse().getStatus());
		String content = csv.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertTrue(content.contains("Número de control"));
		assertTrue(content.contains("Almacenes MORENA"));
	}

	@Test
	@Order(10)
	void bitacoraIntegraYDetectaAlteraciones() throws Exception {
		JsonNode ok = call(HttpMethod.GET, "/api/audit/verify", admin, null, 200).get("data");
		assertTrue(ok.get("valid").asBoolean(), ok.toString());
		assertTrue(ok.get("checkedEntries").asInt() > 15);

		JsonNode page = call(HttpMethod.GET, "/api/audit/all?action=DTE_INVALIDADO", admin, null, 200).get("data");
		assertEquals(1, page.get("totalElements").asInt());
		assertEquals("marco", page.at("/content/0/username").asString());

		// Alguien con acceso directo a la base altera un registro: la cadena de hashes lo detecta
		Long id = jdbc.queryForObject("SELECT MIN(id) FROM audit_log WHERE action = 'VENTA_REGISTRADA'", Long.class);
		jdbc.update("UPDATE audit_log SET detail = 'Venta por 0.01' WHERE id = ?", id);
		JsonNode tampered = call(HttpMethod.GET, "/api/audit/verify", admin, null, 200).get("data");
		assertFalse(tampered.get("valid").asBoolean());
		assertEquals(id.longValue(), tampered.get("firstInvalidId").asLong());
	}

	// ------------------------------------------------------------------

	private String login(String username) throws Exception {
		return call(HttpMethod.POST, "/api/auth/login", null, Map.of("identifier", username, "password", PASSWORD), 200)
				.at("/data/token").asString();
	}

	private JsonNode sale(String customerId, String docType, String payment, String productId, int quantity,
						  int status) throws Exception {
		Map<String, Object> body = new java.util.HashMap<>();
		body.put("customerId", customerId);
		body.put("docType", docType);
		body.put("paymentMethod", payment);
		body.put("items", List.of(Map.of("productId", productId, "quantity", quantity)));
		return call(HttpMethod.POST, "/api/sale/save", seller, body, status);
	}

	private String productId(String code) throws Exception {
		return product(code).get("id").asString();
	}

	private int stock(String code) throws Exception {
		return product(code).get("stock").asInt();
	}

	private JsonNode product(String code) throws Exception {
		JsonNode data = call(HttpMethod.GET, "/api/product/all?search=" + code, seller, null, 200).get("data");
		for (JsonNode p : data) {
			if (code.equals(p.get("code").asString())) return p;
		}
		throw new AssertionError("No existe el producto " + code);
	}

	private String customerId(String search) throws Exception {
		return call(HttpMethod.GET, "/api/customer/all?search=" + search, seller, null, 200).at("/data/0/id").asString();
	}

	private JsonNode raw(String url, String token) throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token))
				.andReturn();
		assertEquals(200, result.getResponse().getStatus());
		return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
	}

	private JsonNode call(HttpMethod method, String url, String token, Object body, int expectedStatus) throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, url);
		if (token != null) request.header("Authorization", "Bearer " + token);
		if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body));
		MvcResult result = mvc.perform(request).andReturn();
		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertEquals(expectedStatus, result.getResponse().getStatus(), () -> method + " " + url + " -> " + content);
		return content.isBlank() ? null : JSON.readTree(content);
	}
}
