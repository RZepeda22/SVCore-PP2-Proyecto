import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { DOC_NAMES, TAXPAYER } from '../constants';
import { formatDateTime, formatMoney, formatTime, orDash, parseDate } from '../format';
import { useToast } from '../components/Toast';
import CustomerModal from '../components/CustomerModal';

const FEMININE = ['FACTURA', 'NOTA_CREDITO', 'NOTA_DEBITO'];
const REJECTION_TITLES = { '003': 'Receptor inválido' };

/** Enlace de verificación del portal de consulta pública de Hacienda (confirmar formato con la normativa). */
function verificationUrl(dte) {
    const d = parseDate(dte.issuedAt);
    const date = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    return 'https://admin.factura.gob.sv/consultaPublica?ambiente=' + encodeURIComponent(dte.environment) +
        '&codGen=' + encodeURIComponent(dte.generationCode) + '&fechaEmi=' + date;
}

function InfoItem({ label, value, className }) {
    return (
        <div className="info-item">
            <span className="info-label">{label}</span>
            <span className={'info-value' + (className ? ' ' + className : '')}>{orDash(value)}</span>
        </div>
    );
}

export default function DteResult() {
    const { id } = useParams();
    const [params] = useSearchParams();
    const toast = useToast();
    const [dte, setDte] = useState(null);
    const [error, setError] = useState(null);
    const [retrying, setRetrying] = useState(false);
    const [customerModal, setCustomerModal] = useState(null); // 'retry' | 'edit'
    const [receiverDraft, setReceiverDraft] = useState(null);
    const toastShown = useRef(false);

    const load = useCallback(async () => {
        try {
            const data = await api('/api/dte/?dteId=' + encodeURIComponent(id));
            setDte(data);
            setError(null);
            setReceiverDraft(current => current || {
                id: data.receiver.customerId, name: data.receiver.name, taxpayerType: data.receiver.taxpayerType,
                nrc: data.receiver.nrc, nit: data.receiver.nit, email: data.receiver.email, address: data.receiver.address
            });
        } catch (err) {
            setError(err.message);
        }
    }, [id]);

    useEffect(() => { load(); }, [load]);

    // La cola de reintentos trabaja en segundo plano: se consulta el estado cada 10 segundos
    useEffect(() => {
        if (!dte || dte.status !== 'CONTINGENCIA') return undefined;
        const interval = setInterval(load, 10000);
        return () => clearInterval(interval);
    }, [dte && dte.status, load]);

    useEffect(() => {
        if (!dte || toastShown.current) return;
        toastShown.current = true;
        if (params.get('desde') === 'contingencia' && dte.status === 'SELLADO') {
            toast('El documento salió de contingencia y ya tiene sello de Hacienda.', 'success');
        }
        if (params.get('desde') === 'rechazado' && dte.status === 'SELLADO') {
            toast('Datos corregidos: el documento fue sellado por Hacienda.', 'success');
        }
    }, [dte, params, toast]);

    async function retry() {
        return api('/api/dte/retry?dteId=' + encodeURIComponent(id), { method: 'POST' });
    }

    async function retryNow() {
        setRetrying(true);
        try {
            const updated = await retry();
            if (updated.status === 'CONTINGENCIA') {
                toast('Hacienda sigue sin responder. El sistema seguirá reintentando automáticamente.', 'warning');
            }
            setDte(updated);
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            setRetrying(false);
        }
    }

    function sendEmail() {
        if (!dte.receiver.email) {
            toast('El cliente no tiene un correo registrado.', 'warning');
            return;
        }
        const body = [
            'Estimado cliente:',
            '',
            'Le compartimos los datos de su ' + dte.typeLabel + ':',
            '',
            'Código de generación: ' + dte.generationCode,
            'Número de control: ' + dte.controlNumber,
            'Sello de recepción: ' + dte.receptionStamp,
            'Total: ' + formatMoney(dte.total),
            '',
            'Puede verificarlo en: ' + verificationUrl(dte),
            '',
            dte.emitter.tradeName
        ].join('\n');
        window.location.href = 'mailto:' + encodeURIComponent(dte.receiver.email) +
            '?subject=' + encodeURIComponent(dte.typeLabel + ' ' + dte.controlNumber) +
            '&body=' + encodeURIComponent(body);
    }

    const header = (
        <header className="dte-emitido-header">
            <div className="header-left">
                <h1>Resultado de la venta</h1>
                <p>Punto de venta · Emisión de DTE</p>
            </div>
            <Link to="/pos" className="btn-new-sale">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                Nueva venta
            </Link>
        </header>
    );

    if (error || !dte) {
        return (
            <>
                {header}
                <div className="dte-emitido-content">
                    <div className="sv-empty">
                        {error || 'Cargando documento...'}<br />
                        <Link to="/dte">Ver documentos emitidos</Link>
                    </div>
                </div>
            </>
        );
    }

    const canEditReceiver = !!dte.receiver.customerId;
    const newSaleIcon = (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
    );

    return (
        <>
            {header}
            <div className="dte-emitido-content">
                {dte.status === 'SELLADO' || dte.status === 'ANULADO' ? (
                    <div className="success-banner">
                        <div className="success-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                        </div>
                        <div className="success-text">
                            <h2>{dte.typeLabel} {FEMININE.includes(dte.type) ? 'emitida' : 'emitido'}</h2>
                            <p>{dte.status === 'ANULADO'
                                ? 'Este documento fue invalidado ante el Ministerio de Hacienda.'
                                : 'El documento fue firmado, transmitido y sellado por el Ministerio de Hacienda.'}</p>
                        </div>
                        <div className="success-seal">
                            <span className="seal-label">{dte.status === 'ANULADO' ? 'DOCUMENTO ANULADO' : 'SELLO RECIBIDO'}</span>
                            <span className="seal-date">{formatDateTime(dte.stampedAt)}</span>
                        </div>
                    </div>
                ) : dte.status === 'RECHAZADO' ? (
                    <div className="error-banner">
                        <div className="error-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </div>
                        <div className="error-text">
                            <h2>El documento fue rechazado por Hacienda</h2>
                            <p>La venta quedó registrada, pero el DTE no se selló. Corrige los datos indicados y reintenta
                                la transmisión.</p>
                        </div>
                        <div className="error-seal">
                            <span className="seal-label">INTENTO</span>
                            <span className="seal-date">{formatDateTime(dte.lastAttemptAt || dte.issuedAt)}</span>
                        </div>
                    </div>
                ) : (
                    <div className="warning-banner">
                        <div className="warning-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 16 14"></polyline>
                            </svg>
                        </div>
                        <div className="warning-text">
                            <h2>Venta registrada · DTE en cola de contingencia</h2>
                            <p>El servicio del Ministerio de Hacienda no está disponible. El documento se transmitirá
                                automáticamente cuando el servicio se restablezca. No se requiere ninguna acción.</p>
                        </div>
                        <div className="warning-seal">
                            <span className="seal-label">GUARDADO EN CONTINGENCIA</span>
                            <span className="seal-date">{formatDateTime(dte.issuedAt)}</span>
                        </div>
                    </div>
                )}

                <div className="dte-emitido-grid">
                    <div className="document-card">
                        <div className="document-header">
                            <span className="document-type-badge">{dte.typeLabel}</span>
                            {dte.status === 'RECHAZADO' ? (
                                <span className="rejected-badge">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                        <line x1="12" y1="9" x2="12" y2="13"></line>
                                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                    </svg>
                                    No sellado
                                </span>
                            ) : dte.status === 'CONTINGENCIA' ? (
                                <span className="pending-badge">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <polyline points="12 6 12 12 16 14"></polyline>
                                    </svg>
                                    Pendiente de sello
                                </span>
                            ) : (
                                <span className="signed-badge">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                                    </svg>
                                    Firmado electrónicamente
                                </span>
                            )}
                        </div>

                        {dte.status === 'RECHAZADO' && (
                            <div className="rejection-reason">
                                <div className="rejection-icon">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="8" x2="12" y2="12"></line>
                                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                                    </svg>
                                </div>
                                <div className="rejection-text">
                                    <strong>MOTIVO DEVUELTO POR EL MINISTERIO DE HACIENDA</strong>
                                    <span className="rejection-code">
                                        Código {dte.rejectionCode} · {REJECTION_TITLES[dte.rejectionCode] || 'Validación rechazada'}
                                    </span>
                                    <p>{dte.rejectionMessage}</p>
                                </div>
                            </div>
                        )}

                        {dte.status === 'CONTINGENCIA' && (
                            <div className="transmission-status">
                                <div className="transmission-icon">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="8" x2="12" y2="12"></line>
                                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                                    </svg>
                                </div>
                                <div className="transmission-text">
                                    <strong>ESTADO DE LA TRANSMISIÓN</strong>
                                    <p>La venta y el documento ya están guardados de forma segura. El sistema reintentará
                                        el envío en segundo plano, sin interrumpir la atención en caja. Podrás descargar o
                                        reenviar el comprobante una vez que Hacienda devuelva el sello.</p>
                                </div>
                            </div>
                        )}

                        <div className="document-section">
                            <h3>IDENTIFICACIÓN DEL DOCUMENTO</h3>
                            <div className="info-grid">
                                <InfoItem label="TIPO" value={DOC_NAMES[dte.type] || dte.typeLabel} />
                                <InfoItem label="NÚMERO DE CONTROL" value={dte.controlNumber} />
                                {dte.status === 'SELLADO' || dte.status === 'ANULADO' ? (
                                    <InfoItem label="AMBIENTE" value={dte.environment === '01' ? 'Producción' : 'Pruebas'}
                                              className={dte.environment === '01' ? 'production' : ''} />
                                ) : (
                                    <InfoItem label="ESTADO" value={dte.status === 'RECHAZADO' ? 'Rechazado' : 'Pendiente de sello'}
                                              className={dte.status === 'RECHAZADO' ? 'danger' : 'warning'} />
                                )}
                                <InfoItem label="CÓDIGO DE GENERACIÓN" value={dte.generationCode} className="code" />
                                {dte.status === 'CONTINGENCIA' && (
                                    <InfoItem label="EN COLA DESDE" value={formatDateTime(dte.issuedAt, false)} />
                                )}
                                <InfoItem label="SELLO DE RECEPCIÓN"
                                          value={dte.status === 'CONTINGENCIA' ? '— pendiente —' : dte.receptionStamp}
                                          className={dte.status === 'CONTINGENCIA' ? 'pending' : 'code'} />
                                {dte.status === 'SELLADO' || dte.status === 'ANULADO' ? (
                                    <>
                                        <InfoItem label="FECHA Y HORA DE EMISIÓN" value={formatDateTime(dte.issuedAt, false)} />
                                        <InfoItem label="FORMA DE PAGO" value={dte.payment.label + ' · cód. ' + dte.payment.code} />
                                    </>
                                ) : dte.status === 'CONTINGENCIA' ? null : null}
                            </div>
                        </div>

                        {(dte.status === 'SELLADO' || dte.status === 'ANULADO') && (
                            <>
                                <div className="document-section">
                                    <h3>EMISOR</h3>
                                    <div className="info-grid">
                                        <InfoItem label="RAZÓN SOCIAL" value={dte.emitter.businessName} />
                                        <InfoItem label="NOMBRE COMERCIAL" value={dte.emitter.tradeName} />
                                        <InfoItem label="NIT" value={dte.emitter.nit} />
                                        <InfoItem label="NRC" value={dte.emitter.nrc} />
                                        <InfoItem label="ACTIVIDAD ECONÓMICA" value={dte.emitter.economicActivity} />
                                        <InfoItem label="ESTABLECIMIENTO" value={dte.emitter.establishment} />
                                    </div>
                                </div>

                                <div className="document-section">
                                    <h3>RECEPTOR</h3>
                                    <div className="info-grid">
                                        <InfoItem label="NOMBRE / RAZÓN SOCIAL" value={dte.receiver.name} />
                                        <InfoItem label="TIPO DE CONTRIBUYENTE"
                                                  value={dte.receiver.taxpayerType ? TAXPAYER[dte.receiver.taxpayerType] : 'Consumidor final'} />
                                        <InfoItem label="NRC" value={dte.receiver.nrc} />
                                        <InfoItem label="NIT" value={dte.receiver.nit} />
                                        <InfoItem label="DIRECCIÓN" value={dte.receiver.address} />
                                        <InfoItem label="CORREO" value={dte.receiver.email} />
                                    </div>
                                </div>

                                <div className="document-section">
                                    <h3>DETALLE DE LA VENTA</h3>
                                    <table className="detail-table">
                                        <thead>
                                            <tr>
                                                <th>CANT.</th>
                                                <th>DESCRIPCIÓN</th>
                                                <th>PRECIO UNIT.</th>
                                                <th>VENTA NO SUJETA</th>
                                                <th>VENTA EXENTA</th>
                                                <th>VENTA GRAVADA</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {dte.items.map((item, index) => (
                                                <tr key={index}>
                                                    <td>{item.quantity}</td>
                                                    <td>{item.description}<br /><span className="item-code">{item.code}</span></td>
                                                    <td>{formatMoney(item.unitPrice)} c/u</td>
                                                    <td>{formatMoney(item.nonSubjectSale)}</td>
                                                    <td>{formatMoney(item.exemptSale)}</td>
                                                    <td>{formatMoney(item.taxedSale)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="document-totals">
                                    <div className="total-row"><span>Subtotal ventas no sujetas</span>
                                        <span>{formatMoney(dte.items.reduce((t, i) => t + Number(i.nonSubjectSale), 0))}</span></div>
                                    <div className="total-row"><span>Subtotal ventas exentas</span>
                                        <span>{formatMoney(dte.items.reduce((t, i) => t + Number(i.exemptSale), 0))}</span></div>
                                    <div className="total-row"><span>Subtotal ventas gravadas</span>
                                        <span>{formatMoney(dte.items.reduce((t, i) => t + Number(i.taxedSale), 0))}</span></div>
                                    <div className="total-row"><span>Subtotal</span><span>{formatMoney(dte.subtotal)}</span></div>
                                    <div className="total-row"><span>Descuento</span><span>-{formatMoney(dte.discount)}</span></div>
                                    <div className="total-row"><span>IVA (13%)</span><span>{formatMoney(dte.tax)}</span></div>
                                    <div className="total-row final"><span>TOTAL A PAGAR</span><span>{formatMoney(dte.total)}</span></div>
                                </div>
                            </>
                        )}

                        {dte.status === 'RECHAZADO' && (
                            <div className="document-section">
                                <h3>RECEPTOR (A CORREGIR)</h3>
                                <div className="info-grid">
                                    <InfoItem label="NOMBRE / RAZÓN SOCIAL" value={dte.receiver.name} />
                                    <InfoItem label="NRC REPORTADO" className="danger"
                                              value={dte.receiver.nrc
                                                  ? dte.receiver.nrc + (dte.rejectionCode === '003' ? ' (inválido)' : '') : null} />
                                </div>
                            </div>
                        )}

                        {dte.status === 'CONTINGENCIA' && (
                            <div className="document-section">
                                <h3>RESUMEN</h3>
                                <div className="info-grid">
                                    <InfoItem label="RECEPTOR" value={dte.receiver.name} />
                                    <InfoItem label="FORMA DE PAGO" value={dte.payment.label + ' · cód. ' + dte.payment.code} />
                                    <InfoItem label="TOTAL" value={formatMoney(dte.total)} />
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="dte-emitido-sidebar">
                        {(dte.status === 'SELLADO' || dte.status === 'ANULADO') && (
                            <>
                                <div className="qr-card">
                                    <a className="qr-placeholder" href={verificationUrl(dte)} target="_blank"
                                       rel="noopener noreferrer" title="Verificar en el portal de Hacienda">
                                        <svg width="120" height="120" viewBox="0 0 120 120" fill="none">
                                            <rect width="120" height="120" fill="#1e3a5f" />
                                            <rect x="10" y="10" width="40" height="40" fill="white" />
                                            <rect x="70" y="10" width="40" height="40" fill="white" />
                                            <rect x="10" y="70" width="40" height="40" fill="white" />
                                            <rect x="15" y="15" width="30" height="30" fill="#1e3a5f" />
                                            <rect x="75" y="15" width="30" height="30" fill="#1e3a5f" />
                                            <rect x="15" y="75" width="30" height="30" fill="#1e3a5f" />
                                            <rect x="20" y="20" width="20" height="20" fill="white" />
                                            <rect x="80" y="20" width="20" height="20" fill="white" />
                                            <rect x="20" y="80" width="20" height="20" fill="white" />
                                            <rect x="55" y="10" width="5" height="5" fill="white" />
                                            <rect x="55" y="20" width="5" height="5" fill="white" />
                                            <rect x="55" y="55" width="5" height="5" fill="white" />
                                            <rect x="60" y="55" width="5" height="5" fill="white" />
                                            <rect x="65" y="55" width="5" height="5" fill="white" />
                                            <rect x="10" y="55" width="5" height="5" fill="white" />
                                            <rect x="20" y="55" width="5" height="5" fill="white" />
                                            <rect x="30" y="55" width="5" height="5" fill="white" />
                                            <rect x="40" y="55" width="5" height="5" fill="white" />
                                            <rect x="55" y="60" width="5" height="5" fill="white" />
                                            <rect x="70" y="60" width="5" height="5" fill="white" />
                                            <rect x="80" y="60" width="5" height="5" fill="white" />
                                            <rect x="90" y="60" width="5" height="5" fill="white" />
                                            <rect x="100" y="60" width="5" height="5" fill="white" />
                                            <rect x="55" y="70" width="5" height="5" fill="white" />
                                            <rect x="65" y="70" width="5" height="5" fill="white" />
                                            <rect x="75" y="70" width="5" height="5" fill="white" />
                                            <rect x="85" y="70" width="5" height="5" fill="white" />
                                            <rect x="95" y="70" width="5" height="5" fill="white" />
                                            <rect x="105" y="70" width="5" height="5" fill="white" />
                                            <rect x="55" y="80" width="5" height="5" fill="white" />
                                            <rect x="60" y="85" width="5" height="5" fill="white" />
                                            <rect x="70" y="80" width="5" height="5" fill="white" />
                                            <rect x="80" y="90" width="5" height="5" fill="white" />
                                            <rect x="90" y="80" width="5" height="5" fill="white" />
                                            <rect x="100" y="85" width="5" height="5" fill="white" />
                                            <rect x="55" y="95" width="5" height="5" fill="white" />
                                            <rect x="65" y="100" width="5" height="5" fill="white" />
                                            <rect x="75" y="95" width="5" height="5" fill="white" />
                                            <rect x="85" y="100" width="5" height="5" fill="white" />
                                            <rect x="95" y="95" width="5" height="5" fill="white" />
                                            <rect x="105" y="100" width="5" height="5" fill="white" />
                                        </svg>
                                    </a>
                                    <p className="qr-text">Escanea o haz clic para verificar el documento en el portal del
                                        Ministerio de Hacienda</p>
                                </div>

                                <div className="actions-card">
                                    <h3>ACCIONES</h3>
                                    <button className="action-btn primary" onClick={() => window.print()}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                            <polyline points="7 10 12 15 17 10"></polyline>
                                            <line x1="12" y1="15" x2="12" y2="3"></line>
                                        </svg>
                                        Descargar PDF
                                    </button>
                                    <button className="action-btn secondary" onClick={sendEmail}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                                            <polyline points="22,6 12,13 2,6"></polyline>
                                        </svg>
                                        Enviar por correo
                                    </button>
                                    <Link to="/pos" className="action-btn success">{newSaleIcon}Iniciar nueva venta</Link>
                                </div>
                            </>
                        )}

                        {dte.status === 'RECHAZADO' && (
                            <div className="actions-card">
                                <h3>¿QUÉ DESEAS HACER?</h3>
                                {canEditReceiver && (
                                    <>
                                        <button className="action-btn primary" onClick={() => setCustomerModal('retry')}>
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"></path>
                                            </svg>
                                            Corregir datos y reintentar
                                        </button>
                                        <button className="action-btn secondary" onClick={() => setCustomerModal('edit')}>
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path>
                                            </svg>
                                            Editar datos del cliente
                                        </button>
                                    </>
                                )}
                                <button className="action-btn disabled" disabled>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                        <polyline points="7 10 12 15 17 10"></polyline>
                                        <line x1="12" y1="15" x2="12" y2="3"></line>
                                    </svg>
                                    Descargar PDF
                                </button>
                                <p className="action-note">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                    </svg>
                                    No se puede descargar ni enviar un documento rechazado.
                                </p>
                            </div>
                        )}

                        {dte.status === 'CONTINGENCIA' && (
                            <>
                                <div className="progress-card">
                                    <h3>PROGRESO DE LA EMISIÓN</h3>
                                    <div className="progress-timeline">
                                        <div className="progress-step completed">
                                            <div className="step-dot"></div>
                                            <div className="step-info">
                                                <span className="step-name">Venta registrada</span>
                                                <span className="step-time">{formatTime(dte.issuedAt, true)}</span>
                                            </div>
                                        </div>
                                        <div className="progress-step completed">
                                            <div className="step-dot"></div>
                                            <div className="step-info">
                                                <span className="step-name">Documento generado y firmado</span>
                                                <span className="step-time">{formatTime(dte.issuedAt, true)}</span>
                                            </div>
                                        </div>
                                        <div className="progress-step active">
                                            <div className="step-dot"></div>
                                            <div className="step-info">
                                                <span className="step-name">En cola de contingencia</span>
                                                <span className="step-time">
                                                    {formatTime(dte.lastAttemptAt || dte.issuedAt, true)} · {dte.attempts}
                                                    {dte.attempts === 1 ? ' intento' : ' intentos'}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="progress-step pending">
                                            <div className="step-dot"></div>
                                            <div className="step-info">
                                                <span className="step-name">Sello de Hacienda</span>
                                                <span className="step-time">pendiente</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="actions-card">
                                    <h3>ACCIONES</h3>
                                    <button className="action-btn primary" disabled={retrying} onClick={retryNow}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"></path>
                                        </svg>
                                        {retrying ? 'Reintentando...' : 'Reintentar ahora'}
                                    </button>
                                    <Link to="/pos" className="action-btn success">{newSaleIcon}Iniciar nueva venta</Link>
                                    <Link to="/dte?status=CONTINGENCIA" className="action-btn secondary">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                                        </svg>
                                        Ver cola de contingencia
                                    </Link>
                                    <p className="action-note">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <circle cx="12" cy="12" r="10"></circle>
                                            <polyline points="12 6 12 12 16 14"></polyline>
                                        </svg>
                                        Descarga y envío disponibles tras el sellado.
                                    </p>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {customerModal && (
                <CustomerModal
                    title={customerModal === 'retry' ? 'Corregir datos del receptor' : 'Editar datos del cliente'}
                    submitLabel={customerModal === 'retry' ? 'Guardar y reintentar' : 'Guardar cliente'}
                    customer={receiverDraft}
                    onClose={() => setCustomerModal(null)}
                    onSaved={async saved => {
                        setReceiverDraft(saved);
                        if (customerModal === 'retry') {
                            const updated = await retry();
                            if (updated.status === 'RECHAZADO') {
                                toast('Hacienda volvió a rechazar el documento: ' + updated.rejectionMessage, 'error');
                            } else if (updated.status === 'SELLADO') {
                                toast('Datos corregidos: el documento fue sellado por Hacienda.', 'success');
                            }
                            setDte(updated);
                        } else {
                            toast('Cliente actualizado. Usa "Corregir datos y reintentar" para volver a transmitir.', 'success');
                        }
                    }}
                />
            )}
        </>
    );
}
