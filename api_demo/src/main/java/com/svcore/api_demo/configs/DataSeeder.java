package com.svcore.api_demo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.repositories.RolRepository;
import com.svcore.api_demo.repositories.UserRepository;
import com.svcore.api_demo.utils.RoleCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea los usuarios de demostración la primera vez que arranca la API.
 * La contraseña viene de DEMO_PASSWORD para no dejar hashes ni contraseñas en el repositorio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

	private final UserRepository userRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${svcore.demo.password:}")
	private String demoPassword;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		if (demoPassword == null || demoPassword.isBlank()) {
			log.warn("No hay usuarios y DEMO_PASSWORD está vacío: no se crearon usuarios de demostración.");
			return;
		}

		createUser("marco", "marco.arana@svcore.demo", "Marco Arana", RoleCodes.ADMINISTRADOR);
		createUser("ricardo", "ricardo.garcia@svcore.demo", "Ricardo García", RoleCodes.VENDEDOR);
		createUser("josemaria", "josemaria.munoz@svcore.demo", "Josemaría Muñoz", RoleCodes.INVENTARIO);
		log.info("Usuarios de demostración creados: marco (ADMN), ricardo (VEND), josemaria (INVE)");
	}

	private void createUser(String username, String email, String fullName, String rolId) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setFullName(fullName);
		user.setPasswordHash(passwordEncoder.encode(demoPassword));
		user.getRoles().add(rolRepository.findById(rolId)
				.orElseThrow(() -> new IllegalStateException("Falta el rol " + rolId + " (migración V2)")));
		userRepository.save(user);
	}
}
