package com.svcore.api_demo.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipos de DTE en alcance. El código corresponde al catálogo de tipos de documento de Hacienda.
 */
@Getter
@RequiredArgsConstructor
public enum DteType {

    FACTURA("01", "FAC", "Factura"),
    CREDITO_FISCAL("03", "CCF", "Comprobante de Crédito Fiscal"),
    NOTA_CREDITO("05", "NC", "Nota de Crédito"),
    NOTA_DEBITO("06", "ND", "Nota de Débito");

    private final String code;
    private final String shortName;
    private final String label;
}
