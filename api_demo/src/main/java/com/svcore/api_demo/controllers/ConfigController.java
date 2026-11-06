package com.svcore.api_demo.controllers;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.EmitterConfigDTO;
import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.EmitterConfigService;
import com.svcore.api_demo.service.implementations.HaciendaServiceMockImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** CU-09: configuración de facturación. */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

	private final EmitterConfigService emitterConfigService;
	private final ObjectProvider<HaciendaServiceMockImpl> haciendaSimulator;
	private final AuditService auditService;

	@GetMapping("/emitter")
	public ResponseEntity<GeneralResponse> emitter() {
		return GeneralResponse.getResponse(EmitterConfigDTO.from(emitterConfigService.get()));
	}

	@PutMapping("/emitter")
	public ResponseEntity<GeneralResponse> updateEmitter(@Valid @RequestBody EmitterConfigDTO emitterConfigDTO) {
		return GeneralResponse.getResponse(HttpStatus.OK, "Configuración fiscal actualizada",
				emitterConfigService.update(emitterConfigDTO));
	}

	/** Estado de la simulación de Hacienda (solo existe en modo mock). */
	@GetMapping("/hacienda-simulator")
	public ResponseEntity<GeneralResponse> simulator() {
		return GeneralResponse.getResponse(Map.of("available", simulatorOrFail().isAvailable()));
	}

	/** Enciende o apaga la simulación de Hacienda para demostrar la contingencia sin reiniciar la API. */
	@PutMapping("/hacienda-simulator")
	public ResponseEntity<GeneralResponse> setSimulator(@RequestParam(name = "available") boolean available) {
		simulatorOrFail().setAvailable(available);
		auditService.record(AuditAction.SIMULADOR_HACIENDA, "hacienda", null,
				available ? "Simulación: Hacienda disponible" : "Simulación: Hacienda NO disponible");
		return GeneralResponse.getResponse(HttpStatus.OK,
				available ? "Hacienda (simulada) disponible" : "Hacienda (simulada) fuera de servicio",
				Map.of("available", available));
	}

	private HaciendaServiceMockImpl simulatorOrFail() {
		HaciendaServiceMockImpl simulator = haciendaSimulator.getIfAvailable();
		if (simulator == null) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "La simulación de Hacienda no está activa (modo api)");
		}
		return simulator;
	}
}
