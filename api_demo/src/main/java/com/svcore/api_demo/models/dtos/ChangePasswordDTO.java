package com.svcore.api_demo.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {

	@NotBlank(message = "Ingresa tu contraseña actual")
	private String currentPassword;

	@NotBlank(message = "Ingresa la nueva contraseña")
	private String newPassword;
}
