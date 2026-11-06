package com.svcore.api_demo.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Error de negocio con un mensaje apto para mostrarse al usuario (RNF-09).
 * GlobalExceptionHandler lo convierte en GeneralResponse con el status indicado.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final HttpStatus status;

	public BusinessException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
