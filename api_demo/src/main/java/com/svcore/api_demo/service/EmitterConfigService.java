package com.svcore.api_demo.service;

import com.svcore.api_demo.models.dtos.EmitterConfigDTO;
import com.svcore.api_demo.models.entities.EmitterConfig;

/** CU-09: configuración de facturación (datos fiscales del emisor). */
public interface EmitterConfigService {

	EmitterConfig get();

	EmitterConfigDTO update(EmitterConfigDTO info);
}
