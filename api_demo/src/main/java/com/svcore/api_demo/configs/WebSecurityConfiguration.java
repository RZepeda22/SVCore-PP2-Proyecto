package com.svcore.api_demo.configs;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static com.svcore.api_demo.utils.RoleCodes.ADMINISTRADOR;
import static com.svcore.api_demo.utils.RoleCodes.INVENTARIO;
import static com.svcore.api_demo.utils.RoleCodes.VENDEDOR;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

	@Value("${svcore.cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/login").permitAll() // Iniciar sesión
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/actuator/health").permitAll()

						// Sprint 1: usuarios, roles y bitácora
						.requestMatchers("/api/user/me", "/api/user/change-password").authenticated() // Cualquier usuario
						.requestMatchers("/api/user/**", "/api/rol/**").hasAuthority(ADMINISTRADOR) // CU-10
						.requestMatchers("/api/audit/**").hasAuthority(ADMINISTRADOR) // RNF-06

						// Sprints 2-3: catálogo, clientes e inventario
						.requestMatchers("/api/product/save", "/api/product/edit", "/api/category/save")
						.hasAnyAuthority(INVENTARIO, ADMINISTRADOR) // Mantener el catálogo
						.requestMatchers("/api/customer/save", "/api/customer/edit")
						.hasAnyAuthority(VENDEDOR, ADMINISTRADOR) // Registrar y corregir clientes
						.requestMatchers("/api/inventory/**").hasAnyAuthority(INVENTARIO, ADMINISTRADOR) // Entradas, ajustes, kardex

						// Sprints 4-7: ventas, DTE, invalidación, notas de crédito y reportes
						.requestMatchers("/api/sale/**").hasAnyAuthority(VENDEDOR, ADMINISTRADOR) // Registrar ventas
						.requestMatchers("/api/dte/invalidate").hasAuthority(ADMINISTRADOR) // CU-04 Anular DTE
						.requestMatchers("/api/dte/**").hasAnyAuthority(VENDEDOR, ADMINISTRADOR) // Consultar, reintentar, NC
						.requestMatchers("/api/report/**").hasAuthority(ADMINISTRADOR) // CU-06
						.requestMatchers(HttpMethod.PUT, "/api/config/**").hasAuthority(ADMINISTRADOR) // CU-09
						.requestMatchers("/api/config/hacienda-simulator").hasAuthority(ADMINISTRADOR)

						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(allowedOrigins);
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
		cors.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}
}
