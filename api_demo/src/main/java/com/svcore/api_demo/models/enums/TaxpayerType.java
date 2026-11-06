package com.svcore.api_demo.models.enums;

/** Clasificación normativa del cliente (decisión de diseño 5). */
public enum TaxpayerType {
    /** Tiene NRC: puede recibir Comprobante de Crédito Fiscal. */
    CONTRIBUYENTE,
    /** Sin NRC: solo recibe Factura. */
    NO_CONTRIBUYENTE
}
