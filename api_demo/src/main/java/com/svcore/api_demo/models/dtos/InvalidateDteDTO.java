package com.svcore.api_demo.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** CU-04: solicitud de invalidación (anulación) de un DTE sellado. */
@Data
public class InvalidateDteDTO {

	@NotNull(message = "Indica el documento a invalidar")
	private UUID dteId;

	/** 1 = error en la información, 2 = rescindir la operación, 3 = otro. */
	@NotNull(message = "Indica el tipo de invalidación")
	@Min(value = 1, message = "Tipo de invalidación no válido")
	@Max(value = 3, message = "Tipo de invalidación no válido")
	private Integer invalidationType;

	@NotBlank(message = "Indica el motivo de la invalidación")
	@Size(max = 250)
	private String reason;
}
