package com.svcore.api_demo.models.dtos;

import com.svcore.api_demo.models.enums.DteStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Resultado de transmitir un DTE al Ministerio de Hacienda. */
@Data
@AllArgsConstructor
public class HaciendaResponseDTO {

	private DteStatus status;
	private String receptionStamp;
	private String code;
	private String message;

	public static HaciendaResponseDTO stamped(String receptionStamp) {
		return new HaciendaResponseDTO(DteStatus.SELLADO, receptionStamp, null, null);
	}

	public static HaciendaResponseDTO rejected(String code, String message) {
		return new HaciendaResponseDTO(DteStatus.RECHAZADO, null, code, message);
	}

	public static HaciendaResponseDTO unavailable() {
		return new HaciendaResponseDTO(DteStatus.CONTINGENCIA, null, null, null);
	}
}
