package com.svcore.api_demo.models.dtos;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditUserDTO {

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "El correo no es válido")
	@Size(max = 100)
	private String email;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String fullName;

	@NotEmpty(message = "Asigna al menos un rol")
	private List<String> roles;

	@NotNull(message = "Indica si el usuario está activo")
	private Boolean active;
}
