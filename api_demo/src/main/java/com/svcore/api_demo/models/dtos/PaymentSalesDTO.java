package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;

import com.svcore.api_demo.models.enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentSalesDTO {

	private PaymentMethod method;
	private String label;
	private long sales;
	private BigDecimal total;
}
