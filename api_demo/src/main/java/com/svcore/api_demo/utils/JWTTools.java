package com.svcore.api_demo.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.svcore.api_demo.models.dtos.TokenDTO;
import com.svcore.api_demo.models.entities.Rol;
import com.svcore.api_demo.models.entities.User;

import lombok.RequiredArgsConstructor;

/**
 * Emite los JWT. La validación de los tokens entrantes la hace el resource server de Spring Security
 * (configurado en JwtConfig), por eso ya no hace falta un filtro propio como JWTTokenFilter.
 */
@Component
@RequiredArgsConstructor
public class JWTTools {

	public static final String ROLES_CLAIM = "roles";
	private static final String ISSUER = "svcore-api";

	private final JwtEncoder jwtEncoder;

	@Value("${svcore.jwt.expiration-minutes}")
	private long expirationMinutes;

	public TokenDTO createToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
		List<String> roles = user.getRoles().stream().map(Rol::getId).sorted().toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(user.getUsername())
				.claim("name", user.getFullName())
				.claim(ROLES_CLAIM, roles)
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new TokenDTO(token, "Bearer", expiresAt, user.getFullName(), roles);
	}
}
