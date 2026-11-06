/*
 * SV-Core · utilidades compartidas por todas las pantallas:
 * sesión (JWT), llamadas a la API, formato de datos, avisos, modales y barra lateral.
 * Se expone como window.SV.
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'svcore.session';
    const API_URL = (window.SVCORE_CONFIG && window.SVCORE_CONFIG.apiUrl) || '';
    const ROLE_LABELS = { ADMN: 'Administrador', VEND: 'Vendedor', INVE: 'Encargado de Inventario' };
    const SALES_ROLES = ['VEND', 'ADMN'];
    const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

    // Estados del DTE: etiqueta, color del badge y pantalla de detalle
    const STATUS = {
        SELLADO: { label: 'Sellado', badge: 'success', page: 'dte-emitido.html' },
        CONTINGENCIA: { label: 'En contingencia', badge: 'warning', page: 'dte-contingencia.html' },
        RECHAZADO: { label: 'Rechazado', badge: 'danger', page: 'dte-rechazado.html' },
        ANULADO: { label: 'Anulado', badge: 'neutral', page: 'dte-emitido.html' }
    };

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

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

    function clearSession() {
        for (const store of [sessionStorage, localStorage]) {
            try { store.removeItem(STORAGE_KEY); } catch (e) { /* sin acceso */ }
        }
    }

    /** remember=true conserva la sesión entre pestañas (localStorage); si no, solo en esta pestaña. */
    function saveSession(tokenDto, remember) {
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
    function getSession() {
        const session = readStoredSession();
        if (!session || !session.token || !(Date.now() < session.expiresAt)) return null;
        return session;
    }

    function hasRole(session, roles) {
        return !roles || roles.some(role => session.roles.includes(role));
    }

    function homeFor(session) {
        return hasRole(session, SALES_ROLES) ? 'dashboard.html' : 'inventario.html';
    }

    function logout(reason) {
        clearSession();
        window.location.replace('index.html' + (reason ? '?motivo=' + encodeURIComponent(reason) : ''));
    }

    /**
     * Protege una pantalla: sin sesión vuelve al login; sin el rol requerido va a su pantalla de inicio.
     * Devuelve la sesión o null (en ese caso la página ya se está redirigiendo).
     */
    function requireAuth(roles) {
        const stored = readStoredSession();
        const session = getSession();
        if (!session) {
            logout(stored ? 'expirada' : '');
            return null;
        }
        if (!hasRole(session, roles)) {
            window.location.replace(homeFor(session));
            return null;
        }
        // RNF-05: al vencer el token (15 min) la sesión se cierra sola
        setTimeout(() => logout('expirada'), Math.max(0, session.expiresAt - Date.now()));
        renderSidebar(session);
        return session;
    }

    function minutesLeft(session) {
        return Math.max(0, Math.ceil((session.expiresAt - Date.now()) / 60000));
    }

    // ------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------

    class ApiError extends Error {
        constructor(message, status, data) {
            super(message);
            this.status = status;
            this.data = data;
        }
    }

    /** Llama a la API y devuelve el campo data de GeneralResponse. Lanza ApiError con un mensaje legible. */
    async function api(path, options = {}) {
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
            logout('expirada');
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

    // ------------------------------------------------------------------
    // Formato
    // ------------------------------------------------------------------

    const moneyFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

    function formatMoney(value) {
        return moneyFormat.format(Number(value || 0));
    }

    /** "$1,848.02" -> ["$1,848", ".02"] para las tarjetas que muestran los decimales en pequeño. */
    function splitMoney(value) {
        const text = formatMoney(value);
        const dot = text.lastIndexOf('.');
        return dot === -1 ? [text, ''] : [text.slice(0, dot), text.slice(dot)];
    }

    /** La API envía LocalDateTime sin zona ("2026-11-06T14:41:28.123456"): se interpreta como hora local. */
    function parseDate(value) {
        if (!value) return null;
        const date = new Date(String(value).replace(/(\.\d{3})\d+/, '$1'));
        return isNaN(date.getTime()) ? null : date;
    }

    const pad = n => String(n).padStart(2, '0');

    function formatTime(value, withSeconds) {
        const d = parseDate(value);
        if (!d) return '—';
        return pad(d.getHours()) + ':' + pad(d.getMinutes()) + (withSeconds ? ':' + pad(d.getSeconds()) : '');
    }

    function formatDate(value) {
        const d = parseDate(value);
        return d ? d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + d.getFullYear() : '—';
    }

    /** "20 jun 2026 · 10:42:18" */
    function formatDateTime(value, withSeconds = true) {
        const d = parseDate(value);
        return d ? formatDate(value) + ' · ' + formatTime(value, withSeconds) : '—';
    }

    function isToday(value) {
        const d = parseDate(value);
        const now = new Date();
        return !!d && d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
            && d.getDate() === now.getDate();
    }

    /** Hora si es de hoy; si no, fecha corta y hora. */
    function formatWhen(value) {
        const d = parseDate(value);
        if (!d) return '—';
        return isToday(value) ? formatTime(value) : d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + formatTime(value);
    }

    /** Evita inyección de HTML al pintar datos que vienen de la base (nombres de clientes, etc.). */
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function initials(name) {
        return String(name || '')
            .replace(/[^\p{L}\s]/gu, ' ')
            .split(/\s+/).filter(Boolean).slice(0, 2)
            .map(word => word[0].toUpperCase()).join('') || '?';
    }

    function normalize(text) {
        return String(text || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    }

    function statusInfo(status) {
        return STATUS[status] || { label: status, badge: 'neutral', page: 'dte-emitido.html' };
    }

    function dteUrl(dte) {
        return statusInfo(dte.status).page + '?id=' + encodeURIComponent(dte.id);
    }

    function statusBadge(status) {
        const info = statusInfo(status);
        return '<span class="status-badge ' + info.badge + '">' + escapeHtml(info.label) + '</span>';
    }

    function typeBadge(shortName) {
        return '<span class="type-badge ' + escapeHtml(String(shortName).toLowerCase()) + '">'
            + escapeHtml(shortName) + '</span>';
    }

    // ------------------------------------------------------------------
    // Avisos (toasts)
    // ------------------------------------------------------------------

    function toast(message, type = 'info') {
        let container = document.querySelector('.sv-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'sv-toast-container';
            container.setAttribute('role', 'status');
            container.setAttribute('aria-live', 'polite');
            document.body.appendChild(container);
        }
        const item = document.createElement('div');
        item.className = 'sv-toast sv-toast-' + type;
        item.textContent = message;
        container.appendChild(item);
        setTimeout(() => item.classList.add('sv-toast-hide'), 4500);
        setTimeout(() => item.remove(), 5000);
    }

    // ------------------------------------------------------------------
    // Modales con formulario
    // ------------------------------------------------------------------

    /**
     * Abre un formulario modal. onSubmit(values, form) puede lanzar ApiError: el mensaje se muestra
     * en el modal y, si la API devolvió errores por campo, se marcan junto a cada campo.
     */
    function openModal({ title, body, submitLabel = 'Guardar', onOpen, onSubmit }) {
        const overlay = document.createElement('div');
        overlay.className = 'sv-modal-overlay';
        overlay.innerHTML =
            '<form class="sv-modal" novalidate role="dialog" aria-modal="true" aria-labelledby="sv-modal-title">' +
            '  <div class="sv-modal-header">' +
            '    <h3 id="sv-modal-title">' + escapeHtml(title) + '</h3>' +
            '    <button type="button" class="sv-modal-close" data-close aria-label="Cerrar">&times;</button>' +
            '  </div>' +
            '  <div class="sv-modal-body">' + body + '<div class="sv-form-error" hidden></div></div>' +
            '  <div class="sv-modal-footer">' +
            '    <button type="button" class="sv-btn sv-btn-secondary" data-close>Cancelar</button>' +
            '    <button type="submit" class="sv-btn sv-btn-primary">' + escapeHtml(submitLabel) + '</button>' +
            '  </div>' +
            '</form>';
        document.body.appendChild(overlay);

        const form = overlay.querySelector('form');
        const errorBox = form.querySelector('.sv-form-error');
        const submit = form.querySelector('[type=submit]');

        const onKey = event => { if (event.key === 'Escape') close(); };
        function close() {
            document.removeEventListener('keydown', onKey);
            overlay.remove();
        }
        document.addEventListener('keydown', onKey);
        overlay.addEventListener('mousedown', event => { if (event.target === overlay) close(); });
        form.querySelectorAll('[data-close]').forEach(btn => btn.addEventListener('click', close));

        if (onOpen) onOpen(form);
        const first = form.querySelector('input:not([disabled]), select, textarea');
        if (first) first.focus();

        form.addEventListener('submit', async event => {
            event.preventDefault();
            errorBox.hidden = true;
            form.querySelectorAll('.sv-field-error').forEach(el => el.remove());
            form.querySelectorAll('.sv-invalid').forEach(el => el.classList.remove('sv-invalid'));

            const values = {};
            new FormData(form).forEach((value, key) => { values[key] = String(value).trim(); });

            submit.disabled = true;
            try {
                await onSubmit(values, form);
                close();
            } catch (error) {
                errorBox.textContent = error.message;
                errorBox.hidden = false;
                if (error.data && typeof error.data === 'object') {
                    Object.entries(error.data).forEach(([field, message]) => {
                        const input = form.querySelector('[name="' + field + '"]');
                        if (!input) return;
                        input.classList.add('sv-invalid');
                        const hint = document.createElement('small');
                        hint.className = 'sv-field-error';
                        hint.textContent = message;
                        input.insertAdjacentElement('afterend', hint);
                    });
                }
            } finally {
                submit.disabled = false;
            }
        });

        return { close, form };
    }

    /** Formulario de cliente: registra uno nuevo o edita uno existente (customer.id). */
    function openCustomerModal({ customer, title, submitLabel, onSaved }) {
        const c = customer || {};
        const field = (id, name, label, value, extra) =>
            '<div class="form-group"><label for="' + id + '">' + label + '</label>' +
            '<input id="' + id + '" name="' + name + '" class="form-input" value="' + escapeHtml(value || '') + '" ' +
            (extra || '') + '></div>';

        const body =
            field('cm-name', 'name', 'Nombre o razón social', c.name, 'maxlength="150" required') +
            '<div class="form-group"><label for="cm-type">Tipo de cliente</label>' +
            '<select id="cm-type" name="taxpayerType" class="form-input">' +
            '<option value="CONTRIBUYENTE">Contribuyente (recibe Crédito Fiscal)</option>' +
            '<option value="NO_CONTRIBUYENTE">No contribuyente (solo Factura)</option>' +
            '</select></div>' +
            '<div class="sv-form-row">' +
            field('cm-nrc', 'nrc', 'NRC', c.nrc, 'placeholder="000000-0" maxlength="10"') +
            field('cm-nit', 'nit', 'NIT', c.nit, 'placeholder="0000-000000-000-0" maxlength="17"') +
            '</div>' +
            field('cm-email', 'email', 'Correo electrónico', c.email, 'type="email" maxlength="100"') +
            field('cm-address', 'address', 'Dirección', c.address, 'maxlength="200"');

        return openModal({
            title,
            body,
            submitLabel: submitLabel || 'Guardar cliente',
            onOpen(form) {
                const type = form.querySelector('#cm-type');
                const nrc = form.querySelector('#cm-nrc');
                type.value = c.taxpayerType || 'CONTRIBUYENTE';
                const sync = () => { nrc.disabled = type.value !== 'CONTRIBUYENTE'; };
                type.addEventListener('change', sync);
                sync();
            },
            async onSubmit(values) {
                const payload = {
                    name: values.name,
                    taxpayerType: values.taxpayerType,
                    nrc: values.nrc || null,
                    nit: values.nit || null,
                    email: values.email || null,
                    address: values.address || null
                };
                const saved = c.id
                    ? await api('/api/customer/edit?customerId=' + encodeURIComponent(c.id), { method: 'PATCH', body: payload })
                    : await api('/api/customer/save', { method: 'POST', body: payload });
                await onSaved(saved);
            }
        });
    }

    // ------------------------------------------------------------------
    // Barra lateral
    // ------------------------------------------------------------------

    function renderSidebar(session) {
        const isSeller = session.roles.includes('VEND');
        const role = ROLE_LABELS[session.roles.find(r => ROLE_LABELS[r])] || '';

        document.querySelectorAll('.sidebar-user .user-avatar').forEach(el => { el.textContent = initials(session.fullName); });
        document.querySelectorAll('.sidebar-user .user-name').forEach(el => { el.textContent = session.fullName; });
        document.querySelectorAll('.sidebar-user .user-role').forEach(el => {
            el.textContent = role + (isSeller ? ' · Caja 02' : '');
        });

        const userBox = document.querySelector('.sidebar-user');
        if (userBox && !userBox.querySelector('.sv-logout')) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'sv-logout';
            button.title = 'Cerrar sesión';
            button.setAttribute('aria-label', 'Cerrar sesión');
            button.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline>' +
                '<line x1="21" y1="12" x2="9" y2="12"></line></svg>';
            button.addEventListener('click', () => logout());
            userBox.appendChild(button);
        }

        // Opciones según el rol (la API también lo valida)
        document.querySelectorAll('[data-roles]').forEach(el => {
            if (!hasRole(session, el.dataset.roles.split(','))) el.remove();
        });

        // Secciones que aún no existen
        document.querySelectorAll('.sidebar-nav a[href="#"]').forEach(link => {
            link.classList.add('sv-soon');
            link.title = 'Disponible en un próximo sprint';
            link.addEventListener('click', event => {
                event.preventDefault();
                toast('Esta sección estará disponible en un próximo sprint.');
            });
        });

        refreshStockBadge();
    }

    /** Número de productos bajo el stock mínimo junto a "Inventario" en la barra lateral. */
    function updateStockBadge(products) {
        const count = (products || []).filter(p => p.stock < p.minStock).length;
        document.querySelectorAll('.nav-badge').forEach(badge => { badge.textContent = count > 0 ? count : ''; });
    }

    async function refreshStockBadge() {
        try {
            updateStockBadge(await api('/api/product/all'));
        } catch (e) {
            // El badge es informativo: si falla no se interrumpe la pantalla
        }
    }

    window.SV = {
        api, ApiError,
        getSession, saveSession, requireAuth, logout, hasRole, homeFor, minutesLeft,
        formatMoney, splitMoney, parseDate, formatTime, formatDate, formatDateTime, formatWhen, isToday,
        escapeHtml, initials, normalize,
        STATUS, statusInfo, dteUrl, statusBadge, typeBadge,
        toast, openModal, openCustomerModal, updateStockBadge
    };
})();
