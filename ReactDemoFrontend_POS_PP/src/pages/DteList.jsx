import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { statusInfo } from '../constants';
import { formatDate, formatMoney, formatTime, formatWhen, isToday, normalize, splitMoney } from '../format';
import { StatusBadge, TypeBadge } from '../components/Badges';
import { useToast } from '../components/Toast';

const MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre',
    'Octubre', 'Noviembre', 'Diciembre'];
const FILTERS = [
    { status: '', label: 'Todos' },
    { status: 'SELLADO', label: 'Sellados' },
    { status: 'CONTINGENCIA', label: 'Contingencia' },
    { status: 'RECHAZADO', label: 'Rechazados' },
    { status: 'ANULADO', label: 'Anulados' }
];

/** Escapa un valor para CSV y evita inyección de fórmulas al abrirlo en Excel. */
function csvCell(value) {
    let text = String(value == null ? '' : value);
    if (/^[=+\-@\t\r]/.test(text)) text = "'" + text;
    return '"' + text.replace(/"/g, '""') + '"';
}

export default function DteList() {
    const toast = useToast();
    const [params, setParams] = useSearchParams();
    const statusFilter = params.get('status') || '';
    const [dtes, setDtes] = useState([]);
    const [error, setError] = useState(null);
    const [term, setTerm] = useState('');
    const [retryingId, setRetryingId] = useState(null);

    const load = useCallback(async () => {
        try {
            setDtes(await api('/api/dte/all'));
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

    const now = new Date();
    const today = dtes.filter(dte => isToday(dte.issuedAt));
    const pending = dtes.filter(dte => dte.status === 'CONTINGENCIA').length;
    const rejected = dtes.filter(dte => dte.status === 'RECHAZADO').length;
    const total = today.filter(dte => dte.status !== 'ANULADO').reduce((sum, dte) => sum + Number(dte.total), 0);
    const [integer, decimals] = splitMoney(total);

    const t = normalize(term);
    const visible = dtes.filter(dte =>
        (!statusFilter || dte.status === statusFilter) &&
        (!t || normalize(dte.generationCode).includes(t) || normalize(dte.customerName).includes(t)));

    async function retry(dteId) {
        setRetryingId(dteId);
        try {
            const dte = await api('/api/dte/retry?dteId=' + encodeURIComponent(dteId), { method: 'POST' });
            toast(dte.status === 'SELLADO'
                ? 'Documento sellado por Hacienda.'
                : 'Hacienda sigue sin responder. Se reintentará automáticamente.',
                dte.status === 'SELLADO' ? 'success' : 'warning');
        } catch (err) {
            toast(err.message, 'error');
        }
        setRetryingId(null);
        load();
    }

    function exportCsv() {
        if (!visible.length) {
            toast('No hay documentos para exportar.', 'warning');
            return;
        }
        const header = ['Fecha', 'Hora', 'Tipo', 'Código de generación', 'Venta', 'Cliente', 'NRC', 'Estado',
            'Subtotal', 'IVA', 'Total'];
        const lines = visible.map(dte => [
            formatDate(dte.issuedAt), formatTime(dte.issuedAt), dte.type, dte.generationCode, dte.saleNumber,
            dte.customerName, dte.customerNrc || '', statusInfo(dte.status).label,
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
    }

    return (
        <>
            <header className="dte-header">
                <div className="dte-header-left">
                    <h1>Documentos electrónicos</h1>
                    <p>Facturación · {MONTHS[now.getMonth()]} {now.getFullYear()}</p>
                </div>
                <div className="dte-header-right">
                    <div className="search-box">
                        <span className="search-icon">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8"></circle>
                                <path d="m21 21-4.35-4.35"></path>
                            </svg>
                        </span>
                        <input type="text" placeholder="Buscar por código de generación o cliente..." autoComplete="off"
                               value={term} onChange={event => setTerm(event.target.value)} />
                    </div>
                    <button className="notification-btn"
                            title={pending + rejected
                                ? pending + ' en contingencia, ' + rejected + ' rechazado(s)'
                                : 'Sin documentos pendientes'}
                            onClick={() => setParams(rejected ? { status: 'RECHAZADO' } : { status: 'CONTINGENCIA' })}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                        </svg>
                    </button>
                </div>
            </header>

            <div className="dte-content">
                {pending > 0 && (
                    <div className="contingency-banner">
                        <div className="contingency-icon">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                <line x1="12" y1="9" x2="12" y2="13"></line>
                                <line x1="12" y1="17" x2="12.01" y2="17"></line>
                            </svg>
                        </div>
                        <div className="contingency-text">
                            <strong>Modo contingencia activo · {pending} documento(s) en cola</strong>
                            <span>El servicio del Ministerio de Hacienda no responde. Las ventas continúan con normalidad;
                                los DTE se reintentan automáticamente.</span>
                        </div>
                        <button className="btn-retry" onClick={() => setParams({ status: 'CONTINGENCIA' })}>
                            Ver cola de reintentos</button>
                    </div>
                )}

                <div className="stats-grid">
                    <div className="stat-card">
                        <div className="stat-icon success">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </div>
                        <div className="stat-info">
                            <span className="stat-label">Sellados hoy</span>
                            <span className="stat-value">{today.filter(dte => dte.status === 'SELLADO').length}</span>
                            <span className="stat-subtitle">Con sello de Hacienda</span>
                        </div>
                        <div className="stat-bar success"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-icon warning">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 16 14"></polyline>
                            </svg>
                        </div>
                        <div className="stat-info">
                            <span className="stat-label">En contingencia</span>
                            <span className="stat-value warning">{pending}</span>
                            <span className="stat-subtitle">Reintento automático</span>
                        </div>
                        <div className="stat-bar warning"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-icon danger">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </div>
                        <div className="stat-info">
                            <span className="stat-label">Rechazados</span>
                            <span className="stat-value danger">{rejected}</span>
                            <span className="stat-subtitle">Requiere corrección</span>
                        </div>
                        <div className="stat-bar danger"></div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-icon primary">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="12" y1="1" x2="12" y2="23"></line>
                                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                            </svg>
                        </div>
                        <div className="stat-info">
                            <span className="stat-label">Total facturado</span>
                            <span className="stat-value">{integer}<span className="stat-decimal">{decimals}</span></span>
                            <span className="stat-subtitle">Hoy · IVA incluido</span>
                        </div>
                        <div className="stat-bar primary"></div>
                    </div>
                </div>

                <div className="dte-table-container">
                    <div className="dte-table-header">
                        <div className="filter-tabs">
                            {FILTERS.map(f => (
                                <button key={f.status}
                                        className={'filter-tab' + (statusFilter === f.status ? ' active' : '')}
                                        onClick={() => setParams(f.status ? { status: f.status } : {})}>{f.label}</button>
                            ))}
                        </div>
                        <button className="btn-export"
                                title="Descarga los documentos de la lista con subtotal, IVA y total (CSV)"
                                onClick={exportCsv}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="7 10 12 15 17 10"></polyline>
                                <line x1="12" y1="15" x2="12" y2="3"></line>
                            </svg>
                            Exportar IVA
                        </button>
                    </div>

                    <table className="dte-table">
                        <thead>
                            <tr>
                                <th>CÓDIGO DE GENERACIÓN</th>
                                <th>TIPO</th>
                                <th>CLIENTE</th>
                                <th>MONTO</th>
                                <th>ESTADO</th>
                                <th>HORA</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="7" className="sv-empty">{error}</td></tr>
                            ) : visible.length ? visible.map(dte => (
                                <tr key={dte.id}>
                                    <td className="code-cell" title={dte.generationCode}>
                                        <Link to={'/dte/' + dte.id}>{dte.generationCode.slice(0, 14)}...</Link>
                                    </td>
                                    <td><TypeBadge shortName={dte.type} /></td>
                                    <td>{dte.customerName}</td>
                                    <td className="amount-cell">{formatMoney(dte.total)}</td>
                                    <td><StatusBadge status={dte.status} /></td>
                                    <td>{formatWhen(dte.issuedAt)}</td>
                                    <td>
                                        {dte.status === 'CONTINGENCIA' ? (
                                            <button type="button" className="action-link retry"
                                                    disabled={retryingId === dte.id} onClick={() => retry(dte.id)}>
                                                {retryingId === dte.id ? 'Reintentando...' : 'Reintentar'}</button>
                                        ) : (
                                            <Link to={'/dte/' + dte.id}
                                                  className={'action-link' + (dte.status === 'RECHAZADO' ? ' correct' : '')}>
                                                {dte.status === 'RECHAZADO' ? 'Corregir' : 'Ver'}</Link>
                                        )}
                                    </td>
                                </tr>
                            )) : (
                                <tr><td colSpan="7" className="sv-empty">
                                    {dtes.length ? 'No hay documentos con este filtro.' : 'Aún no se han emitido documentos.'}
                                </td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    );
}
