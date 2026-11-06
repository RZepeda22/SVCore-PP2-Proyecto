package com.svcore.api_demo.service.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.models.dtos.EmitterConfigDTO;
import com.svcore.api_demo.models.entities.EmitterConfig;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.repositories.EmitterConfigRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.EmitterConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmitterConfigServiceImpl implements EmitterConfigService {

	private final EmitterConfigRepository emitterConfigRepository;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public EmitterConfig get() {
		return emitterConfigRepository.findById(EmitterConfig.SINGLE_ID)
				.orElseThrow(() -> new IllegalStateException("Falta la configuración fiscal (migración V3)"));
	}

	@Override
	@Transactional
	public EmitterConfigDTO update(EmitterConfigDTO info) {
		EmitterConfig config = get();
		String previousEnvironment = config.getEnvironment();

		config.setBusinessName(info.getBusinessName().trim());
		config.setTradeName(info.getTradeName().trim());
		config.setNit(info.getNit());
		config.setNrc(info.getNrc());
		config.setEconomicActivityCode(info.getEconomicActivityCode());
		config.setEconomicActivity(info.getEconomicActivity().trim());
		config.setEstablishmentType(info.getEstablishmentType());
		config.setEstablishmentName(info.getEstablishmentName().trim());
		config.setDepartmentCode(info.getDepartmentCode());
		config.setMunicipalityCode(info.getMunicipalityCode());
		config.setAddressDetail(info.getAddressDetail().trim());
		config.setPhone(info.getPhone().trim());
		config.setEmail(info.getEmail().trim());
		config.setEstablishmentCode(info.getEstablishmentCode());
		config.setPointOfSaleCode(info.getPointOfSaleCode());
		config.setCashRegister(info.getCashRegister().trim());
		config.setEnvironment(info.getEnvironment());
		emitterConfigRepository.saveAndFlush(config);

		auditService.record(AuditAction.CONFIGURACION_FISCAL, "emitter_config", config.getId(),
				"Configuración fiscal actualizada" + (previousEnvironment.equals(info.getEnvironment())
						? "" : "; ambiente " + previousEnvironment + " -> " + info.getEnvironment()));
		return EmitterConfigDTO.from(config);
	}
}
