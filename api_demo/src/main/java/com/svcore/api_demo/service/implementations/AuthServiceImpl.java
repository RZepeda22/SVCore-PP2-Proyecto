package com.svcore.api_demo.service.implementations;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.LoginDTO;
import com.svcore.api_demo.models.dtos.TokenDTO;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.repositories.UserRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.AuthService;
import com.svcore.api_demo.utils.JWTTools;

import lombok.RequiredArgsConstructor;

/**
 * Inicio de sesión con usuario/correo y contraseña (RNF-03). Cada intento queda en la bitácora.
 * Sin @Transactional a propósito: el contador de intentos fallidos debe guardarse aunque se lance la excepción.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JWTTools jwtTools;
	private final AuditService auditService;

	@Value("${svcore.security.max-failed-attempts}")
	private int maxFailedAttempts;

	@Value("${svcore.security.lock-minutes}")
	private long lockMinutes;

	@Override
	public TokenDTO login(LoginDTO info) {
		String identifier = info.getIdentifier().trim();
		// Mismo mensaje si el usuario no existe o la contraseña es incorrecta: no revela qué usuarios existen
		User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier).orElse(null);
		if (user == null) {
			auditService.recordAs(identifier, AuditAction.LOGIN_FALLIDO, "user", null, "Usuario inexistente");
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}

		if (!user.isActive()) {
			auditService.recordAs(user.getUsername(), AuditAction.LOGIN_FALLIDO, "user", user.getId(), "Usuario inactivo");
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}

		LocalDateTime now = LocalDateTime.now();
		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
			auditService.recordAs(user.getUsername(), AuditAction.LOGIN_FALLIDO, "user", user.getId(), "Cuenta bloqueada");
			throw new BusinessException(HttpStatus.LOCKED,
					"La cuenta está bloqueada por intentos fallidos. Intenta de nuevo en unos minutos.");
		}

		if (!passwordEncoder.matches(info.getPassword(), user.getPasswordHash())) {
			registerFailedAttempt(user, now);
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}

		user.setFailedAttempts(0);
		user.setLockedUntil(null);
		userRepository.save(user);
		auditService.recordAs(user.getUsername(), AuditAction.LOGIN_EXITOSO, "user", user.getId(), null);
		return jwtTools.createToken(user);
	}

	/** Al llegar al máximo de intentos, bloquea la cuenta temporalmente. */
	private void registerFailedAttempt(User user, LocalDateTime now) {
		int attempts = user.getFailedAttempts() + 1;
		boolean lock = attempts >= maxFailedAttempts;
		if (lock) {
			user.setLockedUntil(now.plusMinutes(lockMinutes));
			attempts = 0;
		}
		user.setFailedAttempts(attempts);
		userRepository.save(user);
		auditService.recordAs(user.getUsername(), AuditAction.LOGIN_FALLIDO, "user", user.getId(), "Contraseña incorrecta");
		if (lock) {
			auditService.recordAs(user.getUsername(), AuditAction.CUENTA_BLOQUEADA, "user", user.getId(),
					"Bloqueada " + lockMinutes + " minutos tras " + maxFailedAttempts + " intentos fallidos");
		}
	}
}
