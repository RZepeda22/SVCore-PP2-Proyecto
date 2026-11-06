package com.svcore.api_demo.service;

import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.dtos.SaveSaleDTO;

public interface SaleService {

	/** Registra la venta, descuenta inventario y emite el DTE en un solo paso (CU-02). */
	DteViewDTO registerSale(SaveSaleDTO info);
}
