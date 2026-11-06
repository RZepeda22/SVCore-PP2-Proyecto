package com.svcore.api_demo.service.implementations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.ContingencyStatus;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.repositories.ContingencyEventRepository;
import com.svcore.api_demo.repositories.DteRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.ContingencyService;
import com.svcore.api_demo.service.EmitterConfigService;
import com.svcore.api_demo.service.HaciendaService;
import com.svcore.api_demo.service.SignatureService;
import com.svcore.api_demo.utils.DteJsonBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContingencyServiceImpl implements ContingencyService {

	private static final String DEFAULT_REASON = "El servicio de recepción del Ministerio de Hacienda no respondió.";

	private final ContingencyEventRepository contingencyEventRepository;
	private final DteRepository dteRepository;
	private final DteJsonBuilder dteJsonBuilder;
	private final SignatureService signatureService;
	private final HaciendaService haciendaService;
	private final EmitterConfigService emitterConfigService;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public Optional<ContingencyEvent> findOpenEvent() {
		return contingencyEventRepository.findFirstByStatusOrderByStartedAtAsc(ContingencyStatus.ABIERTO);
	}

	@Override
	@Transactional
	public ContingencyEvent currentOpenEvent() {
		return contingencyEventRepository.findFirstByStatusOrderByStartedAtAsc(ContingencyStatus.ABIERTO)
				.orElseGet(() -> {
					ContingencyEvent event = new ContingencyEvent();
					event.setGenerationCode(UUID.randomUUID().toString().toUpperCase());
					event.setContingencyType(ContingencyEvent.TYPE_MH_UNAVAILABLE);
					event.setReason(DEFAULT_REASON);
					event.setStartedAt(LocalDateTime.now());
					event.setStatus(ContingencyStatus.ABIERTO);
					ContingencyEvent saved = contingencyEventRepository.save(event);
					auditService.record(AuditAction.EVENTO_CONTINGENCIA, "contingency_event", saved.getId(),
							"Inicio de contingencia: Hacienda no disponible");
					return saved;
				});
	}

	@Override
	@Transactional
	public boolean ensureAccepted(ContingencyEvent event) {
		if (event.getStatus() != ContingencyStatus.ABIERTO) {
			return true;
		}
		List<Dte> dtes = dteRepository.findAllByContingencyEventOrderByIssuedAtAsc(event);
		event.setEndedAt(LocalDateTime.now());
		String json = dteJsonBuilder.buildContingencyEvent(event, dtes, emitterConfigService.get());
		event.setJsonDocument(json);
		event.setSignedDocument(signatureService.sign(json));

		HaciendaResponseDTO response = haciendaService.transmitContingencyEvent(event);
		if (response.getStatus() != DteStatus.SELLADO) {
			// Hacienda sigue sin responder: el evento continúa abierto y acumula los DTE nuevos
			event.setEndedAt(null);
			contingencyEventRepository.save(event);
			return false;
		}
		event.setStatus(ContingencyStatus.ACEPTADO);
		event.setReceptionStamp(response.getReceptionStamp());
		contingencyEventRepository.save(event);
		auditService.record(AuditAction.EVENTO_CONTINGENCIA, "contingency_event", event.getId(),
				"Evento de contingencia recibido por Hacienda con " + dtes.size() + " DTE");
		return true;
	}

	@Override
	@Transactional
	public void closeIfComplete(ContingencyEvent event) {
		if (event.getStatus() == ContingencyStatus.ACEPTADO
				&& !dteRepository.existsByContingencyEventAndStatus(event, DteStatus.CONTINGENCIA)) {
			event.setStatus(ContingencyStatus.CERRADO);
			contingencyEventRepository.save(event);
		}
	}
}
