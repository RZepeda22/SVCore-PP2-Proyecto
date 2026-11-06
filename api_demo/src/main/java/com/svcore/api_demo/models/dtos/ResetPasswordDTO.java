package com.svcore.api_demo.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {

	@NotBlank(message = "Ingresa la nueva contraseña")
	private String newPassword;
}
