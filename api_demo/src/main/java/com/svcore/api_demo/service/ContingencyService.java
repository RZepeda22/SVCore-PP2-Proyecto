package com.svcore.api_demo.service;

import java.util.Optional;

import com.svcore.api_demo.models.entities.ContingencyEvent;

/**
 * Decisión de diseño 1: contingencia. Los DTE emitidos mientras Hacienda no responde se agrupan en un
 * evento de contingencia; Hacienda debe recibir el evento antes de que se transmita el lote de documentos.
 */
public interface ContingencyService {

	/** Evento abierto, si Hacienda está actualmente en contingencia. */
	Optional<ContingencyEvent> findOpenEvent();

	/** Evento abierto actual (lo crea si no hay uno). */
	ContingencyEvent currentOpenEvent();

	/** Firma y transmite el evento si aún no fue aceptado. Devuelve true si Hacienda ya lo aceptó. */
	boolean ensureAccepted(ContingencyEvent event);

	/** Marca el evento como cerrado cuando ya no le quedan DTE pendientes de transmitir. */
	void closeIfComplete(ContingencyEvent event);
}
