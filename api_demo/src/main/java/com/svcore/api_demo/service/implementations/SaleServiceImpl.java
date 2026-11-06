package com.svcore.api_demo.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.DteViewDTO;
import com.svcore.api_demo.models.dtos.SaleItemDTO;
import com.svcore.api_demo.models.dtos.SaveSaleDTO;
import com.svcore.api_demo.models.entities.Customer;
import com.svcore.api_demo.models.entities.InventoryMovement;
import com.svcore.api_demo.models.entities.Product;
import com.svcore.api_demo.models.entities.Sale;
import com.svcore.api_demo.models.entities.SaleDetail;
import com.svcore.api_demo.models.entities.User;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.DteType;
import com.svcore.api_demo.models.enums.MovementType;
import com.svcore.api_demo.models.enums.TaxpayerType;
import com.svcore.api_demo.repositories.CustomerRepository;
import com.svcore.api_demo.repositories.InventoryMovementRepository;
import com.svcore.api_demo.repositories.ProductRepository;
import com.svcore.api_demo.repositories.SaleRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.DteService;
import com.svcore.api_demo.service.EmitterConfigService;
import com.svcore.api_demo.service.SaleService;
import com.svcore.api_demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

	private static final BigDecimal IVA_RATE = new BigDecimal("0.13");

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final InventoryMovementRepository movementRepository;
	private final CustomerRepository customerRepository;
	private final UserService userService;
	private final DteService dteService;
	private final EmitterConfigService emitterConfigService;
	private final AuditService auditService;

	/**
	 * Flujo de "un clic" (decisión de diseño 2): venta, salida de inventario y DTE en una sola transacción.
	 * Si Hacienda rechaza o no responde, la venta igual queda registrada (decisión de diseño 1).
	 */
	@Override
	@Transactional
	public DteViewDTO registerSale(SaveSaleDTO info) {
		User seller = userService.getCurrentUser();
		Customer customer = findCustomer(info.getCustomerId());
		validateDocumentType(info.getDocType(), customer);

		Sale sale = new Sale();
		sale.setSeller(seller);
		sale.setCustomer(customer);
		sale.setCashRegister(emitterConfigService.get().getCashRegister());
		sale.setDocType(info.getDocType());
		sale.setPaymentMethod(info.getPaymentMethod());

		List<InventoryMovement> movements = new ArrayList<>();
		for (Map.Entry<UUID, Integer> item : groupByProduct(info.getItems()).entrySet()) {
			Product product = findSellableProduct(item.getKey());
			int quantity = item.getValue();

			takeStock(product, quantity);
			movements.add(new InventoryMovement(product, -quantity, MovementType.SALIDA_VENTA, sale, seller));

			SaleDetail detail = new SaleDetail();
			detail.setProduct(product);
			detail.setQuantity(quantity);
			detail.setUnitPrice(product.getPrice());
			detail.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
			sale.addDetail(detail);
		}

		// IVA 13 % sobre el subtotal (en la Factura, el JSON del DTE lo expresa con precios IVA incluido)
		BigDecimal subtotal = sale.getDetails().stream()
				.map(SaleDetail::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal discount = BigDecimal.ZERO.setScale(2);
		BigDecimal taxable = subtotal.subtract(discount);
		BigDecimal tax = taxable.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);

		sale.setSubtotal(subtotal);
		sale.setDiscount(discount);
		sale.setTax(tax);
		sale.setTotal(taxable.add(tax));
		// Suficiente para la demo; con varias cajas en producción conviene una secuencia en la base
		sale.setSaleNumber(saleRepository.findMaxSaleNumber() + 1);

		saleRepository.save(sale);
		movementRepository.saveAll(movements);
		auditService.record(AuditAction.VENTA_REGISTRADA, "sale", sale.getId(),
				"Venta #" + sale.getSaleNumber() + " " + sale.getDocType() + " por " + sale.getTotal());

		return dteService.issue(sale);
	}

	private Customer findCustomer(UUID customerId) {
		if (customerId == null) {
			return null; // Consumidor Final
		}
		return customerRepository.findById(customerId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
	}

	/** Clasificación normativa del cliente (decisión de diseño 5). */
	private void validateDocumentType(DteType docType, Customer customer) {
		if (docType != DteType.FACTURA && docType != DteType.CREDITO_FISCAL) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"Desde el punto de venta solo se emite Factura o Comprobante de Crédito Fiscal");
		}
		boolean contribuyente = customer != null
				&& customer.getTaxpayerType() == TaxpayerType.CONTRIBUYENTE
				&& customer.getNrc() != null;
		if (docType == DteType.CREDITO_FISCAL && !contribuyente) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"El Comprobante de Crédito Fiscal requiere un cliente contribuyente con NRC");
		}
		if (docType == DteType.CREDITO_FISCAL && customer.getNit() == null) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"El Comprobante de Crédito Fiscal requiere el NIT del cliente: complétalo antes de vender");
		}
	}

	/** Si el mismo producto viene en dos líneas, se suman las cantidades. */
	private Map<UUID, Integer> groupByProduct(List<SaleItemDTO> items) {
		Map<UUID, Integer> grouped = new LinkedHashMap<>();
		items.forEach(item -> grouped.merge(item.getProductId(), item.getQuantity(), Integer::sum));
		return grouped;
	}

	/** Lee el producto con bloqueo de fila hasta el fin de la transacción. */
	private Product findSellableProduct(UUID productId) {
		Product product = productRepository.findByIdForUpdate(productId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "El producto no existe"));
		if (!product.isActive()) {
			throw new BusinessException(HttpStatus.CONFLICT, product.getName() + " no está disponible para la venta");
		}
		return product;
	}

	/** Valida la disponibilidad (HU-12) y descuenta el stock. */
	private void takeStock(Product product, int quantity) {
		if (product.getStock() < quantity) {
			throw new BusinessException(HttpStatus.CONFLICT, String.format(
					"Stock insuficiente para %s: disponible %d, solicitado %d",
					product.getName(), product.getStock(), quantity));
		}
		product.setStock(product.getStock() - quantity);
	}
}
