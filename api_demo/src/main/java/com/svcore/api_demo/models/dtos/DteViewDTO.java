package com.svcore.api_demo.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.PaymentMethod;
import com.svcore.api_demo.models.enums.TaxpayerType;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Comprobante completo: alimenta las pantallas dte-emitido, dte-rechazado y dte-contingencia.
 * El frontend decide la pantalla según {@code status}.
 */
@Data
public class DteViewDTO {

	private UUID id;
	private Integer saleNumber;
	private String cashRegister;
	private DteType type;
	private String typeLabel;
	private DteStatus status;
	private String generationCode;
	private String controlNumber;
	private String environment;
	private String receptionStamp;
	private String rejectionCode;
	private String rejectionMessage;
	private int attempts;
	private LocalDateTime issuedAt;
	private LocalDateTime lastAttemptAt;
	private LocalDateTime stampedAt;
	private Emitter emitter;
	private Receiver receiver;
	private List<Item> items;
	private Payment payment;
	private BigDecimal subtotal;
	private BigDecimal discount;
	private BigDecimal tax;
	private BigDecimal total;
	private String totalInWords;

	/** 1 = previo (transmisión inmediata), 2 = diferido (emitido en contingencia). */
	private int modelType;
	/** 1 = normal, 2 = por contingencia. */
	private int operationType;
	private String contingencyEventStamp;

	/** Documento que modifica (por ejemplo, el CCF de una Nota de Crédito). */
	private UUID relatedDteId;
	private String relatedGenerationCode;
	private String relatedControlNumber;
	private String notes;

	private String invalidationReason;
	private String invalidationStamp;
	private LocalDateTime invalidatedAt;

	@Data
	@AllArgsConstructor
	public static class Emitter {
		private String businessName;
		private String tradeName;
		private String nit;
		private String nrc;
		private String economicActivity;
		private String establishment;
	}

	@Data
	@AllArgsConstructor
	public static class Receiver {
		/** null = Consumidor Final. Permite corregir los datos del cliente desde un DTE rechazado. */
		private UUID customerId;
		private String name;
		private TaxpayerType taxpayerType;
		private String nit;
		private String nrc;
		private String email;
		private String address;
	}

	@Data
	@AllArgsConstructor
	public static class Item {
		private Integer quantity;
		private UUID productId;
		private String description;
		private String code;
		private BigDecimal unitPrice;
		private BigDecimal nonSubjectSale;
		private BigDecimal exemptSale;
		private BigDecimal taxedSale;
	}

	@Data
	@AllArgsConstructor
	public static class Payment {
		private PaymentMethod method;
		private String label;
		private String code;
		private int operationCondition;
	}
}
