import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { api, ApiError } from '../api';
import { formatMoney, normalize } from '../format';
import { hasRole } from '../session';
import Modal from '../components/Modal';
import { useToast } from '../components/Toast';

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

export default function Inventario() {
    const session = useOutletContext();
    const toast = useToast();
    const canRegisterEntries = hasRole(session, ['INVE', 'ADMN']);

    const [products, setProducts] = useState([]);
    const [error, setError] = useState(null);
    const [term, setTerm] = useState('');
    const [entryModal, setEntryModal] = useState(null); // productId o null

    const load = useCallback(async () => {
        try {
            setProducts(await api('/api/product/all'));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, []);

    useEffect(() => {
        load();
        const interval = setInterval(load, 30000);
        return () => clearInterval(interval);
    }, [load]);

    const low = products.filter(p => p.stock > 0 && p.stock < p.minStock).length;
    const out = products.filter(p => p.stock === 0).length;
    const value = products.reduce((sum, p) => sum + p.price * p.stock, 0);

    const t = normalize(term);
    const rows = products
        .filter(p => !t || normalize(p.name).includes(t) || normalize(p.code).includes(t))
        .sort((a, b) => (a.stock / (a.minStock || 1)) - (b.stock / (b.minStock || 1)));

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Existencias</h1>
                    <p>Bodega central · {products.length} productos</p>
                </div>
                <div className="header-right">
                    <div className="search-box">
                        <span className="search-icon">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8"></circle>
                                <path d="m21 21-4.35-4.35"></path>
                            </svg>
                        </span>
                        <input type="text" placeholder="Buscar producto..." autoComplete="off"
                               value={term} onChange={event => setTerm(event.target.value)} />
                    </div>
                    {canRegisterEntries && (
                        <button className="btn-add-entry" onClick={() => setEntryModal('')}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="12" y1="5" x2="12" y2="19"></line>
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                            Registrar entrada
                        </button>
                    )}
                </div>
            </header>

            <div className="inventory-content">
                <div className="stats-grid">
                    <div className="stat-card">
                        <div className="stat-label">Productos activos</div>
                        <div className="stat-value">{products.length}</div>
                        <div className="stat-bar blue"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-label">Bajo stock mínimo</div>
                        <div className="stat-value warning">{low}</div>
                        <div className="stat-subtitle warning">Requiere reabastecer</div>
                        <div className="stat-bar orange"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-label">Agotados</div>
                        <div className="stat-value danger">{out}</div>
                        <div className="stat-bar red"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-label">Valor del inventario</div>
                        <div className="stat-value">
                            {value >= 1000
                                ? '$' + (value / 1000).toLocaleString('en-US', { maximumFractionDigits: 1 }) + 'K'
                                : formatMoney(value)}
                        </div>
                        <div className="stat-subtitle">Valorizado a precio de venta</div>
                        <div className="stat-bar green"></div>
                    </div>
                </div>

                <div className="inventory-table-container">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Catálogo de existencias</h2>
                            <span className="table-subtitle">· Ordenado por nivel de stock</span>
                        </div>
                        {low + out > 0 && (
                            <span className="alert-count">
                                <span className="alert-dot"></span>
                                <span>{low + out}{low + out === 1 ? ' alerta activa' : ' alertas activas'}</span>
                            </span>
                        )}
                    </div>

                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>PRODUCTO</th>
                                <th>CÓDIGO</th>
                                <th>CATEGORÍA</th>
                                <th>STOCK</th>
                                <th>NIVEL</th>
                                <th>MÍN.</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="7" className="sv-empty">{error}</td></tr>
                            ) : rows.length ? rows.map(p => {
                                const level = levelOf(p);
                                return (
                                    <tr key={p.id}
                                        className={canRegisterEntries ? 'sv-clickable' : ''}
                                        title={canRegisterEntries ? 'Registrar entrada' : ''}
                                        onClick={() => { if (canRegisterEntries) setEntryModal(p.id); }}>
                                        <td className="product-name-cell">{p.name}</td>
                                        <td className="code-cell">{p.code}</td>
                                        <td>{p.category}</td>
                                        <td className={'stock-cell ' + level.css}>{p.stock}</td>
                                        <td>
                                            <div className="level-bar">
                                                <div className={'level-fill ' + level.css}
                                                     style={{ width: levelWidth(p) + '%' }}></div>
                                            </div>
                                        </td>
                                        <td>{p.minStock}</td>
                                        <td><span className={'status-badge ' + level.css}>{level.label}</span></td>
                                    </tr>
                                );
                            }) : (
                                <tr><td colSpan="7" className="sv-empty">No hay productos que coincidan con la búsqueda.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {entryModal !== null && (
                <Modal
                    title="Registrar entrada de mercadería"
                    submitLabel="Registrar entrada"
                    onClose={() => setEntryModal(null)}
                    onSubmit={async values => {
                        const quantity = Number(values.quantity);
                        if (!Number.isInteger(quantity) || quantity < 1) {
                            throw new ApiError('Ingresa una cantidad entera mayor que cero.', 400,
                                { quantity: 'Cantidad inválida' });
                        }
                        const product = await api('/api/inventory/entry', {
                            method: 'POST',
                            body: { productId: values.productId, quantity }
                        });
                        toast('Entrada registrada: +' + quantity + ' ' + product.name + ' (stock: ' + product.stock + ').',
                            'success');
                        await load();
                    }}
                >
                    <div className="form-group">
                        <label htmlFor="en-product">Producto</label>
                        <select id="en-product" name="productId" className="form-input" defaultValue={entryModal}>
                            {products.slice().sort((a, b) => a.name.localeCompare(b.name)).map(p => (
                                <option key={p.id} value={p.id}>{p.name} · {p.code} (stock: {p.stock})</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label htmlFor="en-qty">Cantidad recibida</label>
                        <input id="en-qty" name="quantity" type="number" min="1" max="10000" step="1"
                               className="form-input" defaultValue="1" />
                    </div>
                </Modal>
            )}
        </>
    );
}
