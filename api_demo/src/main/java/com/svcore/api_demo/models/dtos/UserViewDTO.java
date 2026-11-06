package com.svcore.api_demo.models.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.entities.Rol;
import com.svcore.api_demo.models.entities.User;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Usuario sin datos sensibles (nunca incluye el hash de la contraseña). */
@Data
@AllArgsConstructor
public class UserViewDTO {

	private UUID id;
	private String username;
	private String email;
	private String fullName;
	private boolean active;
	private boolean locked;
	private int failedAttempts;
	private List<String> roles;
	private LocalDateTime createdAt;

	public static UserViewDTO from(User user) {
		boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
		return new UserViewDTO(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.isActive(),
				locked, user.getFailedAttempts(), user.getRoles().stream().map(Rol::getId).sorted().toList(),
				user.getCreatedAt());
	}
}
