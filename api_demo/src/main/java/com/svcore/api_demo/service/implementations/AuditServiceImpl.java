package com.svcore.api_demo.service.implementations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.svcore.api_demo.models.dtos.AuditVerificationDTO;
import com.svcore.api_demo.models.dtos.PageDTO;
import com.svcore.api_demo.models.entities.AuditLog;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.repositories.AuditLogRepository;
import com.svcore.api_demo.service.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Bitácora con cadena de hashes: hash = SHA-256(hash anterior | datos del registro).
 * Modificar o borrar un registro directamente en la base rompe la cadena y verify() lo detecta.
 */
@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

	static final String GENESIS = "0".repeat(64);
	private static final String SYSTEM_USER = "sistema";
	private static final int MAX_PAGE_SIZE = 200;

	private final AuditLogRepository auditLogRepository;
	private final TransactionTemplate newTransaction;

	public AuditServiceImpl(AuditLogRepository auditLogRepository, PlatformTransactionManager transactionManager) {
		this.auditLogRepository = auditLogRepository;
		this.newTransaction = new TransactionTemplate(transactionManager);
		this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	public void record(AuditAction action, String entity, Object entityId, String detail) {
		recordAs(currentUsername(), action, entity, entityId, detail);
	}

	@Override
	public void recordAs(String username, AuditAction action, String entity, Object entityId, String detail) {
		AuditLog entry = new AuditLog();
		entry.setOccurredAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)); // misma precisión que DATETIME(6)
		entry.setUsername(truncate(username == null ? SYSTEM_USER : username, 50));
		entry.setAction(action);
		entry.setEntity(entity);
		entry.setEntityId(entityId == null ? null : truncate(String.valueOf(entityId), 36));
		entry.setDetail(truncate(detail, 500));
		entry.setIpAddress(currentIp());

		// Dentro de una transacción, solo queda registrado lo que realmente se confirmó
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					write(entry);
				}
			});
		} else {
			write(entry);
		}
	}

	/** synchronized + transacción propia: dos registros simultáneos nunca comparten el mismo hash anterior. */
	private synchronized void write(AuditLog entry) {
		try {
			newTransaction.executeWithoutResult(status -> {
				String previous = auditLogRepository.findTopByOrderByIdDesc().map(AuditLog::getHash).orElse(GENESIS);
				entry.setPreviousHash(previous);
				entry.setHash(hash(previous, entry));
				auditLogRepository.save(entry);
			});
		} catch (RuntimeException e) {
			// Un fallo de la bitácora no debe tumbar la operación del usuario, pero queda en el log del servidor
			log.error("No se pudo escribir en la bitácora: {} {}", entry.getAction(), entry.getEntityId(), e);
		}
	}

	@Override
	public PageDTO<AuditLog> search(String username, AuditAction action, LocalDate from, LocalDate to, int page,
									int size) {
		return PageDTO.from(auditLogRepository.search(
				username == null || username.isBlank() ? null : username.trim(),
				action,
				from == null ? null : from.atStartOfDay(),
				to == null ? null : to.plusDays(1).atStartOfDay(),
				PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE))));
	}

	@Override
	public AuditVerificationDTO verify() {
		String expectedPrevious = GENESIS;
		long checked = 0;
		long lastId = 0;
		List<AuditLog> batch;
		while (!(batch = auditLogRepository.findTop500ByIdGreaterThanOrderByIdAsc(lastId)).isEmpty()) {
			for (AuditLog entry : batch) {
				checked++;
				if (!entry.getPreviousHash().equals(expectedPrevious)) {
					return new AuditVerificationDTO(false, checked, entry.getId(),
							"Falta un registro o se alteró el orden antes del registro " + entry.getId());
				}
				if (!entry.getHash().equals(hash(entry.getPreviousHash(), entry))) {
					return new AuditVerificationDTO(false, checked, entry.getId(),
							"El registro " + entry.getId() + " fue modificado después de escribirse");
				}
				expectedPrevious = entry.getHash();
				lastId = entry.getId();
			}
		}
		return new AuditVerificationDTO(true, checked, null, "La bitácora está íntegra");
	}

	static String hash(String previous, AuditLog entry) {
		String content = String.join("|", previous, String.valueOf(entry.getOccurredAt()), entry.getUsername(),
				entry.getAction().name(), Objects.toString(entry.getEntity(), ""),
				Objects.toString(entry.getEntityId(), ""), Objects.toString(entry.getDetail(), ""),
				Objects.toString(entry.getIpAddress(), ""));
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}

	private static String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return SYSTEM_USER;
	}

	/** Se usa la IP de la conexión, no cabeceras como X-Forwarded-For que el cliente puede falsificar. */
	private static String currentIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return truncate(attributes.getRequest().getRemoteAddr(), 45);
		}
		return null;
	}

	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max);
	}
}
