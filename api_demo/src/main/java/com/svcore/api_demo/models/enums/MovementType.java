package com.svcore.api_demo.models.enums;

public enum MovementType {
    /** Salida por venta. */
    SALIDA_VENTA,
    /** Entrada de mercadería comprada. */
    ENTRADA_COMPRA,
    /** Existencias con las que se registró un producto nuevo. */
    INVENTARIO_INICIAL,
    /** Diferencia encontrada en un conteo físico. */
    AJUSTE,
    /** Devolución de mercadería registrada con una Nota de Crédito. */
    ENTRADA_DEVOLUCION,
    /** Movimiento inverso al invalidar (anular) un DTE. */
    REVERSO_ANULACION
}
