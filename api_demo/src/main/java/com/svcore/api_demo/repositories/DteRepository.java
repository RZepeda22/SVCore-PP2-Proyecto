package com.svcore.api_demo.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.svcore.api_demo.models.entities.ContingencyEvent;
import com.svcore.api_demo.models.entities.Dte;
import com.svcore.api_demo.models.enums.DteStatus;
import com.svcore.api_demo.models.enums.DteType;

public interface DteRepository extends JpaRepository<Dte, UUID> {

	@EntityGraph(attributePaths = "sale")
	List<Dte> findAllByOrderByIssuedAtDesc();

	@EntityGraph(attributePaths = "sale")
	List<Dte> findAllByStatusOrderByIssuedAtDesc(DteStatus status);

	List<Dte> findAllByStatusOrderByIssuedAtAsc(DteStatus status);

	List<Dte> findAllByContingencyEventOrderByIssuedAtAsc(ContingencyEvent event);

	boolean existsByContingencyEventAndStatus(ContingencyEvent event, DteStatus status);

	/** Notas de crédito vigentes de un Crédito Fiscal. */
	List<Dte> findAllByRelatedDteAndStatusNot(Dte relatedDte, DteStatus status);

	boolean existsByRelatedDteAndStatusNot(Dte relatedDte, DteStatus status);

	long countByType(DteType type);

	/** Documentos del periodo con su venta, detalle y cliente (para reportes y libro de IVA). */
	@Query("""
			select distinct d from Dte d
			join fetch d.sale s
			left join fetch s.customer
			left join fetch s.details det
			left join fetch det.product
			where d.issuedAt >= :from and d.issuedAt < :to
			order by d.issuedAt
			""")
	List<Dte> findForReport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
