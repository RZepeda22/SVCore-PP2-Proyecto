package com.svcore.api_demo.service;

import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.InvalidationEvent;

/**
 * Transmisión al Ministerio de Hacienda. Implementaciones (svcore.hacienda.mode):
 * <ul>
 *   <li>mock: simulación para la demo (por defecto).</li>
 *   <li>api: cliente HTTP de la API de recepción de DTE (requiere credenciales del emisor).</li>
 * </ul>
 * Si Hacienda no responde, la respuesta es CONTINGENCIA: la venta nunca se detiene.
 */
public interface HaciendaService {

	/** Transmite el DTE firmado (dte.signedDocument). */
	HaciendaResponseDTO transmit(Dte dte);

	/** Transmite el evento de contingencia firmado; debe aceptarse antes de enviar los DTE del lote. */
	HaciendaResponseDTO transmitContingencyEvent(ContingencyEvent event);

	/** Transmite el evento de invalidación firmado. */
	HaciendaResponseDTO transmitInvalidation(InvalidationEvent event, Dte dte);
}
