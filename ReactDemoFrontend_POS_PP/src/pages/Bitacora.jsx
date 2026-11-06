import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { AUDIT_ACTIONS } from '../constants';
import { formatDateTime } from '../format';
import { useToast } from '../components/Toast';

const PAGE_SIZE = 50;

export default function Bitacora() {
    const toast = useToast();
    const [filters, setFilters] = useState({ username: '', action: '', from: '', to: '' });
    const [applied, setApplied] = useState(filters);
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [verifying, setVerifying] = useState(false);
    const [verification, setVerification] = useState(null);

    const load = useCallback(async () => {
        const query = new URLSearchParams();
        if (applied.username) query.set('username', applied.username);
        if (applied.action) query.set('action', applied.action);
        if (applied.from) query.set('from', applied.from);
        if (applied.to) query.set('to', applied.to);
        query.set('page', String(page));
        query.set('size', String(PAGE_SIZE));
        try {
            setData(await api('/api/audit/all?' + query.toString()));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, [applied, page]);

    useEffect(() => { load(); }, [load]);

    async function verify() {
        setVerifying(true);
        try {
            setVerification(await api('/api/audit/verify'));
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setVerifying(false);
        }
    }

    function applyFilters(event) {
        event.preventDefault();
        setPage(0);
        setApplied(filters);
    }

    const rows = data ? data.content : [];

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Bitácora del sistema</h1>
                    <p>RNF-06 · Registros inmodificables con cadena de hashes SHA-256</p>
                </div>
                <div className="header-right">
                    <button className="btn-add-entry" disabled={verifying} onClick={verify}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                        </svg>
                        {verifying ? 'Verificando...' : 'Verificar integridad'}
                    </button>
                </div>
            </header>

            <div className="inventory-content">
                {verification && (
                    <div className={'sv-notice ' + (verification.valid ? 'success' : 'danger')}>
                        <strong>{verification.valid ? 'Bitácora íntegra' : 'Se detectaron alteraciones'}</strong>
                        <span>{verification.message} · Registros revisados: {verification.checkedEntries}
                            {verification.firstInvalidId ? ' · Primer registro alterado: #' + verification.firstInvalidId : ''}</span>
                    </div>
                )}

                <div className="inventory-table-container">
                    <form className="sv-filter-bar" onSubmit={applyFilters}>
                        <div className="form-group">
                            <label htmlFor="bf-username">Usuario</label>
                            <input id="bf-username" className="form-input" placeholder="usuario" value={filters.username}
                                   onChange={e => setFilters({ ...filters, username: e.target.value })} />
                        </div>
                        <div className="form-group">
                            <label htmlFor="bf-action">Acción</label>
                            <select id="bf-action" className="form-input" value={filters.action}
                                    onChange={e => setFilters({ ...filters, action: e.target.value })}>
                                <option value="">Todas</option>
                                {AUDIT_ACTIONS.map(action => <option key={action} value={action}>{action}</option>)}
                            </select>
                        </div>
                        <div className="form-group">
                            <label htmlFor="bf-from">Desde</label>
                            <input id="bf-from" type="date" className="form-input" value={filters.from}
                                   onChange={e => setFilters({ ...filters, from: e.target.value })} />
                        </div>
                        <div className="form-group">
                            <label htmlFor="bf-to">Hasta</label>
                            <input id="bf-to" type="date" className="form-input" value={filters.to}
                                   onChange={e => setFilters({ ...filters, to: e.target.value })} />
                        </div>
                        <button type="submit" className="sv-btn sv-btn-primary">Filtrar</button>
                    </form>

                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>FECHA Y HORA</th>
                                <th>USUARIO</th>
                                <th>ACCIÓN</th>
                                <th>ENTIDAD</th>
                                <th>DETALLE</th>
                                <th>IP</th>
                                <th>HASH</th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="7" className="sv-empty">{error}</td></tr>
                            ) : rows.length ? rows.map(entry => (
                                <tr key={entry.id}>
                                    <td>{formatDateTime(entry.occurredAt)}</td>
                                    <td className="code-cell">{entry.username}</td>
                                    <td><span className="type-badge">{entry.action}</span></td>
                                    <td>{entry.entity || '—'}</td>
                                    <td>{entry.detail || '—'}</td>
                                    <td className="code-cell">{entry.ipAddress || '—'}</td>
                                    <td className="code-cell" title={'Anterior: ' + entry.previousHash}>
                                        {entry.hash.slice(0, 12)}…</td>
                                </tr>
                            )) : (
                                <tr><td colSpan="7" className="sv-empty">
                                    {data ? 'No hay registros con estos filtros.' : 'Cargando bitácora...'}</td></tr>
                            )}
                        </tbody>
                    </table>

                    {data && (
                        <div className="sv-pagination">
                            <span>Página {data.page + 1} de {Math.max(data.totalPages, 1)} · {data.totalElements} registros</span>
                            <div className="sv-actions">
                                <button type="button" className="sv-btn sv-btn-secondary" disabled={page === 0}
                                        onClick={() => setPage(p => p - 1)}>Anterior</button>
                                <button type="button" className="sv-btn sv-btn-secondary"
                                        disabled={page + 1 >= data.totalPages}
                                        onClick={() => setPage(p => p + 1)}>Siguiente</button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
