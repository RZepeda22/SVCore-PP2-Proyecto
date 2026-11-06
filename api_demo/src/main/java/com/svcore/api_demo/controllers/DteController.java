package com.svcore.api_demo.controllers;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.CreditNoteDTO;
import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.InvalidateDteDTO;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.service.CreditNoteService;
import com.svcore.api_demo.service.DteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dte")
@RequiredArgsConstructor
public class DteController {

	private final DteService dteService;
	private final CreditNoteService creditNoteService;

	/** status opcional: SELLADO | CONTINGENCIA | RECHAZADO | ANULADO. */
	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll(@RequestParam(name = "status", required = false) DteStatus status) {
		return GeneralResponse.getResponse(dteService.findAll(status));
	}

	@GetMapping("/")
	public ResponseEntity<GeneralResponse> findOne(@RequestParam(name = "dteId") UUID dteId) {
		return GeneralResponse.getResponse(dteService.findById(dteId));
	}

	@PostMapping("/retry")
	public ResponseEntity<GeneralResponse> retry(@RequestParam(name = "dteId") UUID dteId) {
		return GeneralResponse.getResponse(dteService.retry(dteId));
	}

	/** JSON del DTE según el esquema de Hacienda (descarga). */
	@GetMapping("/json")
	public ResponseEntity<String> json(@RequestParam(name = "dteId") UUID dteId) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dte-" + dteId + ".json\"")
				.body(dteService.getJson(dteId));
	}

	/** Documento firmado (JWS compacto) tal como se transmitió a Hacienda. */
	@GetMapping("/signed")
	public ResponseEntity<String> signed(@RequestParam(name = "dteId") UUID dteId) {
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dte-" + dteId + ".jws\"")
				.body(dteService.getSignedDocument(dteId));
	}

	@GetMapping("/verify-signature")
	public ResponseEntity<GeneralResponse> verifySignature(@RequestParam(name = "dteId") UUID dteId) {
		return GeneralResponse.getResponse(dteService.verifySignature(dteId));
	}

	/** CU-04: invalidación (anulación). Reincorpora el inventario. */
	@PostMapping("/invalidate")
	public ResponseEntity<GeneralResponse> invalidate(@Valid @RequestBody InvalidateDteDTO invalidateDteDTO) {
		return GeneralResponse.getResponse(HttpStatus.OK, "Documento invalidado", dteService.invalidate(invalidateDteDTO));
	}

	/** Nota de Crédito por devolución sobre un Crédito Fiscal sellado. */
	@PostMapping("/credit-note")
	public ResponseEntity<GeneralResponse> creditNote(@Valid @RequestBody CreditNoteDTO creditNoteDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Nota de crédito emitida",
				creditNoteService.create(creditNoteDTO));
	}
}
