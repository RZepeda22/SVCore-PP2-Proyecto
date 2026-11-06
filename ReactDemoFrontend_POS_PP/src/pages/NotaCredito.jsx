import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { formatMoney, formatWhen } from '../format';
import { TypeBadge } from '../components/Badges';
import Modal from '../components/Modal';
import { useToast } from '../components/Toast';

export default function NotaCredito() {
    const toast = useToast();
    const navigate = useNavigate();
    const [dtes, setDtes] = useState([]);
    const [error, setError] = useState(null);
    const [selected, setSelected] = useState(null); // resumen del CCF
    const [detail, setDetail] = useState(null);
    const [quantities, setQuantities] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState(null);
    const [reason, setReason] = useState('');
    const [returnToStock, setReturnToStock] = useState(true);

    const load = useCallback(async () => {
        try {
            const list = await api('/api/dte/all?status=SELLADO');
            // La nota de crédito solo aplica sobre un Comprobante de Crédito Fiscal sellado
            setDtes(list.filter(dte => dte.type === 'CCF'));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    async function selectDte(dte) {
        setSelected(dte);
        setDetail(null);
        setFormError(null);
        setReason('');
        setReturnToStock(true);
        setQuantities({});
        try {
            const data = await api('/api/dte/?dteId=' + encodeURIComponent(dte.id));
            setDetail(data);
            setQuantities(Object.fromEntries(data.items.map(item => [item.productId, 0])));
        } catch (err) {
            setFormError(err.message);
        }
    }

    const creditedItems = detail ? detail.items.filter(item => Number(quantities[item.productId]) > 0) : [];
    const creditSubtotal = creditedItems.reduce((sum, item) =>
        sum + Number(item.unitPrice) * Number(quantities[item.productId]), 0);

    async function submit(event) {
        event.preventDefault();
        setFormError(null);
        if (!creditedItems.length) {
            setFormError('Indica al menos una unidad devuelta.');
            return;
        }
        if (!reason.trim()) {
            setFormError('Indica el motivo de la nota de crédito.');
            return;
        }
        setSubmitting(true);
        try {
            const nc = await api('/api/dte/credit-note', {
                method: 'POST',
                body: {
                    dteId: selected.id,
                    items: creditedItems.map(item => ({
                        productId: item.productId,
                        quantity: Number(quantities[item.productId])
                    })),
                    reason: reason.trim(),
                    returnToStock
                }
            });
            toast('Nota de crédito emitida.', 'success');
            navigate('/dte/' + nc.id);
        } catch (err) {
            setFormError(err.message);
            setSubmitting(false);
        }
    }

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Nota de crédito</h1>
                    <p>Devolución sobre un Crédito Fiscal sellado · genera un DTE tipo 05</p>
                </div>
            </header>

            <div className="inventory-content">
                <div className="sv-notice">
                    <strong>¿Cómo funciona?</strong>
                    <span>Elige el CCF sellado, indica las unidades devueltas de cada producto y el motivo. Por defecto la
                        mercadería regresa al inventario. No se puede acreditar más de lo vendido, descontando las notas
                        anteriores.</span>
                </div>

                <div className="sv-report-grid">
                    <div className="inventory-table-container">
                        <div className="table-header">
                            <div className="table-title">
                                <h2>Créditos fiscales sellados</h2>
                            </div>
                        </div>
                        <table className="inventory-table">
                            <thead>
                                <tr>
                                    <th>CÓDIGO</th>
                                    <th>CLIENTE</th>
                                    <th>TOTAL</th>
                                    <th>EMITIDO</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {error ? (
                                    <tr><td colSpan="5" className="sv-empty">{error}</td></tr>
                                ) : dtes.length ? dtes.map(dte => (
                                    <tr key={dte.id} className={selected && selected.id === dte.id ? 'sv-selected-row' : ''}>
                                        <td className="code-cell" title={dte.generationCode}>
                                            <Link to={'/dte/' + dte.id}>{dte.generationCode.slice(0, 14)}...</Link>
                                        </td>
                                        <td>{dte.customerName}</td>
                                        <td className="amount-cell">{formatMoney(dte.total)}</td>
                                        <td>{formatWhen(dte.issuedAt)}</td>
                                        <td>
                                            <button type="button" className="action-link" onClick={() => selectDte(dte)}>
                                                {selected && selected.id === dte.id ? 'Seleccionado' : 'Aplicar nota'}</button>
                                        </td>
                                    </tr>
                                )) : (
                                    <tr><td colSpan="5" className="sv-empty">
                                        No hay Créditos Fiscales sellados disponibles.</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>

                    {selected && (
                        <div className="inventory-table-container">
                            <div className="table-header">
                                <div className="table-title">
                                    <h2>Productos a acreditar</h2>
                                    <span className="table-subtitle">· {selected.customerName}</span>
                                </div>
                                <TypeBadge shortName={selected.type} />
                            </div>
                            {formError && !detail && <div className="sv-notice danger">{formError}</div>}
                            {!detail ? (
                                <div className="sv-empty">Cargando detalle del documento...</div>
                            ) : (
                                <form onSubmit={submit} className="sv-nc-form">
                                    <table className="inventory-table">
                                        <thead>
                                            <tr>
                                                <th>PRODUCTO</th>
                                                <th>VENDIDAS</th>
                                                <th>PRECIO UNIT.</th>
                                                <th>DEVUELTAS</th>
                                                <th>MONTO</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {detail.items.map(item => {
                                                const qty = Number(quantities[item.productId]) || 0;
                                                return (
                                                    <tr key={item.productId}>
                                                        <td className="product-name-cell">{item.description}<br />
                                                            <span className="item-code">{item.code}</span></td>
                                                        <td>{item.quantity}</td>
                                                        <td>{formatMoney(item.unitPrice)}</td>
                                                        <td>
                                                            <input type="number" className="form-input sv-qty-input"
                                                                   min="0" max={item.quantity} step="1" value={qty}
                                                                   onChange={event => setQuantities({
                                                                       ...quantities,
                                                                       [item.productId]: Math.max(0, Math.min(item.quantity,
                                                                           Number(event.target.value) || 0))
                                                                   })} />
                                                        </td>
                                                        <td className="amount-cell">{formatMoney(Number(item.unitPrice) * qty)}</td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>

                                    <div className="sv-nc-fields">
                                        <div className="form-group">
                                            <label htmlFor="nc-reason">Motivo de la nota de crédito</label>
                                            <textarea id="nc-reason" className="form-input" rows="2" maxLength={250} required
                                                      placeholder="Ej.: devolución de mercadería" value={reason}
                                                      onChange={event => setReason(event.target.value)}></textarea>
                                        </div>
                                        <label className="sv-check-option">
                                            <input type="checkbox" checked={returnToStock}
                                                   onChange={event => setReturnToStock(event.target.checked)} />
                                            Devolver la mercadería al inventario
                                        </label>
                                        <div className="summary-row total">
                                            <span>Subtotal a acreditar (sin IVA)</span>
                                            <span>{formatMoney(creditSubtotal)}</span>
                                        </div>
                                        {formError && <div className="sv-form-error">{formError}</div>}
                                        <button type="submit" className="btn-confirm" disabled={submitting}>
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="20 6 9 17 4 12"></polyline>
                                            </svg>
                                            {submitting ? 'Emitiendo nota de crédito...' : 'Emitir nota de crédito'}
                                        </button>
                                    </div>
                                </form>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
