package com.svcore.api_demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.svcore.api_demo.models.entities.Rol;

public interface RolRepository extends JpaRepository<Rol, String> {
}
