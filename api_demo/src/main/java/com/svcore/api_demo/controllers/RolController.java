package com.svcore.api_demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rol")
@RequiredArgsConstructor
public class RolController {

	private final UserService userService;

	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll() {
		return GeneralResponse.getResponse(userService.findRoles());
	}
}
