package com.svcore.api_demo.service.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.EditProductDTO;
import com.svcore.api_demo.models.dtos.ProductViewDTO;
import com.svcore.api_demo.models.dtos.SaveCategoryDTO;
import com.svcore.api_demo.models.dtos.SaveProductDTO;
import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.entities.ProductCategory;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.MovementType;
import com.svcore.api_demo.repositories.InventoryMovementRepository;
import com.svcore.api_demo.repositories.ProductCategoryRepository;
import com.svcore.api_demo.repositories.ProductRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.ProductService;
import com.svcore.api_demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final ProductCategoryRepository categoryRepository;
	private final InventoryMovementRepository movementRepository;
	private final UserService userService;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public List<ProductViewDTO> findForSale(String category, String search, boolean includeInactive) {
		// El POS envía "all" para la pestaña "Todos"
		String categoryFilter = isBlank(category) || "all".equalsIgnoreCase(category)
				? null : category.trim().toLowerCase();
		String searchFilter = isBlank(search) ? null : search.trim();
		return productRepository.findForSale(categoryFilter, searchFilter, includeInactive).stream()
				.map(ProductViewDTO::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductViewDTO findById(UUID productId) {
		return ProductViewDTO.from(getProduct(productId));
	}

	@Override
	@Transactional
	public ProductViewDTO save(SaveProductDTO info) {
		String code = info.getCode().trim().toUpperCase();
		if (productRepository.existsByCode(code)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un producto con el código " + code);
		}
		int initialStock = info.getInitialStock() == null ? 0 : info.getInitialStock();

		Product product = new Product();
		product.setCode(code);
		product.setName(info.getName().trim());
		product.setPrice(info.getPrice());
		product.setMinStock(info.getMinStock());
		product.setCategory(getCategory(info.getCategoryId()));
		product.setStock(initialStock);
		productRepository.save(product);

		if (initialStock > 0) {
			movementRepository.save(new InventoryMovement(product, initialStock, MovementType.INVENTARIO_INICIAL,
					null, userService.getCurrentUser()));
		}
		auditService.record(AuditAction.PRODUCTO_CREADO, "product", product.getId(),
				code + " " + product.getName() + " a " + product.getPrice() + ", stock inicial " + initialStock);
		return ProductViewDTO.from(product);
	}

	@Override
	@Transactional
	public ProductViewDTO edit(UUID productId, EditProductDTO info) {
		Product product = getProduct(productId);
		String before = product.getPrice() + (product.isActive() ? " activo" : " inactivo");

		product.setName(info.getName().trim());
		product.setPrice(info.getPrice());
		product.setMinStock(info.getMinStock());
		product.setCategory(getCategory(info.getCategoryId()));
		product.setActive(info.getActive());
		productRepository.save(product);

		auditService.record(AuditAction.PRODUCTO_EDITADO, "product", product.getId(), product.getCode() + ": " + before
				+ " -> " + product.getPrice() + (product.isActive() ? " activo" : " inactivo"));
		return ProductViewDTO.from(product);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductCategory> findCategories() {
		return categoryRepository.findAllByOrderByNameAsc();
	}

	@Override
	@Transactional
	public ProductCategory saveCategory(SaveCategoryDTO info) {
		String code = info.getCode().trim().toLowerCase();
		if (categoryRepository.existsByCode(code)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ya existe una categoría con el código " + code);
		}
		ProductCategory category = new ProductCategory();
		category.setCode(code);
		category.setName(info.getName().trim());
		categoryRepository.save(category);
		auditService.record(AuditAction.CATEGORIA_CREADA, "product_category", category.getId(), code);
		return category;
	}

	private Product getProduct(UUID productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
	}

	private ProductCategory getCategory(UUID categoryId) {
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
