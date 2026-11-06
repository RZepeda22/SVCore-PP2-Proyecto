package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.svcore.api_demo.models.enums.DteStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Fila de la tabla de documentos electrónicos (dte.html). */
@Data
@AllArgsConstructor
public class DteSummaryDTO {

	private UUID id;
	private Integer saleNumber;
	private String generationCode;
	private String type;
	private String customerName;
	private String customerNrc;
	private BigDecimal subtotal;
	private BigDecimal tax;
	private BigDecimal total;
	private DteStatus status;
	private LocalDateTime issuedAt;
}
