package com.svcore.api_demo.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.svcore.api_demo.models.entities.AuditLog;
import com.svcore.api_demo.models.enums.AuditAction;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	Optional<AuditLog> findTopByOrderByIdDesc();

	/** Lotes ordenados para recalcular la cadena de hashes. */
	List<AuditLog> findTop500ByIdGreaterThanOrderByIdAsc(Long id);

	@Query("""
			select a from AuditLog a
			where (:username is null or a.username = :username)
			  and (:action is null or a.action = :action)
			  and (:from is null or a.occurredAt >= :from)
			  and (:to is null or a.occurredAt < :to)
			order by a.id desc
			""")
	Page<AuditLog> search(@Param("username") String username, @Param("action") AuditAction action,
						  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
