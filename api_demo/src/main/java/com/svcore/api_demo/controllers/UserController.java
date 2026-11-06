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

import com.svcore.api_demo.models.dtos.ChangePasswordDTO;
import com.svcore.api_demo.models.dtos.EditUserDTO;
import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.dtos.ResetPasswordDTO;
import com.svcore.api_demo.models.dtos.SaveUserDTO;
import com.svcore.api_demo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** CU-10: usuarios. /me y /change-password son para cualquier usuario; el resto solo para ADMN. */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<GeneralResponse> me() {
		return GeneralResponse.getResponse(userService.me());
	}

	@PatchMapping("/change-password")
	public ResponseEntity<GeneralResponse> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
		userService.changeOwnPassword(changePasswordDTO);
		return GeneralResponse.getResponse(HttpStatus.OK, "Contraseña actualizada");
	}

	@GetMapping("/all")
	public ResponseEntity<GeneralResponse> findAll() {
		return GeneralResponse.getResponse(userService.findAll());
	}

	@PostMapping("/save")
	public ResponseEntity<GeneralResponse> save(@Valid @RequestBody SaveUserDTO saveUserDTO) {
		return GeneralResponse.getResponse(HttpStatus.CREATED, "Usuario creado", userService.create(saveUserDTO));
	}

	@PatchMapping("/edit")
	public ResponseEntity<GeneralResponse> edit(@RequestParam(name = "userId") UUID userId,
												@Valid @RequestBody EditUserDTO editUserDTO) {
		return GeneralResponse.getResponse(HttpStatus.OK, "Usuario actualizado", userService.edit(userId, editUserDTO));
	}

	@PatchMapping("/reset-password")
	public ResponseEntity<GeneralResponse> resetPassword(@RequestParam(name = "userId") UUID userId,
														 @Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
		userService.resetPassword(userId, resetPasswordDTO);
		return GeneralResponse.getResponse(HttpStatus.OK, "Contraseña restablecida");
	}

	@PatchMapping("/unlock")
	public ResponseEntity<GeneralResponse> unlock(@RequestParam(name = "userId") UUID userId) {
		userService.unlock(userId);
		return GeneralResponse.getResponse(HttpStatus.OK, "Usuario desbloqueado");
	}
}
