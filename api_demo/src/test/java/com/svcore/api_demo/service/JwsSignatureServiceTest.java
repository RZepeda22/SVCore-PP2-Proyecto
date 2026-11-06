package com.svcore.api_demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.svcore.api_demo.service.implementations.JwsSignatureServiceImpl;

class JwsSignatureServiceTest {

	private static final String JSON = "{\"identificacion\":{\"tipoDte\":\"01\"},\"resumen\":{\"totalPagar\":812.45}}";

	@TempDir
	Path tempDir;

	private JwsSignatureServiceImpl signer;

	@BeforeEach
	void setUp() {
		signer = newSigner();
	}

	private JwsSignatureServiceImpl newSigner() {
		JwsSignatureServiceImpl service = new JwsSignatureServiceImpl("", "", "",
				tempDir.resolve("dte-demo-key.json").toString());
		service.loadKey();
		return service;
	}

	@Test
	void firmaConRs512YRecuperaElContenido() {
		String jws = signer.sign(JSON);

		assertEquals(3, jws.split("\\.").length, "JWS compacto: encabezado.contenido.firma");
		assertEquals("RS512", signer.algorithm());
		assertTrue(signer.verify(jws));
		assertEquals(JSON, signer.payload(jws));
	}

	@Test
	void detectaUnDocumentoAlterado() {
		String[] parts = signer.sign(JSON).split("\\.");
		String altered = JSON.replace("812.45", "1.00");
		String tampered = parts[0] + "."
				+ Base64.getUrlEncoder().withoutPadding().encodeToString(altered.getBytes(StandardCharsets.UTF_8))
				+ "." + parts[2];

		assertFalse(signer.verify(tampered));
	}

	@Test
	void conservaLaLlaveEntreReinicios() {
		String jws = signer.sign(JSON);

		assertTrue(newSigner().verify(jws), "La llave de demostración se guarda y se reutiliza");
	}

	@Test
	void rechazaTextoQueNoEsJws() {
		assertFalse(signer.verify("no-es-un-jws"));
		assertFalse(signer.verify(null));
	}
}
