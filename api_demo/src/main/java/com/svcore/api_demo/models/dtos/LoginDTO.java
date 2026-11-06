package com.svcore.api_demo.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

	/** Usuario o correo. */
	@NotBlank(message = "Ingresa tu usuario o correo")
	private String identifier;

	@NotBlank(message = "Ingresa tu contraseña")
	private String password;
}
