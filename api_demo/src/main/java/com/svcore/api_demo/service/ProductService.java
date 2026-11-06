package com.svcore.api_demo.service;

import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.dtos.EditProductDTO;
import com.svcore.api_demo.models.dtos.ProductViewDTO;
import com.svcore.api_demo.models.dtos.SaveCategoryDTO;
import com.svcore.api_demo.models.dtos.SaveProductDTO;
import com.svcore.api_demo.models.entities.ProductCategory;

/** CU-05: catálogo de productos y categorías. */
public interface ProductService {

	List<ProductViewDTO> findForSale(String category, String search, boolean includeInactive);

	ProductViewDTO findById(UUID productId);

	ProductViewDTO save(SaveProductDTO info);

	ProductViewDTO edit(UUID productId, EditProductDTO info);

	List<ProductCategory> findCategories();

	ProductCategory saveCategory(SaveCategoryDTO info);
}
