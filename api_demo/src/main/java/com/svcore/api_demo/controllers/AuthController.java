package com.svcore.api_demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.LoginDTO;
import com.svcore.api_demo.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<GeneralResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
		return GeneralResponse.getResponse(authService.login(loginDTO));
	}
}
