package com.svcore.api_demo.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.service.AuditService;

import lombok.RequiredArgsConstructor;

/** RNF-06: consulta y verificación de la bitácora (solo ADMN). No existe ningún endpoint para modificarla. */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> search(
			@RequestParam(name = "username", required = false) String username,
			@RequestParam(name = "action", required = false) AuditAction action,
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "50") int size) {
		return GeneralResponse.getResponse(auditService.search(username, action, from, to, page, size));
	}

	/** Recalcula la cadena de hashes: detecta registros modificados o eliminados directamente en la base. */
	@GetMapping("/verify")
	public ResponseEntity<GeneralResponse> verify() {
		return GeneralResponse.getResponse(auditService.verify());
	}
}
