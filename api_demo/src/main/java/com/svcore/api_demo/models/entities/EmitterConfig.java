package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CU-09: configuración fiscal del emisor. Siempre existe una única fila con id = 1. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "emitter_config")
public class EmitterConfig {

	public static final int SINGLE_ID = 1;

	@Id
	@Column(name = "id")
	private Integer id;

	@Column(name = "business_name", nullable = false, length = 250)
	private String businessName;

	@Column(name = "trade_name", nullable = false, length = 150)
	private String tradeName;

	@Column(name = "nit", nullable = false, length = 17)
	private String nit;

	@Column(name = "nrc", nullable = false, length = 10)
	private String nrc;

	@Column(name = "economic_activity_code", nullable = false, length = 6)
	private String economicActivityCode;

	@Column(name = "economic_activity", nullable = false, length = 150)
	private String economicActivity;

	/** Catálogo de tipo de establecimiento: 01 sucursal, 02 casa matriz, 04 bodega, 07 predio. */
	@Column(name = "establishment_type", nullable = false, length = 2)
	private String establishmentType;

	@Column(name = "establishment_name", nullable = false, length = 100)
	private String establishmentName;

	@Column(name = "department_code", nullable = false, length = 2)
	private String departmentCode;

	@Column(name = "municipality_code", nullable = false, length = 2)
	private String municipalityCode;

	@Column(name = "address_detail", nullable = false, length = 200)
	private String addressDetail;

	@Column(name = "phone", nullable = false, length = 30)
	private String phone;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "establishment_code", nullable = false, length = 4)
	private String establishmentCode;

	@Column(name = "point_of_sale_code", nullable = false, length = 4)
	private String pointOfSaleCode;

	@Column(name = "cash_register", nullable = false, length = 50)
	private String cashRegister;

	/** 00 = pruebas, 01 = producción. */
	@Column(name = "environment", nullable = false, length = 2)
	private String environment;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
