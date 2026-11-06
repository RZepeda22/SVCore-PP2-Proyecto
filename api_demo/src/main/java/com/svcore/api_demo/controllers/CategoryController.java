package com.svcore.api_demo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.SaveCategoryDTO;
import com.svcore.api_demo.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

	private final ProductService productService;

	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll() {
		return GeneralResponse.getResponse(productService.findCategories());
	}

	@PostMapping("/save")
	public ResponseEntity<GeneralResponse> save(@Valid @RequestBody SaveCategoryDTO saveCategoryDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Categoría registrada",
				productService.saveCategory(saveCategoryDTO));
	}
}
