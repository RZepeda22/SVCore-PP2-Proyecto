package com.svcore.api_demo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.svcore.api_demo.exceptions.BusinessException;

/**
 * Política de contraseñas (RNF-03, Microsoft SDL): mínimo 10 caracteres, mayúscula, minúscula, número y símbolo,
 * sin contener el nombre de usuario. Máximo 72 porque BCrypt ignora lo que sigue.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 10;
	public static final int MAX_LENGTH = 72;

	private PasswordPolicy() {
	}

	public static List<String> violations(String password, String username) {
		List<String> problems = new ArrayList<>();
		if (password == null || password.length() < MIN_LENGTH) problems.add("al menos " + MIN_LENGTH + " caracteres");
		if (password != null && password.length() > MAX_LENGTH) problems.add("máximo " + MAX_LENGTH + " caracteres");
		if (password == null) return problems;
		if (!password.chars().anyMatch(Character::isUpperCase)) problems.add("una mayúscula");
		if (!password.chars().anyMatch(Character::isLowerCase)) problems.add("una minúscula");
		if (!password.chars().anyMatch(Character::isDigit)) problems.add("un número");
		if (password.chars().allMatch(Character::isLetterOrDigit)) problems.add("un símbolo");
		if (username != null && username.length() >= 3
				&& password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
			problems.add("no contener el nombre de usuario");
		}
		return problems;
	}

	public static void validate(String password, String username) {
		List<String> problems = violations(password, username);
		if (!problems.isEmpty()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"La contraseña debe tener " + String.join(", ", problems) + ".");
		}
	}
}
