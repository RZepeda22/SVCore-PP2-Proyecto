package com.svcore.api_demo.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Resultado de recalcular la cadena de hashes de la bitácora. */
@Data
@AllArgsConstructor
public class AuditVerificationDTO {

	private boolean valid;
	private long checkedEntries;
	private Long firstInvalidId;
	private String message;
}
