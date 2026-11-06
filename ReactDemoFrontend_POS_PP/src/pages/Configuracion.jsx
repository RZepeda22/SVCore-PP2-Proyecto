import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { formatDateTime } from '../format';
import { useToast } from '../components/Toast';

const FIELDS = [
    { name: 'businessName', label: 'Razón social', maxLength: 250 },
    { name: 'tradeName', label: 'Nombre comercial', maxLength: 150 },
    { name: 'nit', label: 'NIT', placeholder: '0000-000000-000-0' },
    { name: 'nrc', label: 'NRC', placeholder: '000000-0' },
    { name: 'economicActivityCode', label: 'Código de actividad económica', placeholder: '5 o 6 dígitos' },
    { name: 'economicActivity', label: 'Actividad económica', maxLength: 150 },
    { name: 'establishmentName', label: 'Nombre del establecimiento', maxLength: 100 },
    { name: 'establishmentCode', label: 'Código de establecimiento', placeholder: '4 caracteres, ej. M001' },
    { name: 'pointOfSaleCode', label: 'Código de punto de venta', placeholder: '4 caracteres, ej. P002' },
    { name: 'cashRegister', label: 'Caja', maxLength: 50 },
    { name: 'departmentCode', label: 'Código de departamento', placeholder: '2 dígitos' },
    { name: 'municipalityCode', label: 'Código de municipio', placeholder: '2 dígitos' },
    { name: 'addressDetail', label: 'Dirección', maxLength: 200, wide: true },
    { name: 'phone', label: 'Teléfono' },
    { name: 'email', label: 'Correo electrónico', type: 'email' }
];

export default function Configuracion() {
    const toast = useToast();
    const [config, setConfig] = useState(null);
    const [error, setError] = useState(null);
    const [saving, setSaving] = useState(false);
    const [available, setAvailable] = useState(null);
    const [toggling, setToggling] = useState(false);

    const load = useCallback(async () => {
        try {
            setConfig(await api('/api/config/emitter'));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
        try {
            const simulator = await api('/api/config/hacienda-simulator');
            setAvailable(simulator.available);
        } catch (err) {
            setAvailable(null); // modo api: la simulación no está activa
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    async function save(event) {
        event.preventDefault();
        const values = {};
        new FormData(event.target).forEach((value, key) => { values[key] = String(value).trim(); });
        setSaving(true);
        try {
            await api('/api/config/emitter', { method: 'PUT', body: { ...config, ...values } });
            toast('Configuración fiscal actualizada.', 'success');
            load();
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setSaving(false);
        }
    }

    async function toggleSimulator() {
        setToggling(true);
        try {
            const result = await api('/api/config/hacienda-simulator?available=' + !available, { method: 'PUT' });
            setAvailable(result.available);
            toast(result.available
                ? 'Hacienda (simulada) disponible: los DTE se sellan al emitirse.'
                : 'Hacienda (simulada) fuera de servicio: las próximas ventas entrarán en contingencia.',
                result.available ? 'success' : 'warning');
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setToggling(false);
        }
    }

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Configuración de facturación</h1>
                    <p>CU-09 · Datos fiscales del emisor y ambiente de transmisión</p>
                </div>
            </header>

            <div className="inventory-content">
                {error && <div className="sv-notice danger">{error}</div>}

                {config && (
                    <form className="inventory-table-container sv-config-form" onSubmit={save}>
                        <div className="table-header">
                            <div className="table-title">
                                <h2>Emisor</h2>
                                <span className="table-subtitle">
                                    · Última actualización: {formatDateTime(config.updatedAt)}</span>
                            </div>
                            <div className="sv-actions">
                                <label htmlFor="cf-environment">Ambiente</label>
                                <select id="cf-environment" name="environment" className="form-input"
                                        defaultValue={config.environment} key={config.environment}>
                                    <option value="00">00 · Pruebas</option>
                                    <option value="01">01 · Producción</option>
                                </select>
                            </div>
                        </div>
                        <div className="sv-config-grid">
                            {FIELDS.map(field => (
                                <div className="form-group" key={field.name}
                                     style={field.wide ? { gridColumn: '1 / -1' } : undefined}>
                                    <label htmlFor={'cf-' + field.name}>{field.label}</label>
                                    <input id={'cf-' + field.name} name={field.name} className="form-input" required
                                           type={field.type || 'text'} placeholder={field.placeholder || ''}
                                           maxLength={field.maxLength} defaultValue={config[field.name] || ''} />
                                </div>
                            ))}
                            <div className="form-group">
                                <label htmlFor="cf-establishmentType">Tipo de establecimiento</label>
                                <select id="cf-establishmentType" name="establishmentType" className="form-input"
                                        defaultValue={config.establishmentType} key={config.establishmentType}>
                                    <option value="01">01 · Sucursal / Agencia</option>
                                    <option value="02">02 · Casa matriz</option>
                                    <option value="04">04 · Bodega</option>
                                    <option value="07">07 · Predio y/o patio</option>
                                    <option value="20">20 · Otro</option>
                                </select>
                            </div>
                        </div>
                        <div className="sv-modal-footer">
                            <button type="submit" className="sv-btn sv-btn-primary" disabled={saving}>
                                {saving ? 'Guardando...' : 'Guardar configuración'}</button>
                        </div>
                    </form>
                )}

                <div className="inventory-table-container sv-report-block">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Simulación de Hacienda</h2>
                            <span className="table-subtitle">· Solo disponible con HACIENDA_MODE=mock</span>
                        </div>
                        {available !== null && (
                            <span className={'status-badge ' + (available ? 'success' : 'danger')}>
                                {available ? 'Disponible' : 'Fuera de servicio'}</span>
                        )}
                    </div>
                    {available === null ? (
                        <div className="sv-empty">La simulación no está activa: la API transmite contra el servicio configurado.</div>
                    ) : (
                        <div className="sv-simulator">
                            <p>Apaga la simulación para demostrar la contingencia: las ventas siguen registrándose y los DTE
                                quedan en cola hasta que el servicio se restablezca.</p>
                            <button type="button"
                                    className={'sv-btn ' + (available ? 'sv-btn-secondary' : 'sv-btn-primary')}
                                    disabled={toggling} onClick={toggleSimulator}>
                                {available ? 'Apagar simulación de Hacienda' : 'Restablecer servicio de Hacienda'}</button>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
