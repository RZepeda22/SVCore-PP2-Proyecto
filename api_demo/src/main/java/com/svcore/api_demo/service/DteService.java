package com.svcore.api_demo.service;

import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.dtos.DteSummaryDTO;
import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.dtos.InvalidateDteDTO;
import com.svcore.api_demo.models.dtos.SignatureVerificationDTO;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.enums.DteStatus;

public interface DteService {

	/** Genera el JSON del DTE de una venta, lo firma y lo transmite a Hacienda. */
	DteViewDTO issue(Sale sale);

	/** Igual que issue, para documentos que modifican otro (Nota de Crédito sobre un CCF). */
	DteViewDTO issue(Sale sale, Dte related);

	List<DteSummaryDTO> findAll(DteStatus status);

	DteViewDTO findById(UUID dteId);

	/** Reintenta un DTE en contingencia o rechazado (con los datos corregidos del cliente). */
	DteViewDTO retry(UUID dteId);

	/** Cola de reintentos: transmite el evento de contingencia y luego los DTE pendientes. */
	void retryPending();

	/** CU-04: invalida un DTE sellado y revierte sus movimientos de inventario. */
	DteViewDTO invalidate(InvalidateDteDTO info);

	/** JSON del DTE según el esquema de Hacienda. */
	String getJson(UUID dteId);

	/** Documento firmado (JWS) tal como se transmitió. */
	String getSignedDocument(UUID dteId);

	SignatureVerificationDTO verifySignature(UUID dteId);
}
