package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/** CU-06: resumen de ventas del periodo. Los montos son netos: ventas menos notas de crédito. */
@Data
@AllArgsConstructor
public class SalesSummaryDTO {

	private LocalDate from;
	private LocalDate to;
	private long sales;
	private long creditNotes;
	private long invalidated;
	private BigDecimal subtotal;
	private BigDecimal tax;
	private BigDecimal total;
	private BigDecimal invalidatedTotal;
	private Map<String, Long> documentsByStatus;
	private Map<String, Long> documentsByType;
}
