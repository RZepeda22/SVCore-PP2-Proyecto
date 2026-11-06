package com.svcore.api_demo.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Entrada de mercadería a la bodega (botón "Registrar entrada" de inventario). */
@Data
public class InventoryEntryDTO {

	@NotNull(message = "Selecciona el producto")
	private UUID productId;

	@NotNull(message = "Indica la cantidad")
	@Min(value = 1, message = "La cantidad mínima es 1")
	@Max(value = 10000, message = "La cantidad máxima por entrada es 10000")
	private Integer quantity;
}
