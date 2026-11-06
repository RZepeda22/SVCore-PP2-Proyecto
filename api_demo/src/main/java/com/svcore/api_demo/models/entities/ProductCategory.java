package com.svcore.api_demo.models.entities;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_category")
public class ProductCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	/** Código que usa el POS para filtrar: bikes, components, clothing, accessories. */
	@Column(name = "code", nullable = false, unique = true, length = 20)
	private String code;

	@Column(name = "name", nullable = false, length = 50)
	private String name;
}
