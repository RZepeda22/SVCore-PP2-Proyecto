package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.svcore.api_demo.models.enums.TaxpayerType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cliente con su clasificación fiscal (contribuyente con NRC o no contribuyente). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "taxpayer_type", nullable = false, length = 20)
	private TaxpayerType taxpayerType;

	@Column(name = "nit", length = 20)
	private String nit;

	@Column(name = "nrc", length = 10)
	private String nrc;

	@Column(name = "email", length = 100)
	private String email;

	@Column(name = "address", length = 200)
	private String address;

	@Column(name = "economic_activity_code", length = 6)
	private String economicActivityCode;

	@Column(name = "economic_activity", length = 150)
	private String economicActivity;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "department_code", length = 2)
	private String departmentCode;

	@Column(name = "municipality_code", length = 2)
	private String municipalityCode;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
