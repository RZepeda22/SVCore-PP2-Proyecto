package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveProductDTO {

	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^[A-Z0-9-]{3,25}$", message = "El código solo admite mayúsculas, números y guiones (3 a 25)")
	private String code;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String name;

	@NotNull(message = "El precio es obligatorio")
	@DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
	@DecimalMax(value = "999999.99", message = "El precio es demasiado alto")
	@Digits(integer = 6, fraction = 2, message = "El precio admite máximo 2 decimales")
	private BigDecimal price;

	@NotNull(message = "El stock mínimo es obligatorio")
	@Min(value = 0, message = "El stock mínimo no puede ser negativo")
	@Max(value = 100000)
	private Integer minStock;

	@NotNull(message = "La categoría es obligatoria")
	private UUID categoryId;

	/** Existencias iniciales (solo al crear). Queda como movimiento INVENTARIO_INICIAL. */
	@Min(value = 0, message = "El stock inicial no puede ser negativo")
	@Max(value = 100000)
	private Integer initialStock;
}
