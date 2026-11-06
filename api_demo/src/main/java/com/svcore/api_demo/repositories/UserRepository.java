package com.svcore.api_demo.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.svcore.api_demo.models.entities.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

	Optional<User> findByUsernameIgnoreCase(String username);

	List<User> findAllByOrderByFullNameAsc();

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

	boolean existsByEmailIgnoreCase(String email);

	/** Administradores activos: el sistema nunca debe quedarse sin uno. */
	@Query("select count(u) from User u join u.roles r where r.id = :rolId and u.active = true")
	long countActiveWithRole(@Param("rolId") String rolId);
}
