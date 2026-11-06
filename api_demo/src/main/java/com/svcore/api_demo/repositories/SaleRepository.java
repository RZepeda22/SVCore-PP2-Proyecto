package com.svcore.api_demo.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.svcore.api_demo.models.entities.Sale;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

	@Query("select coalesce(max(s.saleNumber), 0) from Sale s")
	Integer findMaxSaleNumber();
}
