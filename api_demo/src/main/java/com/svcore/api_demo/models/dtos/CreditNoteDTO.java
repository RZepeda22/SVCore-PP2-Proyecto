package com.svcore.api_demo.models.dtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Nota de Crédito por devolución sobre un Comprobante de Crédito Fiscal sellado. */
@Data
public class CreditNoteDTO {

	@NotNull(message = "Indica el Crédito Fiscal al que aplica la nota")
	private UUID dteId;

	@NotEmpty(message = "Indica los productos devueltos")
	private List<@Valid SaleItemDTO> items;

	@NotBlank(message = "Indica el motivo de la nota de crédito")
	@Size(max = 250)
	private String reason;

	/** true = la mercadería devuelta vuelve al inventario. */
	private boolean returnToStock = true;
}
