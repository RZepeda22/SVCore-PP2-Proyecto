package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CU-04: evento de invalidación (anulación) de un DTE sellado. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invalidation_event")
public class InvalidationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dte_id", nullable = false, unique = true)
	private Dte dte;

	@Column(name = "generation_code", nullable = false, unique = true, length = 36)
	private String generationCode;

	/** 1 = error en la información, 2 = rescindir la operación, 3 = otro. */
	@Column(name = "invalidation_type", nullable = false)
	private Integer invalidationType;

	@Column(name = "reason", nullable = false, length = 250)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by", nullable = false)
	private User requestedBy;

	@Column(name = "reception_stamp", length = 100)
	private String receptionStamp;

	@Column(name = "json_document", nullable = false, columnDefinition = "LONGTEXT")
	private String jsonDocument;

	@Column(name = "signed_document", nullable = false, columnDefinition = "LONGTEXT")
	private String signedDocument;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
