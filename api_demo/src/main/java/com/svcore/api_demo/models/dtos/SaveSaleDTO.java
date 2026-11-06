package com.svcore.api_demo.models.dtos;

import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveSaleDTO {

	/** null = Consumidor Final. */
	private UUID customerId;

	@NotNull(message = "Selecciona el tipo de documento")
	private DteType docType;

	@NotNull(message = "Selecciona la forma de pago")
	private PaymentMethod paymentMethod;

	@NotEmpty(message = "Agrega productos al carrito antes de confirmar la venta")
	private List<@Valid SaleItemDTO> items;
}
