package com.svcore.api_demo.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.svcore.api_demo.models.entities.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

	List<ProductCategory> findAllByOrderByNameAsc();

	boolean existsByCode(String code);
}
