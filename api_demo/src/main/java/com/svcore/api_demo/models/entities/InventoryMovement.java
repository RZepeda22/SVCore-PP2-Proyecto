package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.svcore.api_demo.models.enums.MovementType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Trazabilidad de cada entrada o salida de inventario (decisión de diseño 4). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_movement")
public class InventoryMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	/** Negativo para salidas, positivo para entradas. */
	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 20)
	private MovementType movementType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sale_id")
	private Sale sale;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public InventoryMovement(Product product, Integer quantity, MovementType movementType, Sale sale, User user) {
		this.product = product;
		this.quantity = quantity;
		this.movementType = movementType;
		this.sale = sale;
		this.user = user;
	}
}
