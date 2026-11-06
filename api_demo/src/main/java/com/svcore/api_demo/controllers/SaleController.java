package com.svcore.api_demo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.SaveSaleDTO;
import com.svcore.api_demo.service.SaleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sale")
@RequiredArgsConstructor
public class SaleController {

	private final SaleService saleService;

	/**
	 * "Confirmar venta y emitir DTE". Responde 201 aunque Hacienda rechace o no responda:
	 * la venta quedó registrada y data.status indica qué pantalla mostrar.
	 */
	@PostMapping("/save")
	public ResponseEntity<GeneralResponse> save(@Valid @RequestBody SaveSaleDTO saveSaleDTO) {
		DteViewDTO dte = saleService.registerSale(saveSaleDTO);
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Venta registrada", dte);
	}
}
