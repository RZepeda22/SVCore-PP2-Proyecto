package com.svcore.api_demo.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.entities.InvalidationEvent;

public interface InvalidationEventRepository extends JpaRepository<InvalidationEvent, UUID> {

	Optional<InvalidationEvent> findByDte(Dte dte);

	boolean existsByDte(Dte dte);
}
