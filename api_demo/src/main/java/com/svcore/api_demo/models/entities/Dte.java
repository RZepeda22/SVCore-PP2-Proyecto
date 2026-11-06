package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.TaxpayerType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Documento tributario electrónico de una venta.
 * Guarda una copia del receptor para que un DTE sellado no cambie si después se edita el cliente.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dte")
public class Dte {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id", nullable = false, unique = true)
	private Sale sale;

	@Enumerated(EnumType.STRING)
	@Column(name = "dte_type", nullable = false, length = 20)
	private DteType type;

	@Column(name = "generation_code", nullable = false, unique = true, length = 36)
	private String generationCode;

	@Column(name = "control_number", nullable = false, unique = true, length = 31)
	private String controlNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DteStatus status;

	@Column(name = "environment", nullable = false, length = 2)
	private String environment;

	@Column(name = "receiver_name", nullable = false, length = 150)
	private String receiverName;

	@Enumerated(EnumType.STRING)
	@Column(name = "receiver_taxpayer_type", length = 20)
	private TaxpayerType receiverTaxpayerType;

	@Column(name = "receiver_nit", length = 20)
	private String receiverNit;

	@Column(name = "receiver_nrc", length = 10)
	private String receiverNrc;

	@Column(name = "receiver_email", length = 100)
	private String receiverEmail;

	@Column(name = "receiver_address", length = 200)
	private String receiverAddress;

	@Column(name = "reception_stamp", length = 100)
	private String receptionStamp;

	@Column(name = "rejection_code", length = 10)
	private String rejectionCode;

	@Column(name = "rejection_message", length = 500)
	private String rejectionMessage;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "last_attempt_at")
	private LocalDateTime lastAttemptAt;

	@Column(name = "stamped_at")
	private LocalDateTime stampedAt;

	/** Modelo de facturación: 1 = previo (transmisión inmediata), 2 = diferido (contingencia). */
	@Column(name = "model_type", nullable = false)
	private int modelType = 1;

	/** Tipo de transmisión: 1 = normal, 2 = por contingencia. */
	@Column(name = "operation_type", nullable = false)
	private int operationType = 1;

	/** Documento que modifica (por ejemplo, el CCF al que aplica una Nota de Crédito). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "related_dte_id")
	private Dte relatedDte;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contingency_event_id")
	private ContingencyEvent contingencyEvent;

	/** JSON del DTE según el esquema de Hacienda. */
	@Column(name = "json_document", columnDefinition = "LONGTEXT")
	private String jsonDocument;

	/** JSON firmado (JWS compacto) que se transmite a Hacienda. */
	@Column(name = "signed_document", columnDefinition = "LONGTEXT")
	private String signedDocument;
}
