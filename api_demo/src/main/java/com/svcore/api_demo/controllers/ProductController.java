package com.svcore.api_demo.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.EditProductDTO;
import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.SaveProductDTO;
import com.svcore.api_demo.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	/**
	 * Catálogo. category: bikes | components | clothing | accessories | all.
	 * includeInactive=true muestra también los productos desactivados (gestión de catálogo).
	 */
	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll(@RequestParam(name = "category", required = false) String category,
												   @RequestParam(name = "search", required = false) String search,
												   @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
		return GeneralResponse.getResponse(productService.findForSale(category, search, includeInactive));
	}

	@GetMapping("/")
	public ResponseEntity<GeneralResponse> findOne(@RequestParam(name = "productId") UUID productId) {
		return GeneralResponse.getResponse(productService.findById(productId));
	}

	@PostMapping("/save")
	public ResponseEntity<GeneralResponse> save(@Valid @RequestBody SaveProductDTO saveProductDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Producto registrado", productService.save(saveProductDTO));
	}

	@PatchMapping("/edit")
	public ResponseEntity<GeneralResponse> edit(@RequestParam(name = "productId") UUID productId,
												@Valid @RequestBody EditProductDTO editProductDTO) {
		return GeneralResponse.getResponse(HttpStatus.OK, "Producto actualizado",
				productService.edit(productId, editProductDTO));
	}
}
