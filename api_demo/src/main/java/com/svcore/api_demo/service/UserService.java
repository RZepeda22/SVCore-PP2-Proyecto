package com.svcore.api_demo.service;

import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.dtos.ChangePasswordDTO;
import com.svcore.api_demo.models.dtos.EditUserDTO;
import com.svcore.api_demo.models.dtos.ResetPasswordDTO;
import com.svcore.api_demo.models.dtos.SaveUserDTO;
import com.svcore.api_demo.models.dtos.UserViewDTO;
import com.svcore.api_demo.models.entities.Rol;
import com.svcore.api_demo.models.entities.User;

/** CU-10: usuarios y roles. */
public interface UserService {

	/** Usuario dueño del JWT de la petición actual. */
	User getCurrentUser();

	UserViewDTO me();

	List<UserViewDTO> findAll();

	UserViewDTO create(SaveUserDTO info);

	UserViewDTO edit(UUID userId, EditUserDTO info);

	/** El administrador asigna una contraseña nueva (y desbloquea la cuenta). */
	void resetPassword(UUID userId, ResetPasswordDTO info);

	void unlock(UUID userId);

	/** Cada usuario cambia su propia contraseña confirmando la actual. */
	void changeOwnPassword(ChangePasswordDTO info);

	List<Rol> findRoles();
}
