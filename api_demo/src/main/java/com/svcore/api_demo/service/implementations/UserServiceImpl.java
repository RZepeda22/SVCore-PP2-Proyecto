package com.svcore.api_demo.service.implementations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.ChangePasswordDTO;
import com.svcore.api_demo.models.dtos.EditUserDTO;
import com.svcore.api_demo.models.dtos.ResetPasswordDTO;
import com.svcore.api_demo.models.dtos.SaveUserDTO;
import com.svcore.api_demo.models.dtos.UserViewDTO;
import com.svcore.api_demo.models.entities.Rol;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.repositories.RolRepository;
import com.svcore.api_demo.repositories.UserRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.UserService;
import com.svcore.api_demo.utils.PasswordPolicy;
import com.svcore.api_demo.utils.RoleCodes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	@Override
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Sesión no válida");
		}
		return userRepository.findByUsernameIgnoreCase(jwt.getSubject())
				.filter(User::isActive)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
	}

	@Override
	@Transactional(readOnly = true)
	public UserViewDTO me() {
		return UserViewDTO.from(getCurrentUser());
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserViewDTO> findAll() {
		return userRepository.findAllByOrderByFullNameAsc().stream().map(UserViewDTO::from).toList();
	}

	@Override
	@Transactional
	public UserViewDTO create(SaveUserDTO info) {
		String username = info.getUsername().trim().toLowerCase();
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ya existe el usuario " + username);
		}
		if (userRepository.existsByEmailIgnoreCase(info.getEmail().trim())) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
		}
		PasswordPolicy.validate(info.getPassword(), username);

		User user = new User();
		user.setUsername(username);
		user.setEmail(info.getEmail().trim().toLowerCase());
		user.setFullName(info.getFullName().trim());
		user.setPasswordHash(passwordEncoder.encode(info.getPassword()));
		user.setRoles(resolveRoles(info.getRoles()));
		userRepository.save(user);

		auditService.record(AuditAction.USUARIO_CREADO, "user", user.getId(),
				username + " con roles " + rolesText(user.getRoles()));
		return UserViewDTO.from(user);
	}

	@Override
	@Transactional
	public UserViewDTO edit(UUID userId, EditUserDTO info) {
		User user = getUser(userId);
		User current = getCurrentUser();
		Set<Rol> roles = resolveRoles(info.getRoles());
		boolean wasActiveAdmin = user.isActive() && hasAdmin(user.getRoles());
		boolean willBeActiveAdmin = info.getActive() && hasAdmin(roles);

		if (user.getId().equals(current.getId()) && !willBeActiveAdmin) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"No puedes quitarte el rol de administrador ni desactivar tu propia cuenta");
		}
		if (wasActiveAdmin && !willBeActiveAdmin && userRepository.countActiveWithRole(RoleCodes.ADMINISTRADOR) <= 1) {
			throw new BusinessException(HttpStatus.CONFLICT, "El sistema debe conservar al menos un administrador activo");
		}
		if (userRepository.existsByEmailIgnoreCaseAndIdNot(info.getEmail().trim(), userId)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
		}

		user.setEmail(info.getEmail().trim().toLowerCase());
		user.setFullName(info.getFullName().trim());
		user.setActive(info.getActive());
		user.setRoles(roles);
		userRepository.save(user);

		auditService.record(AuditAction.USUARIO_EDITADO, "user", user.getId(), user.getUsername() + ": roles "
				+ rolesText(roles) + (user.isActive() ? ", activo" : ", desactivado"));
		return UserViewDTO.from(user);
	}

	@Override
	@Transactional
	public void resetPassword(UUID userId, ResetPasswordDTO info) {
		User user = getUser(userId);
		PasswordPolicy.validate(info.getNewPassword(), user.getUsername());
		user.setPasswordHash(passwordEncoder.encode(info.getNewPassword()));
		user.setFailedAttempts(0);
		user.setLockedUntil(null);
		userRepository.save(user);
		auditService.record(AuditAction.CONTRASENA_RESTABLECIDA, "user", user.getId(), user.getUsername());
	}

	@Override
	@Transactional
	public void unlock(UUID userId) {
		User user = getUser(userId);
		user.setFailedAttempts(0);
		user.setLockedUntil(null);
		userRepository.save(user);
		auditService.record(AuditAction.USUARIO_DESBLOQUEADO, "user", user.getId(), user.getUsername());
	}

	@Override
	@Transactional
	public void changeOwnPassword(ChangePasswordDTO info) {
		User user = getCurrentUser();
		if (!passwordEncoder.matches(info.getCurrentPassword(), user.getPasswordHash())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
		}
		if (info.getCurrentPassword().equals(info.getNewPassword())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
		}
		PasswordPolicy.validate(info.getNewPassword(), user.getUsername());
		user.setPasswordHash(passwordEncoder.encode(info.getNewPassword()));
		userRepository.save(user);
		auditService.record(AuditAction.CONTRASENA_CAMBIADA, "user", user.getId(), user.getUsername());
	}

	@Override
	@Transactional(readOnly = true)
	public List<Rol> findRoles() {
		return rolRepository.findAll();
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
	}

	private Set<Rol> resolveRoles(List<String> codes) {
		Set<Rol> roles = new HashSet<>();
		for (String code : codes) {
			roles.add(rolRepository.findById(code)
					.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "El rol " + code + " no existe")));
		}
		return roles;
	}

	private static boolean hasAdmin(Set<Rol> roles) {
		return roles.stream().anyMatch(r -> RoleCodes.ADMINISTRADOR.equals(r.getId()));
	}

	private static String rolesText(Set<Rol> roles) {
		return String.join(",", roles.stream().map(Rol::getId).sorted().toList());
	}
}
