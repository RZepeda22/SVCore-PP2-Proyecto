package com.svcore.api_demo.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.svcore.api_demo.models.dtos.GeneralResponse;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.service.ReportService;

import lombok.RequiredArgsConstructor;

/** CU-06: reportes. from/to en formato AAAA-MM-DD (inclusivos); sin fechas, el mes en curso. */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@GetMapping("/summary")
	public ResponseEntity<GeneralResponse> summary(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return GeneralResponse.getResponse(reportService.summary(from, to));
	}

	@GetMapping("/sales-by-day")
	public ResponseEntity<GeneralResponse> salesByDay(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return GeneralResponse.getResponse(reportService.salesByDay(from, to));
	}

	@GetMapping("/sales-by-payment")
	public ResponseEntity<GeneralResponse> salesByPayment(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return GeneralResponse.getResponse(reportService.salesByPayment(from, to));
	}

	@GetMapping("/top-products")
	public ResponseEntity<GeneralResponse> topProducts(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "limit", defaultValue = "10") int limit) {
		return GeneralResponse.getResponse(reportService.topProducts(from, to, limit));
	}

	@GetMapping("/iva-book")
	public ResponseEntity<GeneralResponse> ivaBook(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "type", required = false) DteType type) {
		return GeneralResponse.getResponse(reportService.ivaBook(from, to, type));
	}

	/** Libro de ventas (IVA) en CSV para Excel. */
	@GetMapping("/iva-book/csv")
	public ResponseEntity<byte[]> ivaBookCsv(
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "type", required = false) DteType type) {
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libro-iva.csv\"")
				.body(reportService.ivaBookCsv(from, to, type).getBytes(StandardCharsets.UTF_8));
	}
}
