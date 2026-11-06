package com.svcore.api_demo.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.svcore.api_demo.models.dtos.GeneralResponse;

import lombok.extern.slf4j.Slf4j;

/** Reemplaza los try/catch de cada controller: toda respuesta de error sale como GeneralResponse. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<GeneralResponse> handleBusiness(BusinessException e) {
		return GeneralResponse.getResponse(e.getStatus(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<GeneralResponse> handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return GeneralResponse.getResponse(HttpStatus.BAD_REQUEST, "Datos inválidos", errors);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<GeneralResponse> handleMalformed(Exception e) {
		return GeneralResponse.getResponse(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato inválido");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<GeneralResponse> handleUnexpected(Exception e) {
		// Errores propios de Spring MVC (404, 405, parámetro faltante...) conservan su status
		if (e instanceof ErrorResponse errorResponse) {
			HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
			return GeneralResponse.getResponse(status != null ? status : HttpStatus.BAD_REQUEST,
					errorResponse.getBody().getDetail());
		}
		// No se expone el detalle técnico al cliente; queda en el log
		log.error("Error no controlado", e);
		return GeneralResponse.getResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocurrió un error interno. Intenta de nuevo.");
	}
}
