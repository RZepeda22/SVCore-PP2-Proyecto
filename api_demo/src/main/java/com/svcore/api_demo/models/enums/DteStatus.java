package com.svcore.api_demo.models.enums;

public enum DteStatus {
    /** Firmado, transmitido y con sello de recepción de Hacienda. */
    SELLADO,
    /** Hacienda no respondió: el documento queda en la cola de reintentos. */
    CONTINGENCIA,
    /** Hacienda devolvió un error de validación: hay que corregir datos y reintentar. */
    RECHAZADO,
    /** Invalidado mediante evento de invalidación. */
    ANULADO
}
