package com.svcore.api_demo.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Sale;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

	List<InventoryMovement> findAllBySale(Sale sale);

	/** Kardex: movimientos más recientes primero; producto y fechas opcionales. */
	@Query("""
			select m from InventoryMovement m
			join fetch m.product
			join fetch m.user
			left join fetch m.sale
			where (:productId is null or m.product.id = :productId)
			  and (:from is null or m.createdAt >= :from)
			  and (:to is null or m.createdAt < :to)
			order by m.createdAt desc
			""")
	List<InventoryMovement> search(@Param("productId") UUID productId, @Param("from") LocalDateTime from,
								   @Param("to") LocalDateTime to, Pageable pageable);
}
