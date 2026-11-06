// Dashboard del vendedor: métricas del día, ventas recientes, avisos de stock y cola de contingencia
(function () {
    'use strict';

    const session = SV.requireAuth(['VEND', 'ADMN']);
    if (!session) return;

    const $ = id => document.getElementById(id);
    const esc = SV.escapeHtml;
    const REFRESH_MS = 30000;

    let todaysDtes = [];

    const alertIcon = '<div class="alert-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>' +
        '<line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg></div>';
    const dotsIcon = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>';

    // ---------- Encabezado y caja ----------

    function renderClock() {
        const now = new Date();
        const date = now.toLocaleDateString('es-SV', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        const time = now.toLocaleTimeString('es-SV', { hour: 'numeric', minute: '2-digit' });
        $('currentDate').textContent = date.charAt(0).toUpperCase() + date.slice(1) + ' · ' + time;
        $('sessionStatus').textContent = 'Sesión activa · expira en ' + SV.minutesLeft(session) + ' min';
    }

    $('cashierName').textContent = 'Turno actual · ' + session.fullName;
    $('cashOpenedAt').textContent = new Date(session.loginAt)
        .toLocaleTimeString('es-SV', { hour: 'numeric', minute: '2-digit' });

    document.querySelector('.btn-close-register').addEventListener('click', () => {
        const total = todaysDtes.reduce((sum, dte) => sum + Number(dte.total), 0);
        const confirmed = window.confirm('¿Cerrar la caja y terminar el turno?\n\n' +
            'Ventas del turno: ' + todaysDtes.length + '\nTotal: ' + SV.formatMoney(total));
        if (confirmed) SV.logout();
    });

    // ---------- Datos ----------

    async function load() {
        const [dtes, products] = await Promise.allSettled([SV.api('/api/dte/all'), SV.api('/api/product/all')]);

        if (dtes.status === 'fulfilled') {
            renderSales(dtes.value);
            renderContingency(dtes.value.filter(dte => dte.status === 'CONTINGENCIA'));
        } else {
            $('recentSales').innerHTML = '<tr><td colspan="7" class="sv-empty">' + esc(dtes.reason.message) + '</td></tr>';
            $('contingencyText').textContent = dtes.reason.message;
        }

        if (products.status === 'fulfilled') {
            renderStockAlerts(products.value);
            SV.updateStockBadge(products.value);
        } else {
            $('stockAlerts').innerHTML = '<div class="sv-empty">' + esc(products.reason.message) + '</div>';
        }
    }

    function renderSales(dtes) {
        todaysDtes = dtes.filter(dte => SV.isToday(dte.issuedAt) && dte.status !== 'ANULADO');
        const sealed = todaysDtes.filter(dte => dte.status === 'SELLADO').length;
        const pending = todaysDtes.filter(dte => dte.status === 'CONTINGENCIA').length;
        const rejected = todaysDtes.filter(dte => dte.status === 'RECHAZADO').length;
        const total = todaysDtes.reduce((sum, dte) => sum + Number(dte.total), 0);

        $('metricSales').textContent = todaysDtes.length;
        $('cashSalesCount').textContent = todaysDtes.length;
        $('metricDtes').textContent = sealed;

        const note = $('metricDtesNote');
        note.className = 'metric-change' + (pending || rejected ? ' warning' : ' positive');
        note.textContent = pending || rejected
            ? [pending && pending + ' en contingencia', rejected && rejected + ' rechazado(s)'].filter(Boolean).join(' · ')
            : 'Todos sellados por Hacienda';

        const [integer, decimals] = SV.splitMoney(total);
        $('metricTotal').innerHTML = esc(integer) + '<span class="metric-decimal">' + esc(decimals) + '</span>';

        const recent = dtes.slice(0, 5);
        $('recentSales').innerHTML = recent.length
            ? recent.map(dte =>
                '<tr>' +
                '<td class="sale-number">' + String(dte.saleNumber).padStart(4, '0') + '</td>' +
                '<td>' + esc(dte.customerName) + '</td>' +
                '<td>' + SV.typeBadge(dte.type) + '</td>' +
                '<td class="amount-cell">' + SV.formatMoney(dte.total) + '</td>' +
                '<td>' + SV.statusBadge(dte.status) + '</td>' +
                '<td>' + SV.formatWhen(dte.issuedAt) + '</td>' +
                '<td><a href="' + SV.dteUrl(dte) + '" class="action-icon" title="Ver documento">' + dotsIcon + '</a></td>' +
                '</tr>').join('')
            : '<tr><td colspan="7" class="sv-empty">Aún no hay ventas. <a href="pos.html">Registra la primera</a>.</td></tr>';
    }

    function renderStockAlerts(products) {
        const low = products
            .filter(p => p.stock < p.minStock)
            .sort((a, b) => a.stock / a.minStock - b.stock / b.minStock);
        $('stockAlertCount').textContent = low.length;
        $('stockAlerts').innerHTML = low.length
            ? low.slice(0, 5).map(p =>
                '<div class="stock-alert-item">' + alertIcon +
                '<div class="alert-info"><span class="alert-name">' + esc(p.name) + '</span>' +
                '<span class="alert-code">' + esc(p.code) + '</span></div>' +
                '<div class="alert-stock">Stock: <strong>' + p.stock + '</strong> · Mín: ' + p.minStock + '</div>' +
                '</div>').join('')
            : '<div class="sv-empty">Todo el inventario está sobre el mínimo.</div>';
    }

    function renderContingency(pending) {
        $('contingencyCount').textContent = pending.length;
        $('retryAllButton').classList.toggle('sv-hidden', pending.length === 0);
        $('contingencyText').textContent = pending.length
            ? pending.length + ' documento(s) pendiente(s) de transmisión. El sistema reintentará automáticamente.'
            : 'No hay documentos pendientes: todos fueron transmitidos a Hacienda.';
        $('contingencyList').innerHTML = pending.map(dte =>
            '<a class="contingency-item" href="' + SV.dteUrl(dte) + '">' +
            '<div class="contingency-item-info">' +
            '<span class="contingency-item-number">#' + String(dte.saleNumber).padStart(4, '0') + '</span>' +
            '<span class="contingency-item-client">' + esc(dte.customerName) + '</span></div>' +
            '<div class="contingency-item-details"><span>' + SV.formatMoney(dte.total) + '</span>' +
            '<span>· En cola desde ' + SV.formatTime(dte.issuedAt) + '</span></div>' +
            '</a>').join('');
        $('retryAllButton').dataset.ids = pending.map(dte => dte.id).join(',');
    }

    $('retryAllButton').addEventListener('click', async event => {
        const button = event.currentTarget;
        const ids = (button.dataset.ids || '').split(',').filter(Boolean);
        if (!ids.length) return;

        button.disabled = true;
        button.textContent = 'Reintentando...';
        let stamped = 0;
        try {
            for (const id of ids) {
                const dte = await SV.api('/api/dte/retry?dteId=' + encodeURIComponent(id), { method: 'POST' });
                if (dte.status === 'SELLADO') stamped++;
            }
            SV.toast(stamped
                ? stamped + ' documento(s) sellado(s) por Hacienda.'
                : 'Hacienda sigue sin responder. Se reintentará automáticamente.', stamped ? 'success' : 'warning');
        } catch (error) {
            SV.toast(error.message, 'error');
        } finally {
            button.disabled = false;
            button.textContent = 'Reintentar ahora';
            load();
        }
    });

    renderClock();
    setInterval(renderClock, 30000);
    load();
    setInterval(load, REFRESH_MS);
})();
