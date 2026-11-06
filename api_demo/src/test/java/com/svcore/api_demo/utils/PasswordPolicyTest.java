package com.svcore.api_demo.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.svcore.api_demo.exceptions.BusinessException;

class PasswordPolicyTest {

	@Test
	void aceptaUnaContrasenaFuerte() {
		assertDoesNotThrow(() -> PasswordPolicy.validate("Caja#Segura2026", "ricardo"));
	}

	@Test
	void detallaCadaRequisitoQueFalta() {
		List<String> problems = PasswordPolicy.violations("corta", "ricardo");
		assertTrue(problems.contains("al menos 10 caracteres"));
		assertTrue(problems.contains("una mayúscula"));
		assertTrue(problems.contains("un número"));
		assertTrue(problems.contains("un símbolo"));
	}

	@Test
	void rechazaContrasenasQueContienenElUsuario() {
		assertThrows(BusinessException.class, () -> PasswordPolicy.validate("Ricardo#2026x", "ricardo"));
	}

	@Test
	void rechazaContrasenasMasLargasQueElLimiteDeBcrypt() {
		assertThrows(BusinessException.class, () -> PasswordPolicy.validate("Aa1#" + "x".repeat(80), "ana"));
	}
}
