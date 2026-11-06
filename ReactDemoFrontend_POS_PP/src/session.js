const STORAGE_KEY = 'svcore.session';

export const ROLE_LABELS = { ADMN: 'Administrador', VEND: 'Vendedor', INVE: 'Encargado de Inventario' };
export const SALES_ROLES = ['VEND', 'ADMN'];

function readStoredSession() {
    for (const store of [sessionStorage, localStorage]) {
        try {
            const raw = store.getItem(STORAGE_KEY);
            if (raw) return JSON.parse(raw);
        } catch (e) {
            // Almacenamiento bloqueado o dato corrupto: se ignora
        }
    }
    return null;
}

export function clearSession() {
    for (const store of [sessionStorage, localStorage]) {
        try { store.removeItem(STORAGE_KEY); } catch (e) { /* sin acceso */ }
    }
}

/** remember=true conserva la sesión entre pestañas (localStorage); si no, solo en esta pestaña. */
export function saveSession(tokenDto, remember) {
    const session = {
        token: tokenDto.token,
        fullName: tokenDto.fullName,
        roles: tokenDto.roles || [],
        expiresAt: Date.parse(tokenDto.expiresAt),
        loginAt: Date.now()
    };
    clearSession();
    try {
        (remember ? localStorage : sessionStorage).setItem(STORAGE_KEY, JSON.stringify(session));
    } catch (e) {
        // Sin almacenamiento la sesión dura solo mientras la página esté abierta
    }
    return session;
}

/** Sesión vigente o null si no hay o ya expiró el token. */
/** true si hay una sesión guardada (aunque ya haya expirado). */
export function storedSessionExists() {
    return !!readStoredSession();
}

export function getSession() {
    const session = readStoredSession();
    if (!session || !session.token || !(Date.now() < session.expiresAt)) return null;
    return session;
}

export function hasRole(session, roles) {
    return !roles || roles.some(role => session.roles.includes(role));
}

export function homeFor(session) {
    return hasRole(session, SALES_ROLES) ? '/dashboard' : '/inventario';
}

export function loginUrl(reason) {
    return '/login' + (reason ? '?motivo=' + encodeURIComponent(reason) : '');
}

export function logout(reason) {
    clearSession();
    window.location.replace(loginUrl(reason));
}

export function minutesLeft(session) {
    return Math.max(0, Math.ceil((session.expiresAt - Date.now()) / 60000));
}
