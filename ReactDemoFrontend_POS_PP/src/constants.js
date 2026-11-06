// Estados del DTE: etiqueta y color del badge
export const STATUS = {
    SELLADO: { label: 'Sellado', badge: 'success' },
    CONTINGENCIA: { label: 'En contingencia', badge: 'warning' },
    RECHAZADO: { label: 'Rechazado', badge: 'danger' },
    ANULADO: { label: 'Anulado', badge: 'neutral' }
};

export function statusInfo(status) {
    return STATUS[status] || { label: status, badge: 'neutral' };
}

export const DOC_NAMES = {
    FACTURA: 'Factura Electrónica',
    CREDITO_FISCAL: 'CCF Electrónico',
    NOTA_CREDITO: 'Nota de Crédito Electrónica',
    NOTA_DEBITO: 'Nota de Débito Electrónica'
};

export const DOC_TYPE_SHORT = {
    FACTURA: 'FAC',
    CREDITO_FISCAL: 'CCF',
    NOTA_CREDITO: 'NC',
    NOTA_DEBITO: 'ND'
};

export const TAXPAYER = { CONTRIBUYENTE: 'Contribuyente', NO_CONTRIBUYENTE: 'No contribuyente' };

export const AUDIT_ACTIONS = [
    'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'CUENTA_BLOQUEADA',
    'USUARIO_CREADO', 'USUARIO_EDITADO', 'USUARIO_DESBLOQUEADO',
    'CONTRASENA_RESTABLECIDA', 'CONTRASENA_CAMBIADA',
    'CLIENTE_CREADO', 'CLIENTE_EDITADO', 'CATEGORIA_CREADA',
    'PRODUCTO_CREADO', 'PRODUCTO_EDITADO', 'ENTRADA_INVENTARIO', 'AJUSTE_INVENTARIO',
    'VENTA_REGISTRADA', 'DTE_EMITIDO', 'DTE_REINTENTO', 'DTE_INVALIDADO',
    'NOTA_CREDITO_EMITIDA', 'EVENTO_CONTINGENCIA', 'CONFIGURACION_FISCAL', 'SIMULADOR_HACIENDA'
];

export const INVALIDATION_TYPES = [
    { value: 1, label: '1 · Error en la información del documento' },
    { value: 2, label: '2 · Rescindir la operación (se anula la venta)' },
    { value: 3, label: '3 · Otro motivo' }
];
