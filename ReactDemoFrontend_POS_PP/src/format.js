const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const moneyFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export function formatMoney(value) {
    return moneyFormat.format(Number(value || 0));
}

/** "$1,848.02" -> ["$1,848", ".02"] para las tarjetas que muestran los decimales en pequeño. */
export function splitMoney(value) {
    const text = formatMoney(value);
    const dot = text.lastIndexOf('.');
    return dot === -1 ? [text, ''] : [text.slice(0, dot), text.slice(dot)];
}

/** La API envía LocalDateTime sin zona ("2026-11-06T14:41:28.123456"): se interpreta como hora local. */
export function parseDate(value) {
    if (!value) return null;
    const date = new Date(String(value).replace(/(\.\d{3})\d+/, '$1'));
    return isNaN(date.getTime()) ? null : date;
}

const pad = n => String(n).padStart(2, '0');

export function formatTime(value, withSeconds) {
    const d = parseDate(value);
    if (!d) return '—';
    return pad(d.getHours()) + ':' + pad(d.getMinutes()) + (withSeconds ? ':' + pad(d.getSeconds()) : '');
}

export function formatDate(value) {
    const d = parseDate(value);
    return d ? d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + d.getFullYear() : '—';
}

/** "20 jun 2026 · 10:42:18" */
export function formatDateTime(value, withSeconds = true) {
    const d = parseDate(value);
    return d ? formatDate(value) + ' · ' + formatTime(value, withSeconds) : '—';
}

export function isToday(value) {
    const d = parseDate(value);
    const now = new Date();
    return !!d && d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
        && d.getDate() === now.getDate();
}

/** Hora si es de hoy; si no, fecha corta y hora. */
export function formatWhen(value) {
    const d = parseDate(value);
    if (!d) return '—';
    return isToday(value) ? formatTime(value) : d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + formatTime(value);
}

export function initials(name) {
    return String(name || '')
        .replace(/[^\p{L}\s]/gu, ' ')
        .split(/\s+/).filter(Boolean).slice(0, 2)
        .map(word => word[0].toUpperCase()).join('') || '?';
}

export function normalize(text) {
    return String(text || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

/** Muestra un valor de la API o un guion si está vacío. */
export function orDash(value) {
    return value === null || value === undefined || value === '' ? '—' : value;
}
