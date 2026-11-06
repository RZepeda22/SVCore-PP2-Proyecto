import { useCallback, useEffect, useState } from 'react';
import { api, downloadFile } from '../api';
import { formatDate, formatMoney, formatWhen } from '../format';
import { StatusBadge } from '../components/Badges';
import { useToast } from '../components/Toast';

function currentMonth() {
    const now = new Date();
    const pad = n => String(n).padStart(2, '0');
    return {
        from: now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-01',
        to: now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
    };
}

export default function Reportes() {
    const toast = useToast();
    const [range, setRange] = useState(currentMonth);
    const [applied, setApplied] = useState(range);
    const [summary, setSummary] = useState(null);
    const [byDay, setByDay] = useState([]);
    const [byPayment, setByPayment] = useState([]);
    const [topProducts, setTopProducts] = useState([]);
    const [ivaRows, setIvaRows] = useState([]);
    const [ivaType, setIvaType] = useState('');
    const [error, setError] = useState(null);
    const [exporting, setExporting] = useState(false);

    const query = useCallback((extra = {}) => {
        const params = new URLSearchParams({ from: applied.from, to: applied.to, ...extra });
        return params.toString();
    }, [applied]);

    const load = useCallback(async () => {
        const params = new URLSearchParams({ from: applied.from, to: applied.to }).toString();
        const results = await Promise.allSettled([
            api('/api/report/summary?' + params),
            api('/api/report/sales-by-day?' + params),
            api('/api/report/sales-by-payment?' + params),
            api('/api/report/top-products?' + params),
            api('/api/report/iva-book?' + params + (ivaType ? '&type=' + ivaType : ''))
        ]);
        const [s, d, p, t, iva] = results;
        if (s.status === 'fulfilled') { setSummary(s.value); setError(null); }
        else setError(s.reason.message);
        if (d.status === 'fulfilled') setByDay(d.value);
        if (p.status === 'fulfilled') setByPayment(p.value);
        if (t.status === 'fulfilled') setTopProducts(t.value);
        if (iva.status === 'fulfilled') setIvaRows(iva.value);
    }, [applied, ivaType]);

    useEffect(() => { load(); }, [load]);

    async function exportIvaCsv() {
        setExporting(true);
        try {
            await downloadFile('/api/report/iva-book/csv?' + query(ivaType ? { type: ivaType } : {}),
                'libro-iva-' + applied.from + '-a-' + applied.to + '.csv');
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setExporting(false);
        }
    }

    const maxDayTotal = Math.max(...byDay.map(d => Number(d.total)), 1);
    const maxPaymentTotal = Math.max(...byPayment.map(p => Number(p.total)), 1);

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Reportes y libro de IVA</h1>
                    <p>CU-06 · Montos netos: ventas menos notas de crédito, sin anulados</p>
                </div>
                <div className="header-right">
                    <form className="sv-filter-bar inline" onSubmit={event => { event.preventDefault(); setApplied(range); }}>
                        <div className="form-group">
                            <label htmlFor="rp-from">Desde</label>
                            <input id="rp-from" type="date" className="form-input" value={range.from}
                                   onChange={e => setRange({ ...range, from: e.target.value })} required />
                        </div>
                        <div className="form-group">
                            <label htmlFor="rp-to">Hasta</label>
                            <input id="rp-to" type="date" className="form-input" value={range.to}
                                   onChange={e => setRange({ ...range, to: e.target.value })} required />
                        </div>
                        <button type="submit" className="sv-btn sv-btn-primary">Aplicar</button>
                    </form>
                </div>
            </header>

            <div className="inventory-content">
                {error && <div className="sv-notice danger">{error}</div>}

                {summary && (
                    <>
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="stat-label">Total neto del periodo</div>
                                <div className="stat-value">{formatMoney(summary.total)}</div>
                                <div className="stat-subtitle">{applied.from} a {applied.to}</div>
                                <div className="stat-bar green"></div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-label">Subtotal</div>
                                <div className="stat-value">{formatMoney(summary.subtotal)}</div>
                                <div className="stat-subtitle">IVA: {formatMoney(summary.tax)}</div>
                                <div className="stat-bar blue"></div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-label">Ventas</div>
                                <div className="stat-value">{summary.sales}</div>
                                <div className="stat-subtitle">Notas de crédito: {summary.creditNotes}</div>
                                <div className="stat-bar blue"></div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-label">Documentos anulados</div>
                                <div className="stat-value danger">{summary.invalidated}</div>
                                <div className="stat-subtitle">Excluidos: {formatMoney(summary.invalidatedTotal)}</div>
                                <div className="stat-bar red"></div>
                            </div>
                        </div>

                        <div className="sv-report-grid">
                            <div className="inventory-table-container">
                                <div className="table-header">
                                    <div className="table-title"><h2>Ventas por día</h2></div>
                                </div>
                                <table className="inventory-table">
                                    <thead>
                                        <tr><th>FECHA</th><th>DOCS.</th><th>TOTAL</th><th></th></tr>
                                    </thead>
                                    <tbody>
                                        {byDay.length ? byDay.map(day => (
                                            <tr key={day.date}>
                                                <td>{formatDate(day.date)}</td>
                                                <td>{day.documents}</td>
                                                <td className="amount-cell">{formatMoney(day.total)}</td>
                                                <td>
                                                    <div className="level-bar">
                                                        <div className="level-fill success"
                                                             style={{ width: Math.round(Number(day.total) / maxDayTotal * 100) + '%' }}></div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )) : (
                                            <tr><td colSpan="4" className="sv-empty">Sin ventas en el periodo.</td></tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>

                            <div>
                                <div className="inventory-table-container sv-report-block">
                                    <div className="table-header">
                                        <div className="table-title"><h2>Ventas por forma de pago</h2></div>
                                    </div>
                                    <table className="inventory-table">
                                        <thead>
                                            <tr><th>FORMA DE PAGO</th><th>VENTAS</th><th>TOTAL</th><th></th></tr>
                                        </thead>
                                        <tbody>
                                            {byPayment.length ? byPayment.map(payment => (
                                                <tr key={payment.method}>
                                                    <td>{payment.label}</td>
                                                    <td>{payment.sales}</td>
                                                    <td className="amount-cell">{formatMoney(payment.total)}</td>
                                                    <td>
                                                        <div className="level-bar">
                                                            <div className="level-fill"
                                                                 style={{ width: Math.round(Number(payment.total) / maxPaymentTotal * 100) + '%' }}></div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )) : (
                                                <tr><td colSpan="4" className="sv-empty">Sin ventas en el periodo.</td></tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="inventory-table-container sv-report-block">
                                    <div className="table-header">
                                        <div className="table-title"><h2>Productos más vendidos</h2></div>
                                    </div>
                                    <table className="inventory-table">
                                        <thead>
                                            <tr><th>PRODUCTO</th><th>CÓDIGO</th><th>UNIDADES</th><th>SUBTOTAL</th></tr>
                                        </thead>
                                        <tbody>
                                            {topProducts.length ? topProducts.map(product => (
                                                <tr key={product.productId}>
                                                    <td className="product-name-cell">{product.name}</td>
                                                    <td className="code-cell">{product.code}</td>
                                                    <td>{product.quantity}</td>
                                                    <td className="amount-cell">{formatMoney(product.subtotal)}</td>
                                                </tr>
                                            )) : (
                                                <tr><td colSpan="4" className="sv-empty">Sin ventas en el periodo.</td></tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </>
                )}

                <div className="inventory-table-container sv-report-block">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Libro de ventas (IVA)</h2>
                            <span className="table-subtitle">· Las notas de crédito restan</span>
                        </div>
                        <div className="sv-actions">
                            <select className="form-input" value={ivaType} aria-label="Tipo de documento"
                                    onChange={e => setIvaType(e.target.value)}>
                                <option value="">Todos los tipos</option>
                                <option value="FACTURA">Factura</option>
                                <option value="CREDITO_FISCAL">Crédito Fiscal</option>
                                <option value="NOTA_CREDITO">Nota de Crédito</option>
                            </select>
                            <button className="btn-export" disabled={exporting} onClick={exportIvaCsv}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                                {exporting ? 'Descargando...' : 'Exportar CSV'}
                            </button>
                        </div>
                    </div>
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>FECHA</th>
                                <th>TIPO</th>
                                <th>NÚMERO DE CONTROL</th>
                                <th>CLIENTE</th>
                                <th>SUBTOTAL</th>
                                <th>IVA</th>
                                <th>TOTAL</th>
                                <th>ESTADO</th>
                            </tr>
                        </thead>
                        <tbody>
                            {ivaRows.length ? ivaRows.map(row => (
                                <tr key={row.generationCode}>
                                    <td>{formatWhen(row.issuedAt)}</td>
                                    <td><span className={'type-badge ' + row.type.toLowerCase()}>{row.type}</span></td>
                                    <td className="code-cell" title={row.generationCode}>{row.controlNumber}</td>
                                    <td>{row.customerName}</td>
                                    <td className="amount-cell">{formatMoney(row.subtotal)}</td>
                                    <td className="amount-cell">{formatMoney(row.tax)}</td>
                                    <td className="amount-cell">{formatMoney(row.total)}</td>
                                    <td><StatusBadge status={row.status} /></td>
                                </tr>
                            )) : (
                                <tr><td colSpan="8" className="sv-empty">No hay documentos en el periodo.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    );
}
