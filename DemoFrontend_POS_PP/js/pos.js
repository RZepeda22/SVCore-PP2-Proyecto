// Punto de venta: catálogo, carrito, cliente y emisión del DTE contra la API
(function () {
    'use strict';

    const session = SV.requireAuth(['VEND', 'ADMN']);
    if (!session) return;

    const $ = id => document.getElementById(id);
    const esc = SV.escapeHtml;

    // Valores que espera la API
    const PAYMENT = { efectivo: 'EFECTIVO', tarjeta: 'TARJETA', transferencia: 'TRANSFERENCIA', credito: 'CREDITO' };
    const DOC_TYPE = { factura: 'FACTURA', 'credito-fiscal': 'CREDITO_FISCAL' };
    const IVA_RATE = 0.13;

    const grid = $('productsGrid');
    const cartContainer = $('cartItems');
    const dropdown = $('customerDropdown');
    const searchInput = $('customerSearchInput');
    const confirmButton = $('confirmBtn');

    let products = [];
    let cart = [];              // [{ product, qty }]
    let currentCustomer = null; // null = Consumidor Final
    let category = 'all';
    let productTerm = '';
    let customers = new Map();
    let searchTimer = null;

    const userIcon = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>';

    // ------------------------------------------------------------------
    // Productos
    // ------------------------------------------------------------------

    async function loadProducts() {
        try {
            products = await SV.api('/api/product/all');
            SV.updateStockBadge(products);
            // Si el stock cambió, el carrito no puede pedir más de lo disponible
            cart = cart
                .map(item => ({ product: products.find(p => p.id === item.product.id), qty: item.qty }))
                .filter(item => item.product && item.product.stock > 0)
                .map(item => ({ product: item.product, qty: Math.min(item.qty, item.product.stock) }));
            renderProducts();
            renderCart();
        } catch (error) {
            grid.innerHTML = '<div class="sv-empty">' + esc(error.message) +
                '<br><button type="button" class="sv-btn sv-btn-secondary" id="retryProducts">Reintentar</button></div>';
            $('retryProducts').addEventListener('click', loadProducts);
        }
    }

    function iconFor(product) {
        if (product.categoryCode === 'bikes') return 'assets/bike.png';
        if (/helmet|casco/i.test(product.name)) return 'assets/helmet.png';
        return 'assets/box.png';
    }

    function qtyInCart(productId) {
        const item = cart.find(i => i.product.id === productId);
        return item ? item.qty : 0;
    }

    function renderProducts() {
        const term = SV.normalize(productTerm);
        const list = products.filter(p =>
            (category === 'all' || p.categoryCode === category) &&
            (!term || SV.normalize(p.name).includes(term) || SV.normalize(p.code).includes(term)));

        if (!list.length) {
            grid.innerHTML = '<div class="sv-empty">No hay productos que coincidan con la búsqueda.</div>';
            return;
        }

        grid.innerHTML = list.map(p => {
            const soldOut = p.stock - qtyInCart(p.id) <= 0;
            return '<div class="product-card' + (soldOut ? ' sv-disabled' : '') + '" data-id="' + esc(p.id) + '"' +
                (soldOut ? ' title="Sin existencias disponibles"' : '') + '>' +
                '<div class="product-image"><img src="' + iconFor(p) + '" alt=""></div>' +
                '<span class="' + (p.stock < p.minStock ? 'product-stock-low' : 'product-stock-normal') + '">' + p.stock + '</span>' +
                '<div class="product-name">' + esc(p.name) + '</div>' +
                '<div class="product-code">' + esc(p.code) + '</div>' +
                '<div class="product-price">' + SV.formatMoney(p.price) + '</div>' +
                '</div>';
        }).join('');
    }

    grid.addEventListener('click', event => {
        const card = event.target.closest('.product-card');
        if (card) addToCart(card.dataset.id);
    });

    document.querySelectorAll('.category-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.category-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            category = tab.dataset.category;
            renderProducts();
        });
    });

    $('productSearch').addEventListener('input', event => {
        productTerm = event.target.value;
        renderProducts();
    });

    // ------------------------------------------------------------------
    // Carrito
    // ------------------------------------------------------------------

    function addToCart(productId) {
        const product = products.find(p => p.id === productId);
        if (!product) return;
        const item = cart.find(i => i.product.id === productId);
        if ((item ? item.qty : 0) >= product.stock) {
            SV.toast('No hay más existencias de ' + product.name + ' (stock: ' + product.stock + ').', 'warning');
            return;
        }
        if (item) item.qty++;
        else cart.push({ product, qty: 1 });
        renderCart();
        renderProducts();
    }

    function changeQty(productId, delta) {
        const item = cart.find(i => i.product.id === productId);
        if (!item) return;
        if (delta > 0) {
            addToCart(productId);
            return;
        }
        item.qty += delta;
        if (item.qty <= 0) cart = cart.filter(i => i !== item);
        renderCart();
        renderProducts();
    }

    function removeFromCart(productId) {
        cart = cart.filter(i => i.product.id !== productId);
        renderCart();
        renderProducts();
    }

    function renderCart() {
        if (!cart.length) {
            cartContainer.innerHTML =
                '<div class="empty-cart"><div class="empty-cart-icon"><img src="assets/shopping-cart.png" alt="Carrito vacío"></div>' +
                '<p>Agrega productos para comenzar</p></div>';
        } else {
            cartContainer.innerHTML = cart.map(({ product, qty }) =>
                '<div class="cart-item" data-id="' + esc(product.id) + '">' +
                '<div class="cart-item-qty">' + qty + '</div>' +
                '<div class="cart-item-info">' +
                '<div class="cart-item-name">' + esc(product.name) + '</div>' +
                '<div class="cart-item-code">' + esc(product.code) + ' · ' + SV.formatMoney(product.price) + ' c/u</div>' +
                '<div class="sv-qty-controls">' +
                '<button type="button" data-action="dec" aria-label="Quitar una unidad">−</button>' +
                '<button type="button" data-action="inc" aria-label="Agregar una unidad">+</button>' +
                '<button type="button" data-action="remove" class="sv-remove">Quitar</button>' +
                '</div></div>' +
                '<div class="cart-item-price"><div class="cart-item-total">' +
                SV.formatMoney(product.price * qty) + '</div></div>' +
                '</div>').join('');
        }
        updateTotals();
    }

    cartContainer.addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.closest('.cart-item').dataset.id;
        if (button.dataset.action === 'inc') changeQty(id, 1);
        if (button.dataset.action === 'dec') changeQty(id, -1);
        if (button.dataset.action === 'remove') removeFromCart(id);
    });

    /** Mismo cálculo que la API: centavos enteros e IVA redondeado a 2 decimales. */
    function updateTotals() {
        const subtotalCents = cart.reduce((sum, i) => sum + Math.round(i.product.price * 100) * i.qty, 0);
        const taxCents = Math.round(subtotalCents * IVA_RATE);
        $('subtotal').textContent = SV.formatMoney(subtotalCents / 100);
        $('discount').textContent = '-' + SV.formatMoney(0);
        $('tax').textContent = SV.formatMoney(taxCents / 100);
        $('total').textContent = SV.formatMoney((subtotalCents + taxCents) / 100);
    }

    // ------------------------------------------------------------------
    // Cliente
    // ------------------------------------------------------------------

    function isContribuyente(customer) {
        return !!customer && customer.taxpayerType === 'CONTRIBUYENTE' && !!customer.nrc;
    }

    function showSearch() {
        $('customerDefault').classList.add('hidden');
        $('customerAssigned').classList.add('hidden');
        $('customerSearch').classList.remove('hidden');
        searchInput.value = '';
        searchInput.focus();
        searchCustomers('');
    }

    function hideSearch() {
        $('customerSearch').classList.add('hidden');
        $(currentCustomer ? 'customerAssigned' : 'customerDefault').classList.remove('hidden');
    }

    function searchCustomers(query) {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(async () => {
            try {
                const list = await SV.api('/api/customer/all' + (query ? '?search=' + encodeURIComponent(query) : ''));
                customers = new Map(list.map(c => [c.id, c]));
                renderDropdown(list);
            } catch (error) {
                dropdown.innerHTML = '<div class="sv-empty">' + esc(error.message) + '</div>';
            }
        }, 250);
    }

    function renderDropdown(list) {
        const items = list.map(c =>
            '<div class="dropdown-item" data-id="' + esc(c.id) + '">' +
            '<div class="dropdown-avatar">' + esc(SV.initials(c.name)) + '</div>' +
            '<div class="dropdown-info"><span class="dropdown-name">' + esc(c.name) + '</span>' +
            '<span class="dropdown-detail">' + (c.nrc ? 'NRC ' + esc(c.nrc) : 'No contribuyente') + '</span></div>' +
            '<span class="dropdown-type">' + (isContribuyente(c) ? 'CCF' : 'FAC') + '</span>' +
            '<span class="dropdown-arrow">→</span></div>').join('');

        dropdown.innerHTML =
            '<div class="dropdown-item default" data-final="1">' +
            '<div class="dropdown-avatar generic">' + userIcon + '</div>' +
            '<div class="dropdown-info"><span class="dropdown-name">Consumidor Final</span></div>' +
            '<span class="dropdown-arrow">→</span></div>' +
            (items ? '<div class="dropdown-divider"></div>' + items : '<div class="sv-empty">Sin resultados</div>') +
            '<div class="dropdown-divider"></div>' +
            '<div class="dropdown-item register" data-register="1">' +
            '<span class="dropdown-register-icon">+</span>' +
            '<span class="dropdown-name">Registrar nuevo cliente</span></div>';
    }

    function selectCustomer(customer) {
        currentCustomer = customer;
        $('customerSearch').classList.add('hidden');
        $('customerDefault').classList.toggle('hidden', !!customer);
        $('customerAssigned').classList.toggle('hidden', !customer);

        if (customer) {
            const box = $('customerAssigned');
            box.querySelector('.customer-avatar').textContent = SV.initials(customer.name);
            box.querySelector('.customer-name').textContent = customer.name;
            box.querySelector('.customer-nrc').textContent = isContribuyente(customer)
                ? 'NRC ' + customer.nrc + ' · Contribuyente'
                : 'No contribuyente';
        }
        updateDocTypeTabs(isContribuyente(customer));
    }

    /** Clasificación normativa: el Crédito Fiscal solo se habilita para contribuyentes con NRC. */
    function updateDocTypeTabs(contribuyente) {
        const ccf = document.querySelector('.doc-type-tab[data-doc="credito-fiscal"]');
        const factura = document.querySelector('.doc-type-tab[data-doc="factura"]');
        document.querySelectorAll('.doc-type-tab').forEach(tab => tab.classList.remove('active'));
        ccf.classList.toggle('disabled', !contribuyente);
        ccf.title = contribuyente ? '' : 'Requiere un cliente contribuyente con NRC';
        (contribuyente ? ccf : factura).classList.add('active');
    }

    $('btnAssignCustomer').addEventListener('click', showSearch);
    $('btnRemoveCustomer').addEventListener('click', () => selectCustomer(null));
    $('btnCancelSearch').addEventListener('click', hideSearch);
    searchInput.addEventListener('input', () => searchCustomers(searchInput.value.trim()));
    searchInput.addEventListener('keydown', event => { if (event.key === 'Escape') hideSearch(); });

    dropdown.addEventListener('click', event => {
        const item = event.target.closest('.dropdown-item');
        if (!item) return;
        if (item.dataset.final) selectCustomer(null);
        else if (item.dataset.register) registerCustomer();
        else if (customers.has(item.dataset.id)) selectCustomer(customers.get(item.dataset.id));
    });

    function registerCustomer() {
        const typed = searchInput.value.trim();
        SV.openCustomerModal({
            title: 'Registrar nuevo cliente',
            customer: /\d/.test(typed) ? { nrc: typed } : { name: typed },
            onSaved(customer) {
                SV.toast('Cliente ' + customer.name + ' registrado.', 'success');
                selectCustomer(customer);
            }
        });
    }

    // ------------------------------------------------------------------
    // Documento, pago y confirmación
    // ------------------------------------------------------------------

    document.querySelectorAll('.doc-type-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            if (tab.classList.contains('disabled')) {
                if (tab.title) SV.toast(tab.title, 'warning');
                return;
            }
            document.querySelectorAll('.doc-type-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        });
    });

    document.querySelectorAll('.payment-option').forEach(option => {
        option.addEventListener('click', () => {
            document.querySelectorAll('.payment-option').forEach(o => o.classList.remove('active'));
            option.classList.add('active');
        });
    });

    const confirmLabel = confirmButton.innerHTML;

    confirmButton.addEventListener('click', async () => {
        if (!cart.length) {
            SV.toast('Agrega productos al carrito antes de confirmar la venta.', 'warning');
            return;
        }
        const docTab = document.querySelector('.doc-type-tab.active');
        const payment = document.querySelector('.payment-option.active');

        const sale = {
            customerId: currentCustomer ? currentCustomer.id : null,
            docType: DOC_TYPE[docTab && docTab.dataset.doc] || 'FACTURA',
            paymentMethod: PAYMENT[payment && payment.dataset.payment] || 'EFECTIVO',
            items: cart.map(item => ({ productId: item.product.id, quantity: item.qty }))
        };

        confirmButton.disabled = true;
        confirmButton.textContent = 'Emitiendo DTE...';
        try {
            const dte = await SV.api('/api/sale/save', { method: 'POST', body: sale });
            window.location.href = SV.dteUrl(dte);
        } catch (error) {
            SV.toast(error.message, 'error');
            if (error.status === 409) loadProducts(); // el stock cambió mientras se armaba la venta
            confirmButton.disabled = false;
            confirmButton.innerHTML = confirmLabel;
        }
    });

    // ------------------------------------------------------------------

    loadProducts();
    if (new URLSearchParams(window.location.search).get('cliente')) showSearch();
})();
