// Resultado de la emisión: comprobante sellado, rechazado o en contingencia (según body[data-page])
(function () {
    'use strict';

    const session = SV.requireAuth(['VEND', 'ADMN']);
    if (!session) return;

    const page = document.body.dataset.page;
    const params = new URLSearchParams(window.location.search);
    const dteId = params.get('id');
    const $ = id => document.getElementById(id);
    const esc = SV.escapeHtml;

    const DOC_NAMES = {
        FACTURA: 'Factura Electrónica',
        CREDITO_FISCAL: 'CCF Electrónico',
        NOTA_CREDITO: 'Nota de Crédito Electrónica',
        NOTA_DEBITO: 'Nota de Débito Electrónica'
    };
    const FEMININE = ['FACTURA', 'NOTA_CREDITO', 'NOTA_DEBITO'];
    const TAXPAYER = { CONTRIBUYENTE: 'Contribuyente', NO_CONTRIBUYENTE: 'No contribuyente' };
    const REJECTION_TITLES = { '003': 'Receptor inválido' };

    function set(id, value) {
        const el = $(id);
        if (el) el.textContent = value === null || value === undefined || value === '' ? '—' : value;
    }

    function showMessage(message) {
        document.querySelector('.dte-emitido-content').innerHTML =
            '<div class="sv-empty">' + esc(message) + '<br><a href="dte.html">Ver documentos emitidos</a></div>';
    }

    if (!dteId) {
        showMessage('No se indicó qué documento mostrar.');
        return;
    }

    async function retry() {
        return SV.api('/api/dte/retry?dteId=' + encodeURIComponent(dteId), { method: 'POST' });
    }

    /** Carga el DTE; si su estado ya no corresponde a esta pantalla, va a la correcta. */
    async function load() {
        let dte;
        try {
            dte = await SV.api('/api/dte/?dteId=' + encodeURIComponent(dteId));
        } catch (error) {
            showMessage(error.message);
            return;
        }
        const expected = SV.statusInfo(dte.status).page;
        if (!window.location.pathname.endsWith(expected)) {
            window.location.replace(SV.dteUrl(dte) + '&desde=' + page);
            return;
        }
        renderCommon(dte);
        RENDER[page](dte);
    }

    function renderCommon(dte) {
        set('docTypeBadge', dte.typeLabel);
        set('docType', DOC_NAMES[dte.type] || dte.typeLabel);
        set('generationCode', dte.generationCode);
        set('controlNumber', dte.controlNumber);
        set('rcName', dte.receiver.name);
        set('payment', dte.payment.label + ' · cód. ' + dte.payment.code);
        set('total', SV.formatMoney(dte.total));
    }

    /** Enlace de verificación del portal de consulta pública de Hacienda (confirmar formato con la normativa). */
    function verificationUrl(dte) {
        const d = SV.parseDate(dte.issuedAt);
        const date = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        return 'https://admin.factura.gob.sv/consultaPublica?ambiente=' + encodeURIComponent(dte.environment) +
            '&codGen=' + encodeURIComponent(dte.generationCode) + '&fechaEmi=' + date;
    }

    // ------------------------------------------------------------------

    function renderEmitido(dte) {
        const suffix = FEMININE.includes(dte.type) ? 'emitida' : 'emitido';
        set('bannerTitle', dte.typeLabel + ' ' + suffix);
        set('sealDate', SV.formatDateTime(dte.stampedAt));

        const environment = $('environment');
        environment.textContent = dte.environment === '01' ? 'Producción' : 'Pruebas';
        environment.classList.toggle('production', dte.environment === '01');

        set('receptionStamp', dte.receptionStamp);
        set('issuedAt', SV.formatDateTime(dte.issuedAt, false));

        const e = dte.emitter;
        set('emBusinessName', e.businessName);
        set('emTradeName', e.tradeName);
        set('emNit', e.nit);
        set('emNrc', e.nrc);
        set('emActivity', e.economicActivity);
        set('emEstablishment', e.establishment);

        const r = dte.receiver;
        set('rcTaxpayerType', r.taxpayerType ? TAXPAYER[r.taxpayerType] : 'Consumidor final');
        set('rcNrc', r.nrc);
        set('rcNit', r.nit);
        set('rcAddress', r.address);
        set('rcEmail', r.email);

        $('itemsBody').innerHTML = dte.items.map(item =>
            '<tr><td>' + item.quantity + '</td>' +
            '<td>' + esc(item.description) + '<br><span class="item-code">' + esc(item.code) + '</span></td>' +
            '<td>' + SV.formatMoney(item.unitPrice) + ' c/u</td>' +
            '<td>' + SV.formatMoney(item.nonSubjectSale) + '</td>' +
            '<td>' + SV.formatMoney(item.exemptSale) + '</td>' +
            '<td>' + SV.formatMoney(item.taxedSale) + '</td></tr>').join('');

        const sum = field => dte.items.reduce((total, item) => total + Number(item[field]), 0);
        set('totNonSubject', SV.formatMoney(sum('nonSubjectSale')));
        set('totExempt', SV.formatMoney(sum('exemptSale')));
        set('totTaxed', SV.formatMoney(sum('taxedSale')));
        set('totSubtotal', SV.formatMoney(dte.subtotal));
        set('totDiscount', '-' + SV.formatMoney(dte.discount));
        set('totTax', SV.formatMoney(dte.tax));
        set('totTotal', SV.formatMoney(dte.total));

        $('qrLink').href = verificationUrl(dte);
    }

    function setupEmitidoActions() {
        $('btnPdf').addEventListener('click', () => window.print());
        $('btnEmail').addEventListener('click', async () => {
            const dte = await SV.api('/api/dte/?dteId=' + encodeURIComponent(dteId));
            if (!dte.receiver.email) {
                SV.toast('El cliente no tiene un correo registrado.', 'warning');
                return;
            }
            const body = [
                'Estimado cliente:',
                '',
                'Le compartimos los datos de su ' + dte.typeLabel + ':',
                '',
                'Código de generación: ' + dte.generationCode,
                'Número de control: ' + dte.controlNumber,
                'Sello de recepción: ' + dte.receptionStamp,
                'Total: ' + SV.formatMoney(dte.total),
                '',
                'Puede verificarlo en: ' + verificationUrl(dte),
                '',
                dte.emitter.tradeName
            ].join('\n');
            window.location.href = 'mailto:' + encodeURIComponent(dte.receiver.email) +
                '?subject=' + encodeURIComponent(dte.typeLabel + ' ' + dte.controlNumber) +
                '&body=' + encodeURIComponent(body);
        });

        const from = params.get('desde');
        if (from === 'contingencia') SV.toast('El documento salió de contingencia y ya tiene sello de Hacienda.', 'success');
        if (from === 'rechazado') SV.toast('Datos corregidos: el documento fue sellado por Hacienda.', 'success');
    }

    // ------------------------------------------------------------------

    let receiverDraft = null;

    function renderRechazado(dte) {
        set('attemptDate', SV.formatDateTime(dte.lastAttemptAt || dte.issuedAt));
        set('rejectionCode', 'Código ' + dte.rejectionCode + ' · ' + (REJECTION_TITLES[dte.rejectionCode] || 'Validación rechazada'));
        set('rejectionMessage', dte.rejectionMessage);
        set('statusLabel', 'Rechazado');
        set('rcNrc', dte.receiver.nrc ? dte.receiver.nrc + (dte.rejectionCode === '003' ? ' (inválido)' : '') : null);

        const r = dte.receiver;
        receiverDraft = receiverDraft || {
            id: r.customerId, name: r.name, taxpayerType: r.taxpayerType,
            nrc: r.nrc, nit: r.nit, email: r.email, address: r.address
        };
        const canEdit = !!r.customerId;
        $('btnFixRetry').classList.toggle('sv-hidden', !canEdit);
        $('btnEditCustomer').classList.toggle('sv-hidden', !canEdit);
    }

    async function handleRetryResult(dte) {
        if (dte.status === 'RECHAZADO') {
            SV.toast('Hacienda volvió a rechazar el documento: ' + dte.rejectionMessage, 'error');
            renderCommon(dte);
            renderRechazado(dte);
        } else {
            window.location.replace(SV.dteUrl(dte) + '&desde=rechazado');
        }
    }

    function setupRechazadoActions() {
        $('btnFixRetry').addEventListener('click', () => {
            SV.openCustomerModal({
                title: 'Corregir datos del receptor',
                submitLabel: 'Guardar y reintentar',
                customer: receiverDraft,
                async onSaved(customer) {
                    receiverDraft = customer;
                    await handleRetryResult(await retry());
                }
            });
        });
        $('btnEditCustomer').addEventListener('click', () => {
            SV.openCustomerModal({
                title: 'Editar datos del cliente',
                customer: receiverDraft,
                onSaved(customer) {
                    receiverDraft = customer;
                    SV.toast('Cliente actualizado. Usa "Corregir datos y reintentar" para volver a transmitir.', 'success');
                }
            });
        });
    }

    // ------------------------------------------------------------------

    function renderContingencia(dte) {
        set('savedAt', SV.formatDateTime(dte.issuedAt));
        set('statusLabel', 'Pendiente de sello');
        set('queuedSince', SV.formatDateTime(dte.issuedAt, false));
        set('receptionStamp', '— pendiente —');
        set('stepSale', SV.formatTime(dte.issuedAt, true));
        set('stepSigned', SV.formatTime(dte.issuedAt, true));
        set('stepQueue', SV.formatTime(dte.lastAttemptAt || dte.issuedAt, true) + ' · ' + dte.attempts +
            (dte.attempts === 1 ? ' intento' : ' intentos'));
        set('stepStamp', 'pendiente');
    }

    function setupContingenciaActions() {
        $('btnRetryNow').addEventListener('click', async event => {
            const button = event.currentTarget;
            button.disabled = true;
            try {
                const dte = await retry();
                if (dte.status === 'CONTINGENCIA') {
                    SV.toast('Hacienda sigue sin responder. El sistema seguirá reintentando automáticamente.', 'warning');
                    renderContingencia(dte);
                } else {
                    window.location.replace(SV.dteUrl(dte) + '&desde=contingencia');
                }
            } catch (error) {
                SV.toast(error.message, 'error');
            } finally {
                button.disabled = false;
            }
        });
        // La cola de reintentos trabaja en segundo plano: se consulta el estado cada 10 segundos
        setInterval(load, 10000);
    }

    const RENDER = { emitido: renderEmitido, rechazado: renderRechazado, contingencia: renderContingencia };
    const SETUP = { emitido: setupEmitidoActions, rechazado: setupRechazadoActions, contingencia: setupContingenciaActions };

    SETUP[page]();
    load();
})();
