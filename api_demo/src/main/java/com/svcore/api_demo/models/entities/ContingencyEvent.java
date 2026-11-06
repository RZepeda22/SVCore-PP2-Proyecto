package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.svcore.api_demo.models.enums.ContingencyStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de contingencia: informa a Hacienda el periodo en que no estuvo disponible y los DTE emitidos
 * en ese tiempo. Debe ser aceptado antes de transmitir el lote de documentos.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contingency_event")
public class ContingencyEvent {

	/** Catálogo de tipo de contingencia: 1 = no disponibilidad del sistema del Ministerio de Hacienda. */
	public static final int TYPE_MH_UNAVAILABLE = 1;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@Column(name = "generation_code", nullable = false, unique = true, length = 36)
	private String generationCode;

	@Column(name = "contingency_type", nullable = false)
	private Integer contingencyType;

	@Column(name = "reason", length = 500)
	private String reason;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ContingencyStatus status;

	@Column(name = "reception_stamp", length = 100)
	private String receptionStamp;

	@Column(name = "json_document", columnDefinition = "LONGTEXT")
	private String jsonDocument;

	@Column(name = "signed_document", columnDefinition = "LONGTEXT")
	private String signedDocument;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
