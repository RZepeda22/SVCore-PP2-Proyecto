package com.svcore.api_demo.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.enums.ContingencyStatus;

public interface ContingencyEventRepository extends JpaRepository<ContingencyEvent, UUID> {

	Optional<ContingencyEvent> findFirstByStatusOrderByStartedAtAsc(ContingencyStatus status);
}
