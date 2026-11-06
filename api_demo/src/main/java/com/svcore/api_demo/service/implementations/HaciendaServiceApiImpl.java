package com.svcore.api_demo.service.implementations;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.svcore.api_demo.models.dtos.HaciendaResponseDTO;
import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.InvalidationEvent;
import com.svcore.api_demo.service.EmitterConfigService;
import com.svcore.api_demo.service.HaciendaService;
import com.svcore.api_demo.utils.DteJsonBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de la API de recepción de DTE del Ministerio de Hacienda (svcore.hacienda.mode=api).
 * <p>
 * NO PROBADO contra Hacienda: requiere el usuario y la contraseña de la API que Hacienda entrega al emisor
 * autorizado. Rutas y campos según el manual técnico de integración; confirmarlos con la versión vigente.
 * Cualquier falla de red, tiempo de espera o error 5xx se trata como CONTINGENCIA.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "svcore.hacienda.mode", havingValue = "api")
public class HaciendaServiceApiImpl implements HaciendaService {

	private static final Duration TOKEN_VALIDITY = Duration.ofHours(23);

	private final RestClient client;
	private final String user;
	private final String password;
	private final EmitterConfigService emitterConfigService;
	private final AtomicInteger sendId = new AtomicInteger(1);

	private String token;
	private Instant tokenExpiresAt = Instant.EPOCH;

	public HaciendaServiceApiImpl(@Value("${svcore.hacienda.api.url}") String baseUrl,
								  @Value("${svcore.hacienda.api.user}") String user,
								  @Value("${svcore.hacienda.api.password}") String password,
								  @Value("${svcore.hacienda.api.timeout-seconds:8}") int timeoutSeconds,
								  EmitterConfigService emitterConfigService) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
		this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
		this.user = user;
		this.password = password;
		this.emitterConfigService = emitterConfigService;
	}

	@Override
	public HaciendaResponseDTO transmit(Dte dte) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ambiente", dte.getEnvironment());
		body.put("idEnvio", sendId.getAndIncrement());
		body.put("version", DteJsonBuilder.schemaVersion(dte.getType()));
		body.put("tipoDte", dte.getType().getCode());
		body.put("documento", dte.getSignedDocument());
		body.put("codigoGeneracion", dte.getGenerationCode());
		return post("/fesv/recepciondte", body, "PROCESADO");
	}

	@Override
	public HaciendaResponseDTO transmitContingencyEvent(ContingencyEvent event) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("nit", emitterConfigService.get().getNit().replaceAll("\\D", ""));
		body.put("documento", event.getSignedDocument());
		return post("/fesv/contingencia", body, "RECIBIDO");
	}

	@Override
	public HaciendaResponseDTO transmitInvalidation(InvalidationEvent event, Dte dte) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ambiente", dte.getEnvironment());
		body.put("idEnvio", sendId.getAndIncrement());
		body.put("version", 2);
		body.put("documento", event.getSignedDocument());
		return post("/fesv/anulardte", body, "PROCESADO");
	}

	@SuppressWarnings("unchecked")
	private HaciendaResponseDTO post(String path, Map<String, Object> body, String acceptedState) {
		try {
			Map<String, Object> response = client.post().uri(path)
					.header("Authorization", token())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(Map.class);
			return interpret(response, acceptedState);
		} catch (HttpClientErrorException e) {
			// 4xx: Hacienda respondió y rechazó el envío
			if (e.getStatusCode().value() == 401) {
				tokenExpiresAt = Instant.EPOCH;
			}
			Map<String, Object> response = e.getResponseBodyAs(Map.class);
			return response != null ? interpret(response, acceptedState)
					: HaciendaResponseDTO.rejected(String.valueOf(e.getStatusCode().value()), e.getStatusText());
		} catch (RestClientException e) {
			log.warn("Hacienda no respondió en {}: {}", path, e.getMessage());
			return HaciendaResponseDTO.unavailable();
		}
	}

	private HaciendaResponseDTO interpret(Map<String, Object> response, String acceptedState) {
		if (response == null) {
			return HaciendaResponseDTO.unavailable();
		}
		String state = String.valueOf(response.get("estado"));
		if (acceptedState.equalsIgnoreCase(state)) {
			return HaciendaResponseDTO.stamped(String.valueOf(response.get("selloRecibido")));
		}
		String message = String.valueOf(response.getOrDefault("descripcionMsg", "Documento rechazado"));
		Object observations = response.get("observaciones");
		if (observations instanceof List<?> list && !list.isEmpty()) {
			message += " Observaciones: " + String.join("; ", list.stream().map(String::valueOf).toList());
		}
		return HaciendaResponseDTO.rejected(String.valueOf(response.getOrDefault("codigoMsg", "")), message);
	}

	/** Token de la API de Hacienda (se renueva antes de las 24 horas de validez). */
	@SuppressWarnings("unchecked")
	private synchronized String token() {
		if (token != null && Instant.now().isBefore(tokenExpiresAt)) {
			return token;
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("user", user);
		form.add("pwd", password);
		Map<String, Object> response = client.post().uri("/seguridad/auth")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(Map.class);
		Object bodyField = response != null ? response.get("body") : null;
		if (!(bodyField instanceof Map<?, ?> tokenBody) || tokenBody.get("token") == null) {
			throw new RestClientException("Hacienda no devolvió un token de autenticación");
		}
		token = String.valueOf(tokenBody.get("token"));
		tokenExpiresAt = Instant.now().plus(TOKEN_VALIDITY);
		return token;
	}
}
