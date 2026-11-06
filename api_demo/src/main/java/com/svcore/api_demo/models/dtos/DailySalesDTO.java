package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailySalesDTO {

	private LocalDate date;
	private long documents;
	private BigDecimal subtotal;
	private BigDecimal tax;
	private BigDecimal total;
}
