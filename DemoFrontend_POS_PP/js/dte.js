// Documentos electrónicos: listado, filtros, reintentos y exportación de IVA
(function () {
    'use strict';

    const session = SV.requireAuth(['VEND', 'ADMN']);
    if (!session) return;

    const $ = id => document.getElementById(id);
    const esc = SV.escapeHtml;
    const MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre',
        'Octubre', 'Noviembre', 'Diciembre'];

    let dtes = [];
    let statusFilter = new URLSearchParams(window.location.search).get('status') || '';
    let term = '';

    const now = new Date();
    $('periodLabel').textContent = 'Facturación · ' + MONTHS[now.getMonth()] + ' ' + now.getFullYear();

    async function load() {
        try {
            dtes = await SV.api('/api/dte/all');
            renderStats();
            renderTable();
        } catch (error) {
            $('dteRows').innerHTML = '<tr><td colspan="7" class="sv-empty">' + esc(error.message) + '</td></tr>';
        }
    }

    function renderStats() {
        const today = dtes.filter(dte => SV.isToday(dte.issuedAt));
        const pending = dtes.filter(dte => dte.status === 'CONTINGENCIA').length;
        const rejected = dtes.filter(dte => dte.status === 'RECHAZADO').length;
        const total = today.filter(dte => dte.status !== 'ANULADO').reduce((sum, dte) => sum + Number(dte.total), 0);

        $('statSealed').textContent = today.filter(dte => dte.status === 'SELLADO').length;
        $('statContingency').textContent = pending;
        $('statRejected').textContent = rejected;
        const [integer, decimals] = SV.splitMoney(total);
        $('statTotal').innerHTML = esc(integer) + '<span class="stat-decimal">' + esc(decimals) + '</span>';

        $('contingencyBanner').classList.toggle('sv-hidden', pending === 0);
        $('contingencyTitle').textContent = 'Modo contingencia activo · ' + pending + ' documento(s) en cola';
        $('notificationButton').title = pending + rejected
            ? pending + ' en contingencia, ' + rejected + ' rechazado(s)'
            : 'Sin documentos pendientes';
    }

    function visibleDtes() {
        const t = SV.normalize(term);
        return dtes.filter(dte =>
            (!statusFilter || dte.status === statusFilter) &&
            (!t || SV.normalize(dte.generationCode).includes(t) || SV.normalize(dte.customerName).includes(t)));
    }

    function actionFor(dte) {
        if (dte.status === 'CONTINGENCIA') {
            return '<button type="button" class="action-link retry" data-retry="' + esc(dte.id) + '">Reintentar</button>';
        }
        if (dte.status === 'RECHAZADO') {
            return '<a href="' + SV.dteUrl(dte) + '" class="action-link correct">Corregir</a>';
        }
        return '<a href="' + SV.dteUrl(dte) + '" class="action-link">Ver</a>';
    }

    function renderTable() {
        document.querySelectorAll('.filter-tab').forEach(tab => {
            tab.classList.toggle('active', tab.dataset.status === statusFilter);
        });

        const rows = visibleDtes();
        $('dteRows').innerHTML = rows.length
            ? rows.map(dte =>
                '<tr>' +
                '<td class="code-cell" title="' + esc(dte.generationCode) + '">' +
                '<a href="' + SV.dteUrl(dte) + '">' + esc(dte.generationCode.slice(0, 14)) + '...</a></td>' +
                '<td>' + SV.typeBadge(dte.type) + '</td>' +
                '<td>' + esc(dte.customerName) + '</td>' +
                '<td class="amount-cell">' + SV.formatMoney(dte.total) + '</td>' +
                '<td>' + SV.statusBadge(dte.status) + '</td>' +
                '<td>' + SV.formatWhen(dte.issuedAt) + '</td>' +
                '<td>' + actionFor(dte) + '</td>' +
                '</tr>').join('')
            : '<tr><td colspan="7" class="sv-empty">' +
              (dtes.length ? 'No hay documentos con este filtro.' : 'Aún no se han emitido documentos.') + '</td></tr>';
    }

    document.querySelectorAll('.filter-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            statusFilter = tab.dataset.status;
            renderTable();
        });
    });

    $('dteSearch').addEventListener('input', event => {
        term = event.target.value;
        renderTable();
    });

    $('showQueueButton').addEventListener('click', () => {
        statusFilter = 'CONTINGENCIA';
        renderTable();
    });

    $('notificationButton').addEventListener('click', () => {
        statusFilter = dtes.some(dte => dte.status === 'RECHAZADO') ? 'RECHAZADO' : 'CONTINGENCIA';
        renderTable();
    });

    $('dteRows').addEventListener('click', async event => {
        const button = event.target.closest('[data-retry]');
        if (!button) return;
        button.disabled = true;
        button.textContent = 'Reintentando...';
        try {
            const dte = await SV.api('/api/dte/retry?dteId=' + encodeURIComponent(button.dataset.retry), { method: 'POST' });
            SV.toast(dte.status === 'SELLADO'
                ? 'Documento sellado por Hacienda.'
                : 'Hacienda sigue sin responder. Se reintentará automáticamente.', dte.status === 'SELLADO' ? 'success' : 'warning');
        } catch (error) {
            SV.toast(error.message, 'error');
        }
        load();
    });

    // ---------- Exportar IVA (CSV) ----------

    /** Escapa un valor para CSV y evita inyección de fórmulas al abrirlo en Excel. */
    function csvCell(value) {
        let text = String(value == null ? '' : value);
        if (/^[=+\-@\t\r]/.test(text)) text = "'" + text;
        return '"' + text.replace(/"/g, '""') + '"';
    }

    $('exportButton').addEventListener('click', () => {
        const rows = visibleDtes();
        if (!rows.length) {
            SV.toast('No hay documentos para exportar.', 'warning');
            return;
        }
        const header = ['Fecha', 'Hora', 'Tipo', 'Código de generación', 'Venta', 'Cliente', 'NRC', 'Estado',
            'Subtotal', 'IVA', 'Total'];
        const lines = rows.map(dte => [
            SV.formatDate(dte.issuedAt), SV.formatTime(dte.issuedAt), dte.type, dte.generationCode, dte.saleNumber,
            dte.customerName, dte.customerNrc || '', SV.statusInfo(dte.status).label,
            Number(dte.subtotal).toFixed(2), Number(dte.tax).toFixed(2), Number(dte.total).toFixed(2)
        ].map(csvCell).join(','));

        // BOM para que Excel respete las tildes
        const blob = new Blob(['﻿' + [header.map(csvCell).join(','), ...lines].join('\r\n')],
            { type: 'text/csv;charset=utf-8' });
        const link = document.createElement('a');
        const d = new Date();
        link.href = URL.createObjectURL(blob);
        link.download = 'iva-svcore-' + d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' +
            String(d.getDate()).padStart(2, '0') + '.csv';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(link.href), 1000);
    });

    load();
    setInterval(load, 30000);
})();
