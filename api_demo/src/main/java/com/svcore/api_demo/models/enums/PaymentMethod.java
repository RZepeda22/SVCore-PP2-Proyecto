package com.svcore.api_demo.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Homologación de las formas de pago del POS al catálogo fiscal (decisión de diseño 6).
 * TODO: confirmar los códigos contra el catálogo de formas de pago de la normativa DTE v1.1
 * antes de pasar a producción.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    EFECTIVO("01", "Efectivo", 1),
    TARJETA("02", "Tarjeta", 1),
    TRANSFERENCIA("05", "Transferencia", 1),
    CREDITO("99", "Crédito", 2);

    /** Código de la forma de pago en el catálogo fiscal. */
    private final String code;
    private final String label;
    /** Condición de la operación: 1 = contado, 2 = a crédito. */
    private final int operationCondition;
}
