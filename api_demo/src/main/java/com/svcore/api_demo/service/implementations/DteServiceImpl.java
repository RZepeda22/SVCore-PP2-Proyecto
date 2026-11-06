package com.svcore.api_demo.service.implementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.DteSummaryDTO;
import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.dtos.InvalidateDteDTO;
import com.svcore.api_demo.models.dtos.SignatureVerificationDTO;
import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Customer;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.EmitterConfig;
import com.svcore.api_demo.models.entities.InvalidationEvent;
import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.MovementType;
import com.svcore.api_demo.repositories.DteRepository;
import com.svcore.api_demo.repositories.InvalidationEventRepository;
import com.svcore.api_demo.repositories.InventoryMovementRepository;
import com.svcore.api_demo.repositories.ProductRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.ContingencyService;
import com.svcore.api_demo.service.DteService;
import com.svcore.api_demo.service.EmitterConfigService;
import com.svcore.api_demo.service.HaciendaService;
import com.svcore.api_demo.service.SignatureService;
import com.svcore.api_demo.service.UserService;
import com.svcore.api_demo.utils.DteJsonBuilder;
import com.svcore.api_demo.utils.NumberToWords;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ciclo de vida del DTE: generar el JSON, firmarlo, transmitirlo, manejar la contingencia,
 * reintentar e invalidar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DteServiceImpl implements DteService {

	private static final String CONSUMIDOR_FINAL = "Consumidor Final";
	private static final int MODEL_PREVIOUS = 1;
	private static final int MODEL_DEFERRED = 2;
	private static final int OPERATION_NORMAL = 1;
	private static final int OPERATION_CONTINGENCY = 2;

	private final DteRepository dteRepository;
	private final InvalidationEventRepository invalidationEventRepository;
	private final InventoryMovementRepository movementRepository;
	private final ProductRepository productRepository;
	private final HaciendaService haciendaService;
	private final SignatureService signatureService;
	private final ContingencyService contingencyService;
	private final EmitterConfigService emitterConfigService;
	private final UserService userService;
	private final AuditService auditService;
	private final DteJsonBuilder dteJsonBuilder;

	// ------------------------------------------------------------------
	// Emisión
	// ------------------------------------------------------------------

	@Override
	@Transactional
	public DteViewDTO issue(Sale sale) {
		return issue(sale, null);
	}

	@Override
	@Transactional
	public DteViewDTO issue(Sale sale, Dte related) {
		EmitterConfig emitter = emitterConfigService.get();

		Dte dte = new Dte();
		dte.setSale(sale);
		dte.setType(sale.getDocType());
		dte.setRelatedDte(related);
		dte.setGenerationCode(UUID.randomUUID().toString().toUpperCase());
		dte.setControlNumber(buildControlNumber(sale.getDocType(), emitter));
		dte.setEnvironment(emitter.getEnvironment());
		dte.setIssuedAt(LocalDateTime.now());
		dte.setModelType(MODEL_PREVIOUS);
		dte.setOperationType(OPERATION_NORMAL);
		copyReceiver(dte, sale.getCustomer());

		// Con una contingencia abierta el documento se emite directamente en modelo diferido, sin hacer esperar
		// a la caja; la cola de reintentos lo transmite cuando Hacienda acepte el evento de contingencia
		boolean inContingency = contingencyService.findOpenEvent().isPresent();
		if (inContingency) {
			attachToContingency(dte);
		}
		buildDocument(dte, emitter);
		if (inContingency) {
			dte.setStatus(DteStatus.CONTINGENCIA);
		} else {
			transmit(dte);
		}
		dteRepository.save(dte);

		auditService.record(AuditAction.DTE_EMITIDO, "dte", dte.getId(),
				dte.getType().getShortName() + " " + dte.getControlNumber() + " -> " + dte.getStatus());
		return toView(dte);
	}

	/** Genera el JSON según el esquema de Hacienda y lo firma (JWS RS512). */
	private void buildDocument(Dte dte, EmitterConfig emitter) {
		String json = dteJsonBuilder.buildDte(dte, emitter);
		dte.setJsonDocument(json);
		dte.setSignedDocument(signatureService.sign(json));
	}

	/**
	 * Transmite el DTE. Si Hacienda no responde, el documento pasa a modelo diferido dentro del evento de
	 * contingencia abierto y se vuelve a firmar; la venta continúa normalmente (decisión de diseño 1).
	 */
	private void transmit(Dte dte) {
		dte.setAttempts(dte.getAttempts() + 1);
		dte.setLastAttemptAt(LocalDateTime.now());

		HaciendaResponseDTO response = haciendaService.transmit(dte);
		if (response.getStatus() == DteStatus.CONTINGENCIA && dte.getOperationType() == OPERATION_NORMAL) {
			attachToContingency(dte);
			buildDocument(dte, emitterConfigService.get());
		}
		dte.setStatus(response.getStatus());
		dte.setReceptionStamp(response.getReceptionStamp());
		dte.setRejectionCode(response.getCode());
		dte.setRejectionMessage(response.getMessage());
		if (response.getStatus() == DteStatus.SELLADO) {
			dte.setStampedAt(LocalDateTime.now());
		}
	}

	private void attachToContingency(Dte dte) {
		ContingencyEvent event = contingencyService.currentOpenEvent();
		dte.setContingencyEvent(event);
		dte.setModelType(MODEL_DEFERRED);
		dte.setOperationType(OPERATION_CONTINGENCY);
	}

	/** Formato DTE-{tipo}-{establecimiento}{punto de venta}-{correlativo de 15 dígitos}. */
	private String buildControlNumber(DteType type, EmitterConfig emitter) {
		// Suficiente para la demo; con varias cajas en producción conviene una secuencia por tipo
		long next = dteRepository.countByType(type) + 1;
		return String.format("DTE-%s-%s%s-%015d", type.getCode(), emitter.getEstablishmentCode(),
				emitter.getPointOfSaleCode(), next);
	}

	private void copyReceiver(Dte dte, Customer customer) {
		dte.setReceiverName(customer == null ? CONSUMIDOR_FINAL : customer.getName());
		dte.setReceiverTaxpayerType(customer == null ? null : customer.getTaxpayerType());
		dte.setReceiverNit(customer == null ? null : customer.getNit());
		dte.setReceiverNrc(customer == null ? null : customer.getNrc());
		dte.setReceiverEmail(customer == null ? null : customer.getEmail());
		dte.setReceiverAddress(customer == null ? null : customer.getAddress());
	}

	// ------------------------------------------------------------------
	// Consultas
	// ------------------------------------------------------------------

	@Override
	@Transactional(readOnly = true)
	public List<DteSummaryDTO> findAll(DteStatus status) {
		List<Dte> dtes = status == null
				? dteRepository.findAllByOrderByIssuedAtDesc()
				: dteRepository.findAllByStatusOrderByIssuedAtDesc(status);
		return dtes.stream().map(this::toSummary).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DteViewDTO findById(UUID dteId) {
		return toView(getDte(dteId));
	}

	@Override
	@Transactional(readOnly = true)
	public String getJson(UUID dteId) {
		return requireDocument(getDte(dteId).getJsonDocument());
	}

	@Override
	@Transactional(readOnly = true)
	public String getSignedDocument(UUID dteId) {
		return requireDocument(getDte(dteId).getSignedDocument());
	}

	/** Los DTE emitidos antes de la migración V3 no tienen JSON ni firma guardados. */
	private static String requireDocument(String document) {
		if (document == null) {
			throw new BusinessException(HttpStatus.NOT_FOUND,
					"Este documento se emitió antes de incorporar el JSON firmado y no tiene esa información");
		}
		return document;
	}

	@Override
	@Transactional(readOnly = true)
	public SignatureVerificationDTO verifySignature(UUID dteId) {
		Dte dte = getDte(dteId);
		if (dte.getSignedDocument() == null) {
			return new SignatureVerificationDTO(false, false, signatureService.algorithm(), signatureService.keyId(),
					"Este documento se emitió antes de incorporar la firma electrónica");
		}
		boolean valid = signatureService.verify(dte.getSignedDocument());
		boolean matches = valid && signatureService.payload(dte.getSignedDocument()).equals(dte.getJsonDocument());
		String message = !valid ? "La firma no corresponde a la llave del emisor o el documento fue alterado"
				: matches ? "Firma válida: el documento no fue modificado desde que se firmó"
				: "La firma es válida pero el JSON guardado no coincide con el contenido firmado";
		return new SignatureVerificationDTO(valid, matches, signatureService.algorithm(), signatureService.keyId(),
				message);
	}

	// ------------------------------------------------------------------
	// Reintentos y contingencia
	// ------------------------------------------------------------------

	@Override
	@Transactional
	public DteViewDTO retry(UUID dteId) {
		Dte dte = getDte(dteId);
		if (dte.getStatus() != DteStatus.CONTINGENCIA && dte.getStatus() != DteStatus.RECHAZADO) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"Solo se pueden reintentar documentos en contingencia o rechazados");
		}

		if (dte.getStatus() == DteStatus.RECHAZADO) {
			// Toma los datos ya corregidos del cliente ("Corregir datos y reintentar") y vuelve a firmar
			copyReceiver(dte, dte.getSale().getCustomer());
			buildDocument(dte, emitterConfigService.get());
			transmit(dte);
		} else if (contingencyService.ensureAccepted(dte.getContingencyEvent())) {
			transmit(dte);
			contingencyService.closeIfComplete(dte.getContingencyEvent());
		} else {
			// Hacienda sigue sin responder: ni el evento de contingencia pudo transmitirse
			dte.setAttempts(dte.getAttempts() + 1);
			dte.setLastAttemptAt(LocalDateTime.now());
		}
		auditService.record(AuditAction.DTE_REINTENTO, "dte", dte.getId(),
				dte.getControlNumber() + " -> " + dte.getStatus());
		return toView(dte);
	}

	@Override
	@Scheduled(fixedDelayString = "${svcore.hacienda.retry-delay-ms}",
			initialDelayString = "${svcore.hacienda.retry-delay-ms}")
	@Transactional
	public void retryPending() {
		List<Dte> pending = dteRepository.findAllByStatusOrderByIssuedAtAsc(DteStatus.CONTINGENCIA);
		if (pending.isEmpty()) {
			return;
		}
		// Primero el evento de contingencia de cada lote; si Hacienda lo acepta, se transmiten sus DTE
		Map<UUID, ContingencyEvent> events = new LinkedHashMap<>();
		pending.forEach(dte -> events.putIfAbsent(dte.getContingencyEvent().getId(), dte.getContingencyEvent()));

		int stamped = 0;
		for (ContingencyEvent event : events.values()) {
			if (!contingencyService.ensureAccepted(event)) {
				log.info("Cola de contingencia: Hacienda sigue sin responder ({} documentos pendientes)", pending.size());
				return;
			}
			for (Dte dte : pending) {
				if (dte.getContingencyEvent().getId().equals(event.getId())) {
					transmit(dte);
					if (dte.getStatus() == DteStatus.SELLADO) stamped++;
				}
			}
			contingencyService.closeIfComplete(event);
		}
		log.info("Cola de contingencia: {} reintentados, {} sellados", pending.size(), stamped);
	}

	// ------------------------------------------------------------------
	// Invalidación (CU-04)
	// ------------------------------------------------------------------

	@Override
	@Transactional
	public DteViewDTO invalidate(InvalidateDteDTO info) {
		Dte dte = getDte(info.getDteId());
		if (dte.getStatus() != DteStatus.SELLADO) {
			throw new BusinessException(HttpStatus.CONFLICT, "Solo se pueden invalidar documentos sellados por Hacienda");
		}
		if (invalidationEventRepository.existsByDte(dte)) {
			throw new BusinessException(HttpStatus.CONFLICT, "El documento ya tiene un evento de invalidación");
		}
		if (dteRepository.existsByRelatedDteAndStatusNot(dte, DteStatus.ANULADO)) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"El documento tiene notas de crédito vigentes: invalídalas primero");
		}

		User user = userService.getCurrentUser();
		InvalidationEvent event = new InvalidationEvent();
		event.setDte(dte);
		event.setGenerationCode(UUID.randomUUID().toString().toUpperCase());
		event.setInvalidationType(info.getInvalidationType());
		event.setReason(info.getReason().trim());
		event.setRequestedBy(user);
		String json = dteJsonBuilder.buildInvalidationEvent(event, dte, emitterConfigService.get(), user);
		event.setJsonDocument(json);
		event.setSignedDocument(signatureService.sign(json));

		HaciendaResponseDTO response = haciendaService.transmitInvalidation(event, dte);
		if (response.getStatus() == DteStatus.CONTINGENCIA) {
			throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
					"Hacienda no está disponible. La invalidación requiere conexión: intenta de nuevo más tarde.");
		}
		if (response.getStatus() == DteStatus.RECHAZADO) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"Hacienda rechazó la invalidación: " + response.getMessage());
		}
		event.setReceptionStamp(response.getReceptionStamp());
		invalidationEventRepository.save(event);

		reverseInventory(dte.getSale(), user);
		dte.setStatus(DteStatus.ANULADO);

		auditService.record(AuditAction.DTE_INVALIDADO, "dte", dte.getId(),
				dte.getControlNumber() + " invalidado: " + event.getReason());
		return toView(dte);
	}

	/** Decisión de diseño 3: al anular, cada movimiento de inventario de la venta se revierte. */
	private void reverseInventory(Sale sale, User user) {
		for (InventoryMovement movement : movementRepository.findAllBySale(sale)) {
			if (movement.getMovementType() == MovementType.REVERSO_ANULACION) continue;
			Product product = productRepository.findByIdForUpdate(movement.getProduct().getId()).orElseThrow();
			int newStock = product.getStock() - movement.getQuantity();
			if (newStock < 0) {
				throw new BusinessException(HttpStatus.CONFLICT, "No se puede invalidar: el stock de "
						+ product.getName() + " quedaría negativo");
			}
			product.setStock(newStock);
			movementRepository.save(new InventoryMovement(product, -movement.getQuantity(),
					MovementType.REVERSO_ANULACION, sale, user));
		}
	}

	// ------------------------------------------------------------------
	// Mapeo
	// ------------------------------------------------------------------

	private Dte getDte(UUID dteId) {
		return dteRepository.findById(dteId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
	}

	private DteSummaryDTO toSummary(Dte dte) {
		Sale sale = dte.getSale();
		return new DteSummaryDTO(dte.getId(), sale.getSaleNumber(), dte.getGenerationCode(),
				dte.getType().getShortName(), dte.getReceiverName(), dte.getReceiverNrc(), sale.getSubtotal(),
				sale.getTax(), sale.getTotal(), dte.getStatus(), dte.getIssuedAt());
	}

	private DteViewDTO toView(Dte dte) {
		Sale sale = dte.getSale();
		EmitterConfig emitter = emitterConfigService.get();
		DteViewDTO view = new DteViewDTO();
		view.setId(dte.getId());
		view.setSaleNumber(sale.getSaleNumber());
		view.setCashRegister(sale.getCashRegister());
		view.setType(dte.getType());
		view.setTypeLabel(dte.getType().getLabel());
		view.setStatus(dte.getStatus());
		view.setGenerationCode(dte.getGenerationCode());
		view.setControlNumber(dte.getControlNumber());
		view.setEnvironment(dte.getEnvironment());
		view.setReceptionStamp(dte.getReceptionStamp());
		view.setRejectionCode(dte.getRejectionCode());
		view.setRejectionMessage(dte.getRejectionMessage());
		view.setAttempts(dte.getAttempts());
		view.setIssuedAt(dte.getIssuedAt());
		view.setLastAttemptAt(dte.getLastAttemptAt());
		view.setStampedAt(dte.getStampedAt());
		view.setModelType(dte.getModelType());
		view.setOperationType(dte.getOperationType());
		view.setNotes(sale.getNotes());
		if (dte.getContingencyEvent() != null) {
			view.setContingencyEventStamp(dte.getContingencyEvent().getReceptionStamp());
		}
		if (dte.getRelatedDte() != null) {
			view.setRelatedDteId(dte.getRelatedDte().getId());
			view.setRelatedGenerationCode(dte.getRelatedDte().getGenerationCode());
			view.setRelatedControlNumber(dte.getRelatedDte().getControlNumber());
		}
		if (dte.getStatus() == DteStatus.ANULADO) {
			invalidationEventRepository.findByDte(dte).ifPresent(event -> {
				view.setInvalidationReason(event.getReason());
				view.setInvalidationStamp(event.getReceptionStamp());
				view.setInvalidatedAt(event.getCreatedAt());
			});
		}

		view.setEmitter(new DteViewDTO.Emitter(emitter.getBusinessName(), emitter.getTradeName(), emitter.getNit(),
				emitter.getNrc(), emitter.getEconomicActivity(),
				emitter.getEstablishmentName() + " · " + sale.getCashRegister()));

		view.setReceiver(new DteViewDTO.Receiver(
				sale.getCustomer() != null ? sale.getCustomer().getId() : null,
				dte.getReceiverName(), dte.getReceiverTaxpayerType(),
				dte.getReceiverNit(), dte.getReceiverNrc(), dte.getReceiverEmail(), dte.getReceiverAddress()));

		view.setItems(sale.getDetails().stream()
				.map(detail -> new DteViewDTO.Item(detail.getQuantity(), detail.getProduct().getId(),
						detail.getProduct().getName(), detail.getProduct().getCode(), detail.getUnitPrice(),
						BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), detail.getLineTotal()))
				.toList());

		view.setPayment(new DteViewDTO.Payment(sale.getPaymentMethod(), sale.getPaymentMethod().getLabel(),
				sale.getPaymentMethod().getCode(), sale.getPaymentMethod().getOperationCondition()));

		view.setSubtotal(sale.getSubtotal());
		view.setDiscount(sale.getDiscount());
		view.setTax(sale.getTax());
		view.setTotal(sale.getTotal());
		view.setTotalInWords(NumberToWords.amount(sale.getTotal()));
		return view;
	}
}
