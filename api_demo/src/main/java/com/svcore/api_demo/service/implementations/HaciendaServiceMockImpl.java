package com.svcore.api_demo.service.implementations;

import java.time.Year;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.InvalidationEvent;
import com.svcore.api_demo.service.HaciendaService;
import com.svcore.api_demo.service.SignatureService;

/**
 * Simulación del Ministerio de Hacienda para la demo:
 * <ul>
 *   <li>No disponible (svcore.hacienda.disponible=false o PUT /api/config/hacienda-simulator): CONTINGENCIA.</li>
 *   <li>Firma que no corresponde a la llave del emisor: RECHAZADO (código 004 de la simulación).</li>
 *   <li>NRC del receptor que empieza con 99: RECHAZADO con código 003 (receptor inválido).</li>
 *   <li>En otro caso: SELLADO con un sello de recepción generado.</li>
 * </ul>
 * Los códigos de rechazo son propios de la simulación; los reales los define Hacienda.
 */
@Service
@ConditionalOnProperty(name = "svcore.hacienda.mode", havingValue = "mock", matchIfMissing = true)
public class HaciendaServiceMockImpl implements HaciendaService {

	private static final String INVALID_NRC_PREFIX = "99";

	private final SignatureService signatureService;
	private final AtomicBoolean available;

	public HaciendaServiceMockImpl(SignatureService signatureService,
								   @Value("${svcore.hacienda.disponible:true}") boolean available) {
		this.signatureService = signatureService;
		this.available = new AtomicBoolean(available);
	}

	public boolean isAvailable() {
		return available.get();
	}

	/** Permite demostrar la contingencia sin reiniciar la API. */
	public void setAvailable(boolean value) {
		available.set(value);
	}

	@Override
	public HaciendaResponseDTO transmit(Dte dte) {
		if (!available.get()) {
			return HaciendaResponseDTO.unavailable();
		}
		if (!signatureService.verify(dte.getSignedDocument())) {
			return HaciendaResponseDTO.rejected("004", "La firma electrónica del documento no es válida.");
		}
		String nrc = dte.getReceiverNrc();
		if (nrc != null && nrc.startsWith(INVALID_NRC_PREFIX)) {
			return HaciendaResponseDTO.rejected("003",
					"El número de registro de contribuyente (NRC) del receptor no coincide con los registros "
							+ "de la Administración Tributaria. Verifica el NRC del cliente y vuelve a transmitir.");
		}
		return HaciendaResponseDTO.stamped(stamp(dte.getGenerationCode()));
	}

	@Override
	public HaciendaResponseDTO transmitContingencyEvent(ContingencyEvent event) {
		if (!available.get()) {
			return HaciendaResponseDTO.unavailable();
		}
		if (!signatureService.verify(event.getSignedDocument())) {
			return HaciendaResponseDTO.rejected("004", "La firma del evento de contingencia no es válida.");
		}
		return HaciendaResponseDTO.stamped(stamp(event.getGenerationCode()));
	}

	@Override
	public HaciendaResponseDTO transmitInvalidation(InvalidationEvent event, Dte dte) {
		if (!available.get()) {
			return HaciendaResponseDTO.unavailable();
		}
		if (!signatureService.verify(event.getSignedDocument())) {
			return HaciendaResponseDTO.rejected("004", "La firma del evento de invalidación no es válida.");
		}
		return HaciendaResponseDTO.stamped(stamp(event.getGenerationCode()));
	}

	private static String stamp(String generationCode) {
		return Year.now().getValue() + "MH" + generationCode.replace("-", "");
	}
}
