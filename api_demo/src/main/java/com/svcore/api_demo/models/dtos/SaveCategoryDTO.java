package com.svcore.api_demo.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCategoryDTO {

	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^[a-z0-9-]{2,20}$", message = "El código solo admite minúsculas, números y guiones (2 a 20)")
	private String code;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 50)
	private String name;
}
