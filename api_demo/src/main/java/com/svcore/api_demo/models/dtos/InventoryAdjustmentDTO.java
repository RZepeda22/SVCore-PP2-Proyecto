package com.svcore.api_demo.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Conteo físico: la diferencia con el stock del sistema queda como movimiento AJUSTE. */
@Data
public class InventoryAdjustmentDTO {

	@NotNull(message = "Selecciona el producto")
	private UUID productId;

	@NotNull(message = "Indica la cantidad contada")
	@Min(value = 0, message = "La cantidad contada no puede ser negativa")
	@Max(value = 100000)
	private Integer countedQuantity;

	@NotBlank(message = "Indica el motivo del ajuste")
	@Size(max = 200)
	private String reason;
}
