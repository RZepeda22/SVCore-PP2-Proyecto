package com.svcore.api_demo.service;

import java.time.LocalDate;
import java.util.List;

import com.svcore.api_demo.models.dtos.DailySalesDTO;
import com.svcore.api_demo.models.dtos.IvaBookRowDTO;
import com.svcore.api_demo.models.dtos.PaymentSalesDTO;
import com.svcore.api_demo.models.dtos.SalesSummaryDTO;
import com.svcore.api_demo.models.dtos.TopProductDTO;
import com.svcore.api_demo.models.enums.DteType;

/** CU-06: reportes. Las fechas son inclusivas; sin fechas se usa el mes en curso. */
public interface ReportService {

	SalesSummaryDTO summary(LocalDate from, LocalDate to);

	List<DailySalesDTO> salesByDay(LocalDate from, LocalDate to);

	List<PaymentSalesDTO> salesByPayment(LocalDate from, LocalDate to);

	List<TopProductDTO> topProducts(LocalDate from, LocalDate to, int limit);

	/** Libro de ventas (IVA). type opcional. */
	List<IvaBookRowDTO> ivaBook(LocalDate from, LocalDate to, DteType type);

	String ivaBookCsv(LocalDate from, LocalDate to, DteType type);
}
