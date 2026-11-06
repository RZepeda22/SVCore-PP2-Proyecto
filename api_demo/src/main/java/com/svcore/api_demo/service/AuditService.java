package com.svcore.api_demo.service;

import java.time.LocalDate;

import com.svcore.api_demo.models.dtos.AuditVerificationDTO;
import com.svcore.api_demo.models.dtos.PageDTO;
import com.svcore.api_demo.models.entities.AuditLog;
import com.svcore.api_demo.models.enums.AuditAction;

/** RNF-06: bitácora inmodificable de las acciones del sistema. */
public interface AuditService {

	/** Registra la acción del usuario autenticado. Si hay una transacción activa, se escribe solo si confirma. */
	void record(AuditAction action, String entity, Object entityId, String detail);

	/** Registra una acción a nombre de un usuario concreto (por ejemplo, intentos de inicio de sesión). */
	void recordAs(String username, AuditAction action, String entity, Object entityId, String detail);

	PageDTO<AuditLog> search(String username, AuditAction action, LocalDate from, LocalDate to, int page, int size);

	/** Recalcula la cadena de hashes y detecta registros alterados o eliminados. */
	AuditVerificationDTO verify();
}
