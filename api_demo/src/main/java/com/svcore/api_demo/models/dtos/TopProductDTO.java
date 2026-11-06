package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductDTO {

	private UUID productId;
	private String code;
	private String name;
	private long quantity;
	private BigDecimal subtotal;
}
