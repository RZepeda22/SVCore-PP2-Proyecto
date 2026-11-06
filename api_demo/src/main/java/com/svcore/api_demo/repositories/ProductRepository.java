package com.svcore.api_demo.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.svcore.api_demo.models.entities.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	/**
	 * Catálogo. category (código) y search (nombre o código) son opcionales.
	 * includeInactive = false para el POS (solo productos activos).
	 */
	@Query("""
			select p from Product p
			join fetch p.category c
			where (:includeInactive = true or p.active = true)
			  and (:category is null or c.code = :category)
			  and (:search is null
			       or lower(p.name) like lower(concat('%', :search, '%'))
			       or lower(p.code) like lower(concat('%', :search, '%')))
			order by c.name, p.name
			""")
	List<Product> findForSale(@Param("category") String category, @Param("search") String search,
							  @Param("includeInactive") boolean includeInactive);

	/**
	 * SELECT ... FOR UPDATE: bloquea el producto hasta que termine la transacción,
	 * para que dos cajas no vendan la misma unidad al mismo tiempo (HU-12).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") UUID id);

	boolean existsByCode(String code);
}
