import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { api } from '../api';
import { TAXPAYER } from '../constants';
import { initials, normalize, orDash } from '../format';
import { hasRole } from '../session';
import CustomerModal from '../components/CustomerModal';
import { useToast } from '../components/Toast';

export default function Clientes() {
    const session = useOutletContext();
    const toast = useToast();
    const canEdit = hasRole(session, ['VEND', 'ADMN']);

    const [customers, setCustomers] = useState([]);
    const [error, setError] = useState(null);
    const [term, setTerm] = useState('');
    const [modal, setModal] = useState(null); // { customer } o {} para crear

    const load = useCallback(async () => {
        try {
            setCustomers(await api('/api/customer/all'));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    const t = normalize(term);
    const rows = customers.filter(c =>
        !t || normalize(c.name).includes(t) || normalize(c.nrc).includes(t) || normalize(c.nit).includes(t));

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Clientes</h1>
                    <p>CU-08 · La clasificación fiscal determina el tipo de DTE que puede recibir</p>
                </div>
                <div className="header-right">
                    <div className="search-box">
                        <span className="search-icon">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8"></circle>
                                <path d="m21 21-4.35-4.35"></path>
                            </svg>
                        </span>
                        <input type="text" placeholder="Buscar por nombre, NRC o NIT..." autoComplete="off"
                               value={term} onChange={event => setTerm(event.target.value)} />
                    </div>
                    {canEdit && (
                        <button className="btn-add-entry" onClick={() => setModal({})}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="12" y1="5" x2="12" y2="19"></line>
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                            Nuevo cliente
                        </button>
                    )}
                </div>
            </header>

            <div className="inventory-content">
                <div className="inventory-table-container">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Directorio de clientes</h2>
                            <span className="table-subtitle">· {rows.length} de {customers.length}</span>
                        </div>
                    </div>
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>NOMBRE / RAZÓN SOCIAL</th>
                                <th>CLASIFICACIÓN</th>
                                <th>NRC</th>
                                <th>NIT</th>
                                <th>CORREO</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="6" className="sv-empty">{error}</td></tr>
                            ) : rows.length ? rows.map(customer => (
                                <tr key={customer.id}>
                                    <td>
                                        <div className="sv-user-cell">
                                            <div className="dropdown-avatar">{initials(customer.name)}</div>
                                            <div>
                                                <div className="product-name-cell">{customer.name}</div>
                                                <div className="code-cell">{orDash(customer.address)}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span className={'status-badge '
                                            + (customer.taxpayerType === 'CONTRIBUYENTE' ? 'success' : 'neutral')}>
                                            {TAXPAYER[customer.taxpayerType] || customer.taxpayerType}</span>
                                    </td>
                                    <td className="code-cell">{orDash(customer.nrc)}</td>
                                    <td className="code-cell">{orDash(customer.nit)}</td>
                                    <td>{orDash(customer.email)}</td>
                                    <td>
                                        {canEdit && (
                                            <button type="button" className="action-link"
                                                    onClick={() => setModal({ customer })}>Editar</button>
                                        )}
                                    </td>
                                </tr>
                            )) : (
                                <tr><td colSpan="6" className="sv-empty">
                                    {customers.length ? 'No hay clientes que coincidan con la búsqueda.' : 'Cargando clientes...'}
                                </td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {modal && (
                <CustomerModal
                    title={modal.customer ? 'Editar cliente' : 'Registrar nuevo cliente'}
                    customer={modal.customer}
                    onClose={() => setModal(null)}
                    onSaved={saved => {
                        toast('Cliente ' + saved.name + (modal.customer ? ' actualizado.' : ' registrado.'), 'success');
                        load();
                    }}
                />
            )}
        </>
    );
}
