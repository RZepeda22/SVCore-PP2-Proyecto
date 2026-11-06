package com.svcore.api_demo.models.enums;

public enum ContingencyStatus {
    /** Hacienda sigue sin responder: los DTE nuevos se agregan a este evento. */
    ABIERTO,
    /** Hacienda recibió el evento: ya se pueden transmitir los DTE del lote. */
    ACEPTADO,
    /** Ya no quedan DTE del evento pendientes de transmitir. */
    CERRADO
}
