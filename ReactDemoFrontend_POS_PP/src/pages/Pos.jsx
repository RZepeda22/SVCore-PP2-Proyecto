import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { formatMoney, initials, normalize } from '../format';
import { useToast } from '../components/Toast';
import CustomerModal from '../components/CustomerModal';

const DOC_TYPE = { factura: 'FACTURA', 'credito-fiscal': 'CREDITO_FISCAL' };
const IVA_RATE = 0.13;
const CATEGORIES = [
    { code: 'all', label: 'Todos' },
    { code: 'bikes', label: 'Bicicletas' },
    { code: 'components', label: 'Componentes' },
    { code: 'clothing', label: 'Ropa' },
    { code: 'accessories', label: 'Accesorios' }
];
const PAYMENTS = [
    { code: 'EFECTIVO', label: 'Efectivo' },
    { code: 'TARJETA', label: 'Tarjeta' },
    { code: 'TRANSFERENCIA', label: 'Transfer.' },
    { code: 'CREDITO', label: 'Crédito' }
];

function iconFor(product) {
    if (product.categoryCode === 'bikes') return '/assets/bike.png';
    if (/helmet|casco/i.test(product.name)) return '/assets/helmet.png';
    return '/assets/box.png';
}

function isContribuyente(customer) {
    return !!customer && customer.taxpayerType === 'CONTRIBUYENTE' && !!customer.nrc;
}

export default function Pos() {
    const navigate = useNavigate();
    const toast = useToast();
    const [params] = useSearchParams();

    const [products, setProducts] = useState([]);
    const [loadError, setLoadError] = useState(null);
    const [cart, setCart] = useState([]);
    const [customer, setCustomer] = useState(null); // null = Consumidor Final
    const [category, setCategory] = useState('all');
    const [productTerm, setProductTerm] = useState('');
    const [searching, setSearching] = useState(!!params.get('cliente'));
    const [customerTerm, setCustomerTerm] = useState('');
    const [customerResults, setCustomerResults] = useState([]);
    const [customerError, setCustomerError] = useState(null);
    const [docType, setDocType] = useState('factura');
    const [payment, setPayment] = useState('EFECTIVO');
    const [submitting, setSubmitting] = useState(false);
    const [customerModal, setCustomerModal] = useState(null);
    const searchTimer = useRef(null);

    const loadProducts = useCallback(async () => {
        try {
            const list = await api('/api/product/all');
            setProducts(list);
            setLoadError(null);
            // Si el stock cambió, el carrito no puede pedir más de lo disponible
            setCart(current => current
                .map(item => ({ product: list.find(p => p.id === item.product.id), qty: item.qty }))
                .filter(item => item.product && item.product.stock > 0)
                .map(item => ({ product: item.product, qty: Math.min(item.qty, item.product.stock) })));
        } catch (error) {
            setLoadError(error.message);
        }
    }, []);

    useEffect(() => { loadProducts(); }, [loadProducts]);

    const searchCustomers = useCallback(query => {
        clearTimeout(searchTimer.current);
        searchTimer.current = setTimeout(async () => {
            try {
                const list = await api('/api/customer/all' + (query ? '?search=' + encodeURIComponent(query) : ''));
                setCustomerResults(list);
                setCustomerError(null);
            } catch (error) {
                setCustomerError(error.message);
            }
        }, 250);
    }, []);

    useEffect(() => {
        if (searching) searchCustomers('');
        return () => clearTimeout(searchTimer.current);
    }, [searching, searchCustomers]);

    function selectCustomer(selected) {
        setCustomer(selected);
        setSearching(false);
        setCustomerTerm('');
        setDocType(isContribuyente(selected) ? 'credito-fiscal' : 'factura');
    }

    function qtyInCart(productId) {
        const item = cart.find(i => i.product.id === productId);
        return item ? item.qty : 0;
    }

    function addToCart(product) {
        const item = cart.find(i => i.product.id === product.id);
        if ((item ? item.qty : 0) >= product.stock) {
            toast('No hay más existencias de ' + product.name + ' (stock: ' + product.stock + ').', 'warning');
            return;
        }
        setCart(current => item
            ? current.map(i => i.product.id === product.id ? { ...i, qty: i.qty + 1 } : i)
            : [...current, { product, qty: 1 }]);
    }

    function changeQty(productId, delta) {
        if (delta > 0) {
            const product = products.find(p => p.id === productId);
            if (product) addToCart(product);
            return;
        }
        setCart(current => current
            .map(i => i.product.id === productId ? { ...i, qty: i.qty - 1 } : i)
            .filter(i => i.qty > 0));
    }

    // Mismo cálculo que la API: centavos enteros e IVA redondeado a 2 decimales
    const subtotalCents = cart.reduce((sum, i) => sum + Math.round(i.product.price * 100) * i.qty, 0);
    const taxCents = Math.round(subtotalCents * IVA_RATE);

    const term = normalize(productTerm);
    const visibleProducts = products.filter(p =>
        (category === 'all' || p.categoryCode === category) &&
        (!term || normalize(p.name).includes(term) || normalize(p.code).includes(term)));

    const contribuyente = isContribuyente(customer);

    async function confirmSale() {
        if (!cart.length) {
            toast('Agrega productos al carrito antes de confirmar la venta.', 'warning');
            return;
        }
        const sale = {
            customerId: customer ? customer.id : null,
            docType: DOC_TYPE[docType] || 'FACTURA',
            paymentMethod: payment,
            items: cart.map(item => ({ productId: item.product.id, quantity: item.qty }))
        };

        setSubmitting(true);
        try {
            const dte = await api('/api/sale/save', { method: 'POST', body: sale });
            navigate('/dte/' + dte.id);
        } catch (error) {
            toast(error.message, 'error');
            if (error.status === 409) loadProducts(); // el stock cambió mientras se armaba la venta
            setSubmitting(false);
        }
    }

    return (
        <>
            <header className="pos-header">
                <div className="pos-header-info">
                    <h1>Nueva venta</h1>
                    <p>Caja 02 · Turno mañana</p>
                </div>
                <div className="search-box">
                    <span className="search-icon">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8"></circle>
                            <path d="m21 21-4.35-4.35"></path>
                        </svg>
                    </span>
                    <input type="text" placeholder="Buscar producto por nombre o código..." autoComplete="off"
                           value={productTerm} onChange={event => setProductTerm(event.target.value)} />
                </div>
            </header>

            <div className="pos-content">
                <section className="products-section">
                    <div className="category-tabs">
                        {CATEGORIES.map(c => (
                            <button key={c.code} className={'category-tab' + (category === c.code ? ' active' : '')}
                                    onClick={() => setCategory(c.code)}>{c.label}</button>
                        ))}
                    </div>

                    <div className="products-grid">
                        {loadError ? (
                            <div className="sv-empty">{loadError}<br />
                                <button type="button" className="sv-btn sv-btn-secondary"
                                        onClick={loadProducts}>Reintentar</button>
                            </div>
                        ) : !visibleProducts.length ? (
                            <div className="sv-empty">No hay productos que coincidan con la búsqueda.</div>
                        ) : visibleProducts.map(p => {
                            const soldOut = p.stock - qtyInCart(p.id) <= 0;
                            return (
                                <div key={p.id} className={'product-card' + (soldOut ? ' sv-disabled' : '')}
                                     title={soldOut ? 'Sin existencias disponibles' : ''}
                                     onClick={() => addToCart(p)}>
                                    <div className="product-image"><img src={iconFor(p)} alt="" /></div>
                                    <span className={p.stock < p.minStock ? 'product-stock-low' : 'product-stock-normal'}>
                                        {p.stock}
                                    </span>
                                    <div className="product-name">{p.name}</div>
                                    <div className="product-code">{p.code}</div>
                                    <div className="product-price">{formatMoney(p.price)}</div>
                                </div>
                            );
                        })}
                    </div>
                </section>

                <aside className="cart-panel">
                    <div className="cart-header">
                        {!searching && !customer && (
                            <div className="customer-section">
                                <div className="customer-info">
                                    <div className="customer-avatar generic">
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                             strokeWidth="2">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="12" cy="7" r="4"></circle>
                                        </svg>
                                    </div>
                                    <div>
                                        <div className="customer-name">Consumidor Final</div>
                                        <div className="customer-nrc">Sin datos fiscales</div>
                                    </div>
                                </div>
                                <button className="btn-assign-customer" onClick={() => setSearching(true)}>
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                         strokeWidth="2">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <path d="m21 21-4.35-4.35"></path>
                                    </svg>
                                    Asignar cliente
                                </button>
                            </div>
                        )}

                        {!searching && customer && (
                            <div className="customer-section">
                                <div className="customer-info">
                                    <div className="customer-avatar">{initials(customer.name)}</div>
                                    <div>
                                        <div className="customer-name">{customer.name}</div>
                                        <div className="customer-nrc">
                                            {contribuyente ? 'NRC ' + customer.nrc + ' · Contribuyente' : 'No contribuyente'}
                                        </div>
                                    </div>
                                </div>
                                <button className="btn-remove-customer" onClick={() => selectCustomer(null)}>
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                         strokeWidth="2">
                                        <line x1="18" y1="6" x2="6" y2="18"></line>
                                        <line x1="6" y1="6" x2="18" y2="18"></line>
                                    </svg>
                                </button>
                            </div>
                        )}

                        {searching && (
                            <div className="customer-search">
                                <div className="search-input-wrapper">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                         strokeWidth="2">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <path d="m21 21-4.35-4.35"></path>
                                    </svg>
                                    <input type="text" placeholder="Buscar por nombre o NRC..." autoComplete="off"
                                           autoFocus value={customerTerm}
                                           onChange={event => {
                                               setCustomerTerm(event.target.value);
                                               searchCustomers(event.target.value.trim());
                                           }}
                                           onKeyDown={event => { if (event.key === 'Escape') setSearching(false); }} />
                                    <button className="btn-cancel-search" onClick={() => setSearching(false)}>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                             strokeWidth="2">
                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                        </svg>
                                    </button>
                                </div>
                                <div className="customer-dropdown">
                                    <div className="dropdown-item default" onClick={() => selectCustomer(null)}>
                                        <div className="dropdown-avatar generic">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                                 stroke="currentColor" strokeWidth="2">
                                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                <circle cx="12" cy="7" r="4"></circle>
                                            </svg>
                                        </div>
                                        <div className="dropdown-info"><span className="dropdown-name">Consumidor Final</span></div>
                                        <span className="dropdown-arrow">→</span>
                                    </div>
                                    <div className="dropdown-divider"></div>
                                    {customerError ? <div className="sv-empty">{customerError}</div> : (
                                        customerResults.length ? customerResults.map(c => (
                                            <div className="dropdown-item" key={c.id} onClick={() => selectCustomer(c)}>
                                                <div className="dropdown-avatar">{initials(c.name)}</div>
                                                <div className="dropdown-info">
                                                    <span className="dropdown-name">{c.name}</span>
                                                    <span className="dropdown-detail">
                                                        {c.nrc ? 'NRC ' + c.nrc : 'No contribuyente'}
                                                    </span>
                                                </div>
                                                <span className="dropdown-type">{isContribuyente(c) ? 'CCF' : 'FAC'}</span>
                                                <span className="dropdown-arrow">→</span>
                                            </div>
                                        )) : <div className="sv-empty">Sin resultados</div>
                                    )}
                                    <div className="dropdown-divider"></div>
                                    <div className="dropdown-item register" onClick={() => setCustomerModal({})}>
                                        <span className="dropdown-register-icon">+</span>
                                        <span className="dropdown-name">Registrar nuevo cliente</span>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className="doc-type-tabs">
                            <button className={'doc-type-tab' + (docType === 'factura' ? ' active' : '')}
                                    onClick={() => setDocType('factura')}>Factura</button>
                            <button className={'doc-type-tab' + (docType === 'credito-fiscal' ? ' active' : '')
                                + (!contribuyente ? ' disabled' : '')}
                                    title={contribuyente ? '' : 'Requiere un cliente contribuyente con NRC'}
                                    onClick={() => {
                                        if (contribuyente) setDocType('credito-fiscal');
                                        else toast('Requiere un cliente contribuyente con NRC', 'warning');
                                    }}>Crédito Fiscal</button>
                            <button className="doc-type-tab disabled"
                                    title="El Ticket aún no está definido para la emisión de DTE"
                                    onClick={() => toast('El Ticket aún no está definido para la emisión de DTE', 'warning')}>
                                Ticket</button>
                        </div>
                    </div>

                    <div className="cart-items">
                        {!cart.length ? (
                            <div className="empty-cart">
                                <div className="empty-cart-icon">
                                    <img src="/assets/shopping-cart.png" alt="Carrito vacío" />
                                </div>
                                <p>Agrega productos para comenzar</p>
                            </div>
                        ) : cart.map(({ product, qty }) => (
                            <div className="cart-item" key={product.id}>
                                <div className="cart-item-qty">{qty}</div>
                                <div className="cart-item-info">
                                    <div className="cart-item-name">{product.name}</div>
                                    <div className="cart-item-code">{product.code} · {formatMoney(product.price)} c/u</div>
                                    <div className="sv-qty-controls">
                                        <button type="button" aria-label="Quitar una unidad"
                                                onClick={() => changeQty(product.id, -1)}>−</button>
                                        <button type="button" aria-label="Agregar una unidad"
                                                onClick={() => changeQty(product.id, 1)}>+</button>
                                        <button type="button" className="sv-remove"
                                                onClick={() => setCart(current => current.filter(i => i.product.id !== product.id))}>
                                            Quitar</button>
                                    </div>
                                </div>
                                <div className="cart-item-price">
                                    <div className="cart-item-total">{formatMoney(product.price * qty)}</div>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="cart-summary">
                        <div className="summary-row"><span>Subtotal</span><span>{formatMoney(subtotalCents / 100)}</span></div>
                        <div className="summary-row"><span>Descuento</span><span>-{formatMoney(0)}</span></div>
                        <div className="summary-row"><span>IVA (13%)</span><span>{formatMoney(taxCents / 100)}</span></div>
                        <div className="summary-row total">
                            <span>Total</span><span>{formatMoney((subtotalCents + taxCents) / 100)}</span>
                        </div>

                        <div className="payment-methods">
                            <div className="payment-label">Forma de pago</div>
                            <div className="payment-options">
                                {PAYMENTS.map(p => (
                                    <button key={p.code}
                                            className={'payment-option' + (payment === p.code ? ' active' : '')}
                                            onClick={() => setPayment(p.code)}>{p.label}</button>
                                ))}
                            </div>
                        </div>

                        <button className="btn-confirm" disabled={submitting} onClick={confirmSale}>
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                 strokeWidth="2">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                            {submitting ? 'Emitiendo DTE...' : 'Confirmar venta y emitir DTE'}
                        </button>
                    </div>
                </aside>
            </div>

            {customerModal && (
                <CustomerModal
                    title="Registrar nuevo cliente"
                    customer={/\d/.test(customerTerm) ? { nrc: customerTerm } : { name: customerTerm }}
                    onClose={() => setCustomerModal(null)}
                    onSaved={saved => {
                        toast('Cliente ' + saved.name + ' registrado.', 'success');
                        selectCustomer(saved);
                    }}
                />
            )}
        </>
    );
}
