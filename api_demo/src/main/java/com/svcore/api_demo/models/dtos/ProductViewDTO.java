package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.svcore.api_demo.models.entities.Product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto tal como lo muestra el punto de venta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewDTO {

	private UUID id;
	private String name;
	private String code;
	private BigDecimal price;
	private Integer stock;
	private Integer minStock;
	private String category;
	private String categoryCode;

	public static ProductViewDTO from(Product product) {
		return new ProductViewDTO(product.getId(), product.getName(), product.getCode(), product.getPrice(),
				product.getStock(), product.getMinStock(), product.getCategory().getName(),
				product.getCategory().getCode());
	}
}
