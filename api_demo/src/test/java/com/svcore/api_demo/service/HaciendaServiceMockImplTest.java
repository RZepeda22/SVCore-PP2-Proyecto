package com.svcore.api_demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.service.implementations.HaciendaServiceMockImpl;

class HaciendaServiceMockImplTest {

	private SignatureService signatureService;
	private HaciendaServiceMockImpl hacienda;

	@BeforeEach
	void setUp() {
		signatureService = mock(SignatureService.class);
		when(signatureService.verify(anyString())).thenReturn(true);
		hacienda = new HaciendaServiceMockImpl(signatureService, true);
	}

	@Test
	void sellaElDocumentoCuandoHaciendaResponde() {
		HaciendaResponseDTO response = hacienda.transmit(dte("220145-3"));

		assertEquals(DteStatus.SELLADO, response.getStatus());
		assertNotNull(response.getReceptionStamp());
		assertNull(response.getCode());
	}

	@Test
	void sellaFacturaDeConsumidorFinalSinNrc() {
		assertEquals(DteStatus.SELLADO, hacienda.transmit(dte(null)).getStatus());
	}

	@Test
	void rechazaReceptorConNrcInvalido() {
		HaciendaResponseDTO response = hacienda.transmit(dte("990000-0"));

		assertEquals(DteStatus.RECHAZADO, response.getStatus());
		assertEquals("003", response.getCode());
		assertNull(response.getReceptionStamp());
	}

	@Test
	void rechazaUnaFirmaInvalida() {
		when(signatureService.verify(anyString())).thenReturn(false);

		HaciendaResponseDTO response = hacienda.transmit(dte("220145-3"));

		assertEquals(DteStatus.RECHAZADO, response.getStatus());
		assertEquals("004", response.getCode());
	}

	@Test
	void quedaEnContingenciaCuandoHaciendaNoResponde() {
		hacienda.setAvailable(false);

		HaciendaResponseDTO response = hacienda.transmit(dte("220145-3"));

		assertEquals(DteStatus.CONTINGENCIA, response.getStatus());
		assertNull(response.getReceptionStamp());
	}

	private static Dte dte(String receiverNrc) {
		Dte dte = new Dte();
		dte.setGenerationCode(UUID.randomUUID().toString().toUpperCase());
		dte.setReceiverNrc(receiverNrc);
		dte.setSignedDocument("encabezado.contenido.firma");
		return dte;
	}
}
