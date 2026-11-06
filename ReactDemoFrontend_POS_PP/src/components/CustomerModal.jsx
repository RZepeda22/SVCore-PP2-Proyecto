import { useState } from 'react';
import Modal from './Modal';
import { api } from '../api';

/** Formulario de cliente: registra uno nuevo o edita uno existente (customer.id). */
export default function CustomerModal({ customer, title, submitLabel, onSaved, onClose }) {
    const c = customer || {};
    const [taxpayerType, setTaxpayerType] = useState(c.taxpayerType || 'CONTRIBUYENTE');

    async function handleSubmit(values) {
        const payload = {
            name: values.name,
            taxpayerType: values.taxpayerType,
            nrc: values.nrc || null,
            nit: values.nit || null,
            email: values.email || null,
            address: values.address || null
        };
        const saved = c.id
            ? await api('/api/customer/edit?customerId=' + encodeURIComponent(c.id), { method: 'PATCH', body: payload })
            : await api('/api/customer/save', { method: 'POST', body: payload });
        await onSaved(saved);
    }

    return (
        <Modal title={title} submitLabel={submitLabel || 'Guardar cliente'} onSubmit={handleSubmit} onClose={onClose}>
            <div className="form-group">
                <label htmlFor="cm-name">Nombre o razón social</label>
                <input id="cm-name" name="name" className="form-input" defaultValue={c.name || ''}
                       maxLength={150} required />
            </div>
            <div className="form-group">
                <label htmlFor="cm-type">Tipo de cliente</label>
                <select id="cm-type" name="taxpayerType" className="form-input" value={taxpayerType}
                        onChange={event => setTaxpayerType(event.target.value)}>
                    <option value="CONTRIBUYENTE">Contribuyente (recibe Crédito Fiscal)</option>
                    <option value="NO_CONTRIBUYENTE">No contribuyente (solo Factura)</option>
                </select>
            </div>
            <div className="sv-form-row">
                <div className="form-group">
                    <label htmlFor="cm-nrc">NRC</label>
                    <input id="cm-nrc" name="nrc" className="form-input" defaultValue={c.nrc || ''}
                           placeholder="000000-0" maxLength={10} disabled={taxpayerType !== 'CONTRIBUYENTE'} />
                </div>
                <div className="form-group">
                    <label htmlFor="cm-nit">NIT</label>
                    <input id="cm-nit" name="nit" className="form-input" defaultValue={c.nit || ''}
                           placeholder="0000-000000-000-0" maxLength={17} />
                </div>
            </div>
            <div className="form-group">
                <label htmlFor="cm-email">Correo electrónico</label>
                <input id="cm-email" name="email" type="email" className="form-input" defaultValue={c.email || ''}
                       maxLength={100} />
            </div>
            <div className="form-group">
                <label htmlFor="cm-address">Dirección</label>
                <input id="cm-address" name="address" className="form-input" defaultValue={c.address || ''}
                       maxLength={200} />
            </div>
        </Modal>
    );
}
