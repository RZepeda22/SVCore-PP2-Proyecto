package com.svcore.api_demo.service.implementations;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.InventoryAdjustmentDTO;
import com.svcore.api_demo.models.dtos.InventoryEntryDTO;
import com.svcore.api_demo.models.dtos.MovementViewDTO;
import com.svcore.api_demo.models.dtos.ProductViewDTO;
import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.MovementType;
import com.svcore.api_demo.repositories.InventoryMovementRepository;
import com.svcore.api_demo.repositories.ProductRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.InventoryService;
import com.svcore.api_demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

	private static final int MAX_MOVEMENTS = 500;

	private final ProductRepository productRepository;
	private final InventoryMovementRepository movementRepository;
	private final UserService userService;
	private final AuditService auditService;

	@Override
	@Transactional
	public ProductViewDTO registerEntry(InventoryEntryDTO info) {
		Product product = lockProduct(info.getProductId());
		product.setStock(product.getStock() + info.getQuantity());
		movementRepository.save(new InventoryMovement(product, info.getQuantity(), MovementType.ENTRADA_COMPRA,
				null, userService.getCurrentUser()));
		auditService.record(AuditAction.ENTRADA_INVENTARIO, "product", product.getId(),
				"+" + info.getQuantity() + " " + product.getCode() + " (stock " + product.getStock() + ")");
		return ProductViewDTO.from(product);
	}

	@Override
	@Transactional
	public ProductViewDTO adjust(InventoryAdjustmentDTO info) {
		Product product = lockProduct(info.getProductId());
		int difference = info.getCountedQuantity() - product.getStock();
		if (difference == 0) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"El conteo coincide con el sistema (" + product.getStock() + "): no hay nada que ajustar");
		}
		product.setStock(info.getCountedQuantity());
		movementRepository.save(new InventoryMovement(product, difference, MovementType.AJUSTE, null,
				userService.getCurrentUser()));
		auditService.record(AuditAction.AJUSTE_INVENTARIO, "product", product.getId(), product.getCode() + ": "
				+ (difference > 0 ? "+" : "") + difference + " (conteo " + info.getCountedQuantity() + "). Motivo: "
				+ info.getReason().trim());
		return ProductViewDTO.from(product);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MovementViewDTO> movements(UUID productId, LocalDate from, LocalDate to, int limit) {
		List<InventoryMovement> movements = movementRepository.search(productId,
				from == null ? null : from.atStartOfDay(),
				to == null ? null : to.plusDays(1).atStartOfDay(),
				PageRequest.of(0, Math.min(Math.max(1, limit), MAX_MOVEMENTS)));
		return movements.stream().map(MovementViewDTO::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductViewDTO> alerts() {
		return productRepository.findForSale(null, null, false).stream()
				.filter(p -> p.getStock() < p.getMinStock())
				.sorted(Comparator.comparingDouble(p -> (double) p.getStock() / Math.max(1, p.getMinStock())))
				.map(ProductViewDTO::from)
				.toList();
	}

	private Product lockProduct(UUID productId) {
		return productRepository.findByIdForUpdate(productId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "El producto no existe"));
	}
}
