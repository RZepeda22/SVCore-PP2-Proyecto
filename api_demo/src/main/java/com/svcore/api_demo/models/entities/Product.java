package com.svcore.api_demo.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@Column(name = "code", nullable = false, unique = true, length = 25)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	/** Existencias en la bodega central. La base rechaza valores negativos (CHECK). */
	@Column(name = "stock", nullable = false)
	private Integer stock;

	/** Por debajo de este valor el producto aparece en las alertas de inventario. */
	@Column(name = "min_stock", nullable = false)
	private Integer minStock;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private ProductCategory category;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
