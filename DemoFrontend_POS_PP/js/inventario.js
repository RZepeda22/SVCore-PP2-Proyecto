// Inventario: existencias, alertas de stock y registro de entradas de mercadería
(function () {
    'use strict';

    const session = SV.requireAuth();
    if (!session) return;

    const $ = id => document.getElementById(id);
    const esc = SV.escapeHtml;
    const canRegisterEntries = SV.hasRole(session, ['INVE', 'ADMN']);

    let products = [];
    let term = '';

    function levelOf(product) {
        if (product.stock === 0) return { label: 'Agotado', css: 'danger' };
        if (product.stock < product.minStock) return { label: 'Bajo', css: 'warning' };
        return { label: 'Óptimo', css: 'success' };
    }

    /** Ancho de la barra: el mínimo equivale a un tercio de la barra llena. */
    function levelWidth(product) {
        if (!product.minStock) return product.stock > 0 ? 100 : 0;
        return Math.min(100, Math.round(product.stock / (product.minStock * 3) * 100));
    }

    async function load() {
        try {
            products = await SV.api('/api/product/all');
            SV.updateStockBadge(products);
            render();
        } catch (error) {
            $('inventoryRows').innerHTML = '<tr><td colspan="7" class="sv-empty">' + esc(error.message) + '</td></tr>';
        }
    }

    function render() {
        const low = products.filter(p => p.stock > 0 && p.stock < p.minStock).length;
        const out = products.filter(p => p.stock === 0).length;
        const value = products.reduce((sum, p) => sum + p.price * p.stock, 0);

        $('inventorySubtitle').textContent = 'Bodega central · ' + products.length + ' productos';
        $('statActive').textContent = products.length;
        $('statLow').textContent = low;
        $('statOut').textContent = out;
        $('statValue').textContent = value >= 1000
            ? '$' + (value / 1000).toLocaleString('en-US', { maximumFractionDigits: 1 }) + 'K'
            : SV.formatMoney(value);
        $('alertCountText').textContent = (low + out) + (low + out === 1 ? ' alerta activa' : ' alertas activas');
        $('alertCount').classList.toggle('sv-hidden', low + out === 0);

        const t = SV.normalize(term);
        const rows = products
            .filter(p => !t || SV.normalize(p.name).includes(t) || SV.normalize(p.code).includes(t))
            .sort((a, b) => (a.stock / (a.minStock || 1)) - (b.stock / (b.minStock || 1)));

        $('inventoryRows').innerHTML = rows.length
            ? rows.map(p => {
                const level = levelOf(p);
                return '<tr data-id="' + esc(p.id) + '"' + (canRegisterEntries ? ' class="sv-clickable" title="Registrar entrada"' : '') + '>' +
                    '<td class="product-name-cell">' + esc(p.name) + '</td>' +
                    '<td class="code-cell">' + esc(p.code) + '</td>' +
                    '<td>' + esc(p.category) + '</td>' +
                    '<td class="stock-cell ' + level.css + '">' + p.stock + '</td>' +
                    '<td><div class="level-bar"><div class="level-fill ' + level.css + '" style="width: ' +
                    levelWidth(p) + '%"></div></div></td>' +
                    '<td>' + p.minStock + '</td>' +
                    '<td><span class="status-badge ' + level.css + '">' + level.label + '</span></td>' +
                    '</tr>';
            }).join('')
            : '<tr><td colspan="7" class="sv-empty">No hay productos que coincidan con la búsqueda.</td></tr>';
    }

    $('inventorySearch').addEventListener('input', event => {
        term = event.target.value;
        render();
    });

    // ---------- Registrar entrada (Encargado de inventario y Administrador) ----------

    function openEntryModal(productId) {
        const options = products
            .slice()
            .sort((a, b) => a.name.localeCompare(b.name))
            .map(p => '<option value="' + esc(p.id) + '"' + (p.id === productId ? ' selected' : '') + '>' +
                esc(p.name) + ' · ' + esc(p.code) + ' (stock: ' + p.stock + ')</option>')
            .join('');

        SV.openModal({
            title: 'Registrar entrada de mercadería',
            submitLabel: 'Registrar entrada',
            body:
                '<div class="form-group"><label for="en-product">Producto</label>' +
                '<select id="en-product" name="productId" class="form-input">' + options + '</select></div>' +
                '<div class="form-group"><label for="en-qty">Cantidad recibida</label>' +
                '<input id="en-qty" name="quantity" type="number" min="1" max="10000" step="1" class="form-input" value="1"></div>',
            async onSubmit(values) {
                const quantity = Number(values.quantity);
                if (!Number.isInteger(quantity) || quantity < 1) {
                    throw new SV.ApiError('Ingresa una cantidad entera mayor que cero.', 400, { quantity: 'Cantidad inválida' });
                }
                const product = await SV.api('/api/inventory/entry', {
                    method: 'POST',
                    body: { productId: values.productId, quantity }
                });
                SV.toast('Entrada registrada: +' + quantity + ' ' + product.name + ' (stock: ' + product.stock + ').', 'success');
                await load();
            }
        });
    }

    if (canRegisterEntries) {
        $('addEntryButton').classList.remove('sv-hidden');
        $('addEntryButton').addEventListener('click', () => openEntryModal());
        $('inventoryRows').addEventListener('click', event => {
            const row = event.target.closest('tr[data-id]');
            if (row) openEntryModal(row.dataset.id);
        });
    }

    load();
    setInterval(load, 30000);
})();
