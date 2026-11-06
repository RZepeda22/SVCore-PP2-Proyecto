package com.svcore.api_demo.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureVerificationDTO {

	/** La firma corresponde a la llave pública del emisor. */
	private boolean valid;
	/** El contenido firmado es idéntico al JSON guardado del DTE. */
	private boolean matchesStoredJson;
	private String algorithm;
	private String keyId;
	private String message;
}
