import { getSession, clearSession } from './session';

// Dirección de la API: en desarrollo se usa el proxy de Vite (mismo origen, sin CORS);
// en producción, el mismo host desde el que se abrió el frontend, puerto 8080.
// Se puede fijar con VITE_API_URL en un archivo .env.local si la API corre en otro lugar.
export const API_URL = import.meta.env.VITE_API_URL
    || (import.meta.env.DEV ? '' : window.location.protocol + '//' + window.location.hostname + ':8080');

export class ApiError extends Error {
    constructor(message, status, data) {
        super(message);
        this.status = status;
        this.data = data;
    }
}

/** Llama a la API y devuelve el campo data de GeneralResponse. Lanza ApiError con un mensaje legible. */
export async function api(path, options = {}) {
    const session = getSession();
    const headers = { Accept: 'application/json' };
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    if (session) headers.Authorization = 'Bearer ' + session.token;

    let response;
    try {
        response = await fetch(API_URL + path, {
            method: options.method || 'GET',
            headers,
            body: options.body !== undefined ? JSON.stringify(options.body) : undefined
        });
    } catch (e) {
        throw new ApiError('No se pudo conectar con la API (' + API_URL + '). Verifica que esté en ejecución.', 0);
    }

    let payload = null;
    try { payload = await response.json(); } catch (e) { /* respuesta sin cuerpo */ }

    if (response.status === 401 && session) {
        clearSession();
        window.location.replace('/login?motivo=expirada');
        throw new ApiError('Tu sesión expiró.', 401);
    }
    if (response.status === 403) {
        throw new ApiError('No tienes permiso para realizar esta acción.', 403);
    }
    if (!response.ok) {
        const message = (payload && payload.message) || 'Error ' + response.status + ' al comunicarse con la API.';
        throw new ApiError(message, response.status, payload && payload.data);
    }
    return payload ? payload.data : null;
}

/** Descarga un archivo de la API (CSV, JSON, JWS) conservando la autenticación JWT. */
export async function downloadFile(path, filename) {
    const session = getSession();
    const response = await fetch(API_URL + path, {
        headers: session ? { Authorization: 'Bearer ' + session.token } : {}
    });
    if (!response.ok) {
        let payload = null;
        try { payload = await response.json(); } catch (e) { /* sin cuerpo */ }
        throw new ApiError((payload && payload.message) || 'Error ' + response.status + ' al descargar el archivo.',
            response.status);
    }
    const blob = await response.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
