package com.svcore.api_demo.models.dtos;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveUserDTO {

	@NotBlank(message = "El usuario es obligatorio")
	@Pattern(regexp = "^[a-z0-9._]{3,50}$", message = "El usuario solo admite minúsculas, números, punto y guion bajo (3 a 50)")
	private String username;

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "El correo no es válido")
	@Size(max = 100)
	private String email;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String fullName;

	@NotBlank(message = "La contraseña es obligatoria")
	private String password;

	@NotEmpty(message = "Asigna al menos un rol")
	private List<String> roles;
}
