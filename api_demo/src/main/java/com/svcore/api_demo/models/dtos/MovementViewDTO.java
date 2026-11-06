package com.svcore.api_demo.models.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.enums.MovementType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovementViewDTO {

	private UUID id;
	private UUID productId;
	private String productCode;
	private String productName;
	private int quantity;
	private MovementType type;
	private Integer saleNumber;
	private String username;
	private LocalDateTime createdAt;

	public static MovementViewDTO from(InventoryMovement m) {
		return new MovementViewDTO(m.getId(), m.getProduct().getId(), m.getProduct().getCode(), m.getProduct().getName(),
				m.getQuantity(), m.getMovementType(), m.getSale() != null ? m.getSale().getSaleNumber() : null,
				m.getUser().getUsername(), m.getCreatedAt());
	}
}
