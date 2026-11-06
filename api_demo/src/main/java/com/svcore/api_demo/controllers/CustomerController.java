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

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.SaveCustomerDTO;
import com.svcore.api_demo.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	/** Búsqueda por nombre o NRC (selector de cliente del POS). */
	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll(@RequestParam(name = "search", required = false) String search) {
		return GeneralResponse.getResponse(customerService.search(search));
	}

	@GetMapping("/")
	public ResponseEntity<GeneralResponse> findOne(@RequestParam(name = "customerId") UUID customerId) {
		return GeneralResponse.getResponse(customerService.findById(customerId));
	}

	@PostMapping("/save")
	public ResponseEntity<GeneralResponse> save(@Valid @RequestBody SaveCustomerDTO saveCustomerDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Cliente registrado",
				customerService.saveCustomer(saveCustomerDTO));
	}

	@PatchMapping("/edit")
	public ResponseEntity<GeneralResponse> edit(@RequestParam(name = "customerId") UUID customerId,
												@Valid @RequestBody SaveCustomerDTO saveCustomerDTO) {
		return GeneralResponse.getResponse(HttpStatus.OK, "Cliente actualizado",
				customerService.editCustomer(customerId, saveCustomerDTO));
	}
}
