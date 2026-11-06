import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { INVALIDATION_TYPES } from '../constants';
import { formatMoney, formatWhen } from '../format';
import { TypeBadge } from '../components/Badges';
import Modal from '../components/Modal';
import { useToast } from '../components/Toast';

export default function Anulacion() {
    const toast = useToast();
    const [dtes, setDtes] = useState([]);
    const [error, setError] = useState(null);
    const [selected, setSelected] = useState(null);

    const load = useCallback(async () => {
        try {
            setDtes(await api('/api/dte/all?status=SELLADO'));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Anulación de DTE</h1>
                    <p>CU-04 · Invalidación ante Hacienda con reingreso automático del inventario</p>
                </div>
            </header>

            <div className="inventory-content">
                <div className="sv-notice warning">
                    <strong>Acción irreversible</strong>
                    <span>Solo se pueden anular documentos sellados, sin invalidación previa y sin notas de crédito vigentes.
                        Requiere conexión con Hacienda: sin comunicación no es posible invalidar. Al aceptarse, las unidades
                        vendidas regresan al inventario.</span>
                </div>

                <div className="inventory-table-container">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Documentos sellados</h2>
                            <span className="table-subtitle">· Elegibles para invalidación</span>
                        </div>
                    </div>
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>NÚMERO DE CONTROL / CÓDIGO</th>
                                <th>TIPO</th>
                                <th>CLIENTE</th>
                                <th>TOTAL</th>
                                <th>EMITIDO</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="6" className="sv-empty">{error}</td></tr>
                            ) : dtes.length ? dtes.map(dte => (
                                <tr key={dte.id}>
                                    <td className="code-cell" title={dte.generationCode}>
                                        <Link to={'/dte/' + dte.id}>{dte.generationCode.slice(0, 14)}...</Link>
                                    </td>
                                    <td><TypeBadge shortName={dte.type} /></td>
                                    <td>{dte.customerName}</td>
                                    <td className="amount-cell">{formatMoney(dte.total)}</td>
                                    <td>{formatWhen(dte.issuedAt)}</td>
                                    <td>
                                        <button type="button" className="action-link correct"
                                                onClick={() => setSelected(dte)}>Anular</button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td colSpan="6" className="sv-empty">No hay documentos sellados para anular.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {selected && (
                <Modal
                    title="Invalidar documento ante Hacienda"
                    submitLabel="Anular documento"
                    onClose={() => setSelected(null)}
                    onSubmit={async values => {
                        await api('/api/dte/invalidate', {
                            method: 'POST',
                            body: {
                                dteId: selected.id,
                                invalidationType: Number(values.invalidationType),
                                reason: values.reason
                            }
                        });
                        toast('Documento invalidado. El inventario fue reincorporado.', 'success');
                        load();
                    }}
                >
                    <div className="sv-notice warning">
                        <strong>{selected.type} · {formatMoney(selected.total)}</strong>
                        <span>{selected.customerName}</span>
                        <span className="code-cell">{selected.generationCode}</span>
                    </div>
                    <div className="form-group">
                        <label htmlFor="an-type">Tipo de invalidación</label>
                        <select id="an-type" name="invalidationType" className="form-input">
                            {INVALIDATION_TYPES.map(type => (
                                <option key={type.value} value={type.value}>{type.label}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label htmlFor="an-reason">Motivo de la anulación</label>
                        <textarea id="an-reason" name="reason" className="form-input" rows="3" required maxLength={250}
                                  placeholder="Describe por qué se anula el documento"></textarea>
                    </div>
                </Modal>
            )}
        </>
    );
}
