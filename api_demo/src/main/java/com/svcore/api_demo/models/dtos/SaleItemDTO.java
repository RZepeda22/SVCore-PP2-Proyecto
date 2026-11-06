package com.svcore.api_demo.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaleItemDTO {

	@NotNull(message = "Falta el producto")
	private UUID productId;

	@NotNull(message = "Falta la cantidad")
	@Min(value = 1, message = "La cantidad mínima es 1")
	@Max(value = 1000, message = "La cantidad máxima por línea es 1000")
	private Integer quantity;
}
