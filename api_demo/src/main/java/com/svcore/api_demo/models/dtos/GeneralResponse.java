package com.svcore.api_demo.models.dtos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Envoltura común de todas las respuestas: { "message": ..., "data": ... }. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneralResponse {

	private String message;
	private Object data;

	public static ResponseEntity<GeneralResponse> getResponse(HttpStatus status, String message, Object data) {
		return new ResponseEntity<>(new GeneralResponse(message, data), status);
	}

	public static ResponseEntity<GeneralResponse> getResponse(HttpStatus status, String message) {
		return getResponse(status, message, null);
	}

	public static ResponseEntity<GeneralResponse> getResponse(HttpStatus status, Object data) {
		return getResponse(status, status.getReasonPhrase(), data);
	}

	public static ResponseEntity<GeneralResponse> getResponse(Object data) {
		return getResponse(HttpStatus.OK, data);
	}
}
