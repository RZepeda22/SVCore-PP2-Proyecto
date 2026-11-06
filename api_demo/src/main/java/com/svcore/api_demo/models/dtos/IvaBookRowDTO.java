package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.svcore.api_demo.models.enums.DteStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Fila del libro de ventas (IVA). Las notas de crédito restan. */
@Data
@AllArgsConstructor
public class IvaBookRowDTO {

	private LocalDateTime issuedAt;
	private String type;
	private String controlNumber;
	private String generationCode;
	private String receptionStamp;
	private String customerName;
	private String customerNit;
	private String customerNrc;
	private BigDecimal subtotal;
	private BigDecimal tax;
	private BigDecimal total;
	private DteStatus status;
}
