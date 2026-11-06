import { useCallback, useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { api } from '../api';
import { formatMoney, formatTime, formatWhen, isToday, splitMoney } from '../format';
import { logout, minutesLeft } from '../session';
import { StatusBadge, TypeBadge } from '../components/Badges';
import { useToast } from '../components/Toast';

const REFRESH_MS = 30000;

const alertIcon = (
    <div className="alert-icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
        </svg>
    </div>
);
const dotsIcon = (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle>
    </svg>
);

export default function Dashboard() {
    const session = useOutletContext();
    const toast = useToast();
    const [dtes, setDtes] = useState([]);
    const [products, setProducts] = useState([]);
    const [dteError, setDteError] = useState(null);
    const [productError, setProductError] = useState(null);
    const [clock, setClock] = useState(new Date());
    const [retryingAll, setRetryingAll] = useState(false);

    const load = useCallback(async () => {
        const [dteResult, productResult] = await Promise.allSettled([api('/api/dte/all'), api('/api/product/all')]);
        if (dteResult.status === 'fulfilled') {
            setDtes(dteResult.value);
            setDteError(null);
        } else {
            setDteError(dteResult.reason.message);
        }
        if (productResult.status === 'fulfilled') {
            setProducts(productResult.value);
            setProductError(null);
        } else {
            setProductError(productResult.reason.message);
        }
    }, []);

    useEffect(() => {
        load();
        const interval = setInterval(load, REFRESH_MS);
        return () => clearInterval(interval);
    }, [load]);

    useEffect(() => {
        const interval = setInterval(() => setClock(new Date()), REFRESH_MS);
        return () => clearInterval(interval);
    }, []);

    const todaysDtes = dtes.filter(dte => isToday(dte.issuedAt) && dte.status !== 'ANULADO');
    const sealed = todaysDtes.filter(dte => dte.status === 'SELLADO').length;
    const pendingCount = todaysDtes.filter(dte => dte.status === 'CONTINGENCIA').length;
    const rejectedCount = todaysDtes.filter(dte => dte.status === 'RECHAZADO').length;
    const total = todaysDtes.reduce((sum, dte) => sum + Number(dte.total), 0);
    const [integer, decimals] = splitMoney(total);
    const pendingQueue = dtes.filter(dte => dte.status === 'CONTINGENCIA');
    const lowStock = products.filter(p => p.stock < p.minStock)
        .sort((a, b) => a.stock / a.minStock - b.stock / b.minStock);

    const date = clock.toLocaleDateString('es-SV', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    const time = clock.toLocaleTimeString('es-SV', { hour: 'numeric', minute: '2-digit' });

    function closeRegister() {
        const confirmed = window.confirm('¿Cerrar la caja y terminar el turno?\n\n' +
            'Ventas del turno: ' + todaysDtes.length + '\nTotal: ' + formatMoney(total));
        if (confirmed) logout();
    }

    async function retryAll() {
        if (!pendingQueue.length) return;
        setRetryingAll(true);
        let stamped = 0;
        try {
            for (const dte of pendingQueue) {
                const updated = await api('/api/dte/retry?dteId=' + encodeURIComponent(dte.id), { method: 'POST' });
                if (updated.status === 'SELLADO') stamped++;
            }
            toast(stamped
                ? stamped + ' documento(s) sellado(s) por Hacienda.'
                : 'Hacienda sigue sin responder. Se reintentará automáticamente.', stamped ? 'success' : 'warning');
        } catch (error) {
            toast(error.message, 'error');
        } finally {
            setRetryingAll(false);
            load();
        }
    }

    return (
        <>
            <header className="dashboard-header">
                <div className="header-left">
                    <h1>Dashboard</h1>
                    <p>Caja 02 · Turno mañana</p>
                </div>
                <div className="header-center">
                    <div className="session-status">
                        <span className="status-dot"></span>
                        <span>Sesión activa · expira en {minutesLeft(session)} min</span>
                    </div>
                </div>
                <div className="header-right">
                    <span className="current-date">{date.charAt(0).toUpperCase() + date.slice(1)} · {time}</span>
                    <Link to="/pos" className="btn-new-sale">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        Nueva venta
                    </Link>
                </div>
            </header>

            <div className="dashboard-content">
                <div className="dashboard-grid">
                    <div className="dashboard-left">
                        <div className="metrics-grid">
                            <div className="metric-card">
                                <div className="metric-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="9" cy="21" r="1"></circle>
                                        <circle cx="20" cy="21" r="1"></circle>
                                        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                                    </svg>
                                </div>
                                <div className="metric-info">
                                    <span className="metric-label">VENTAS DEL TURNO</span>
                                    <span className="metric-value">{todaysDtes.length}</span>
                                    <span className="metric-change">Ventas registradas hoy</span>
                                </div>
                            </div>
                            <div className="metric-card">
                                <div className="metric-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                        <polyline points="14 2 14 8 20 8"></polyline>
                                        <line x1="16" y1="13" x2="8" y2="13"></line>
                                        <line x1="16" y1="17" x2="8" y2="17"></line>
                                        <polyline points="10 9 9 9 8 9"></polyline>
                                    </svg>
                                </div>
                                <div className="metric-info">
                                    <span className="metric-label">DTE EMITIDOS</span>
                                    <span className="metric-value">{sealed}</span>
                                    <span className={'metric-change' + (pendingCount || rejectedCount ? ' warning' : ' positive')}>
                                        {pendingCount || rejectedCount
                                            ? [pendingCount && pendingCount + ' en contingencia',
                                               rejectedCount && rejectedCount + ' rechazado(s)'].filter(Boolean).join(' · ')
                                            : 'Todos sellados por Hacienda'}
                                    </span>
                                </div>
                            </div>
                            <div className="metric-card">
                                <div className="metric-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <line x1="12" y1="1" x2="12" y2="23"></line>
                                        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                                    </svg>
                                </div>
                                <div className="metric-info">
                                    <span className="metric-label">TOTAL FACTURADO</span>
                                    <span className="metric-value">{integer}<span className="metric-decimal">{decimals}</span></span>
                                    <span className="metric-change">IVA incluido (13%)</span>
                                </div>
                            </div>
                        </div>

                        <div className="card">
                            <div className="card-header">
                                <h2>Ventas recientes</h2>
                                <Link to="/dte" className="card-link">Ver todas →</Link>
                            </div>
                            <table className="sales-table">
                                <thead>
                                    <tr>
                                        <th>#</th>
                                        <th>Cliente</th>
                                        <th>Tipo DTE</th>
                                        <th>Total</th>
                                        <th>Estado</th>
                                        <th>Hora</th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {dteError ? (
                                        <tr><td colSpan="7" className="sv-empty">{dteError}</td></tr>
                                    ) : dtes.length ? dtes.slice(0, 5).map(dte => (
                                        <tr key={dte.id}>
                                            <td className="sale-number">{String(dte.saleNumber).padStart(4, '0')}</td>
                                            <td>{dte.customerName}</td>
                                            <td><TypeBadge shortName={dte.type} /></td>
                                            <td className="amount-cell">{formatMoney(dte.total)}</td>
                                            <td><StatusBadge status={dte.status} /></td>
                                            <td>{formatWhen(dte.issuedAt)}</td>
                                            <td>
                                                <Link to={'/dte/' + dte.id} className="action-icon" title="Ver documento">
                                                    {dotsIcon}</Link>
                                            </td>
                                        </tr>
                                    )) : (
                                        <tr><td colSpan="7" className="sv-empty">
                                            Aún no hay ventas. <Link to="/pos">Registra la primera</Link>.
                                        </td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        <div className="card">
                            <div className="card-header">
                                <h2>Avisos de inventario</h2>
                                <span className="alert-badge">{lowStock.length}</span>
                            </div>
                            <div className="stock-alerts">
                                {productError ? (
                                    <div className="sv-empty">{productError}</div>
                                ) : lowStock.length ? lowStock.slice(0, 5).map(p => (
                                    <div className="stock-alert-item" key={p.id}>
                                        {alertIcon}
                                        <div className="alert-info">
                                            <span className="alert-name">{p.name}</span>
                                            <span className="alert-code">{p.code}</span>
                                        </div>
                                        <div className="alert-stock">Stock: <strong>{p.stock}</strong> · Mín: {p.minStock}</div>
                                    </div>
                                )) : (
                                    <div className="sv-empty">Todo el inventario está sobre el mínimo.</div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="dashboard-right">
                        <div className="card cash-register-card">
                            <span className="cash-register-label">CAJA ACTIVA</span>
                            <h3 className="cash-register-name">Caja 02</h3>
                            <p className="cash-register-subtitle">Turno actual · {session.fullName}</p>
                            <div className="cash-register-divider"></div>
                            <div className="cash-register-info">
                                <div className="cash-register-row">
                                    <span>Apertura</span>
                                    <span>{new Date(session.loginAt)
                                        .toLocaleTimeString('es-SV', { hour: 'numeric', minute: '2-digit' })}</span>
                                </div>
                                <div className="cash-register-row">
                                    <span>Ventas en turno</span>
                                    <span>{todaysDtes.length}</span>
                                </div>
                            </div>
                            <button className="btn-close-register" onClick={closeRegister}>Cerrar caja</button>
                        </div>

                        <div className="card contingency-card">
                            <div className="contingency-header">
                                <div className="contingency-title">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"></path>
                                        <line x1="16" y1="8" x2="20" y2="8"></line>
                                        <line x1="18" y1="6" x2="18" y2="10"></line>
                                    </svg>
                                    <span>Cola de contingencia</span>
                                </div>
                                <span className="contingency-badge">{pendingQueue.length}</span>
                            </div>
                            <p className="contingency-text">
                                {dteError || (pendingQueue.length
                                    ? pendingQueue.length + ' documento(s) pendiente(s) de transmisión. El sistema reintentará automáticamente.'
                                    : 'No hay documentos pendientes: todos fueron transmitidos a Hacienda.')}
                            </p>
                            <div>
                                {pendingQueue.map(dte => (
                                    <Link className="contingency-item" to={'/dte/' + dte.id} key={dte.id}>
                                        <div className="contingency-item-info">
                                            <span className="contingency-item-number">#{String(dte.saleNumber).padStart(4, '0')}</span>
                                            <span className="contingency-item-client">{dte.customerName}</span>
                                        </div>
                                        <div className="contingency-item-details">
                                            <span>{formatMoney(dte.total)}</span>
                                            <span>· En cola desde {formatTime(dte.issuedAt)}</span>
                                        </div>
                                    </Link>
                                ))}
                            </div>
                            {pendingQueue.length > 0 && (
                                <button className="btn-retry-now" disabled={retryingAll} onClick={retryAll}>
                                    {retryingAll ? 'Reintentando...' : 'Reintentar ahora'}</button>
                            )}
                        </div>

                        <div className="card quick-access-card">
                            <h2>Accesos rápidos</h2>
                            <div className="quick-access-grid">
                                <Link to="/pos" className="quick-access-tile">
                                    <div className="tile-icon">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <circle cx="9" cy="21" r="1"></circle>
                                            <circle cx="20" cy="21" r="1"></circle>
                                            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                                        </svg>
                                    </div>
                                    <span>Nueva venta</span>
                                </Link>
                                <Link to="/pos?cliente=1" className="quick-access-tile">
                                    <div className="tile-icon">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="12" cy="7" r="4"></circle>
                                        </svg>
                                    </div>
                                    <span>Buscar cliente</span>
                                </Link>
                                <Link to="/dte" className="quick-access-tile">
                                    <div className="tile-icon">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                            <polyline points="14 2 14 8 20 8"></polyline>
                                            <line x1="16" y1="13" x2="8" y2="13"></line>
                                            <line x1="16" y1="17" x2="8" y2="17"></line>
                                        </svg>
                                    </div>
                                    <span>Mis DTEs</span>
                                </Link>
                                <Link to="/inventario" className="quick-access-tile">
                                    <div className="tile-icon">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                                            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                                            <line x1="12" y1="22.08" x2="12" y2="12"></line>
                                        </svg>
                                    </div>
                                    <span>Consultar stock</span>
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
