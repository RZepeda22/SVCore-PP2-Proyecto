package com.svcore.api_demo.controllers;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.InventoryAdjustmentDTO;
import com.svcore.api_demo.models.dtos.InventoryEntryDTO;
import com.svcore.api_demo.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@PostMapping("/entry")
	public ResponseEntity<GeneralResponse> registerEntry(@Valid @RequestBody InventoryEntryDTO inventoryEntryDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Entrada registrada",
				inventoryService.registerEntry(inventoryEntryDTO));
	}

	/** Conteo físico. */
	@PostMapping("/adjustment")
	public ResponseEntity<GeneralResponse> adjust(@Valid @RequestBody InventoryAdjustmentDTO adjustmentDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Ajuste registrado", inventoryService.adjust(adjustmentDTO));
	}

	/** Kardex: historial de movimientos. Producto y fechas opcionales. */
	@GetMapping("/movements")
	public ResponseEntity<GeneralResponse> movements(
			@RequestParam(name = "productId", required = false) UUID productId,
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "limit", defaultValue = "100") int limit) {
		return GeneralResponse.getResponse(inventoryService.movements(productId, from, to, limit));
	}

	@GetMapping("/alerts")
	public ResponseEntity<GeneralResponse> alerts() {
		return GeneralResponse.getResponse(inventoryService.alerts());
	}
}
