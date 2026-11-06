package com.svcore.api_demo.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sale")
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@Column(name = "sale_number", nullable = false, unique = true)
	private Integer saleNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	/** null = Consumidor Final. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Column(name = "cash_register", nullable = false, length = 50)
	private String cashRegister;

	@Enumerated(EnumType.STRING)
	@Column(name = "doc_type", nullable = false, length = 20)
	private DteType docType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "discount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discount;

	@Column(name = "tax", nullable = false, precision = 12, scale = 2)
	private BigDecimal tax;

	@Column(name = "total", nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	/** Motivo de la Nota de Crédito u otras observaciones. */
	@Column(name = "notes", length = 250)
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SaleDetail> details = new ArrayList<>();

	public void addDetail(SaleDetail detail) {
		detail.setSale(this);
		details.add(detail);
	}
}
