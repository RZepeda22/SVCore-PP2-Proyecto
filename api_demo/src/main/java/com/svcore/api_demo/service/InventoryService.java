package com.svcore.api_demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.dtos.InventoryAdjustmentDTO;
import com.svcore.api_demo.models.dtos.InventoryEntryDTO;
import com.svcore.api_demo.models.dtos.MovementViewDTO;
import com.svcore.api_demo.models.dtos.ProductViewDTO;

/** CU-05: existencias y trazabilidad del inventario (decisión de diseño 4). */
public interface InventoryService {

	/** Suma existencias a un producto y deja el movimiento registrado. */
	ProductViewDTO registerEntry(InventoryEntryDTO info);

	/** Conteo físico: la diferencia con el sistema queda como movimiento AJUSTE. */
	ProductViewDTO adjust(InventoryAdjustmentDTO info);

	/** Kardex: movimientos más recientes primero (máximo 500). */
	List<MovementViewDTO> movements(UUID productId, LocalDate from, LocalDate to, int limit);

	/** Productos por debajo del stock mínimo. */
	List<ProductViewDTO> alerts();
}
