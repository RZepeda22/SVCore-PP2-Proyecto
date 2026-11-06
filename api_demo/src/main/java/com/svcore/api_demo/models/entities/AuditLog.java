package com.svcore.api_demo.models.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.svcore.api_demo.models.enums.AuditAction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de la bitácora (RNF-06). Hibernate no permite actualizarlo (@Immutable), el usuario de la base
 * no puede borrar (sin DELETE) y cada registro encadena el hash del anterior para detectar alteraciones.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 40)
	private AuditAction action;

	@Column(name = "entity", length = 40)
	private String entity;

	@Column(name = "entity_id", length = 36)
	private String entityId;

	@Column(name = "detail", length = 500)
	private String detail;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "previous_hash", nullable = false, length = 64)
	private String previousHash;

	@Column(name = "hash", nullable = false, length = 64)
	private String hash;
}
