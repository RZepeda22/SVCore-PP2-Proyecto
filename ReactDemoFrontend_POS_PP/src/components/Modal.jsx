import { useEffect, useRef, useState } from 'react';

/**
 * Formulario modal. onSubmit(values) puede lanzar ApiError: el mensaje se muestra en el modal
 * y, si la API devolvió errores por campo, se marcan junto a cada campo (inputs con atributo name).
 */
export default function Modal({ title, submitLabel = 'Guardar', onSubmit, onClose, children, wide = false }) {
    const formRef = useRef(null);
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        const onKey = event => { if (event.key === 'Escape') onClose(); };
        document.addEventListener('keydown', onKey);
        const first = formRef.current && formRef.current.querySelector('input:not([disabled]), select, textarea');
        if (first) first.focus();
        return () => document.removeEventListener('keydown', onKey);
    }, [onClose]);

    async function handleSubmit(event) {
        event.preventDefault();
        const form = formRef.current;
        setError(null);
        form.querySelectorAll('.sv-field-error').forEach(el => el.remove());
        form.querySelectorAll('.sv-invalid').forEach(el => el.classList.remove('sv-invalid'));

        const values = {};
        new FormData(form).forEach((value, key) => { values[key] = String(value).trim(); });

        setSubmitting(true);
        try {
            await onSubmit(values, form);
            onClose();
        } catch (err) {
            setError(err.message);
            if (err.data && typeof err.data === 'object') {
                Object.entries(err.data).forEach(([field, message]) => {
                    const input = form.querySelector('[name="' + field + '"]');
                    if (!input) return;
                    input.classList.add('sv-invalid');
                    const hint = document.createElement('small');
                    hint.className = 'sv-field-error';
                    hint.textContent = message;
                    input.insertAdjacentElement('afterend', hint);
                });
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="sv-modal-overlay" onMouseDown={event => { if (event.target === event.currentTarget) onClose(); }}>
            <form className={'sv-modal' + (wide ? ' sv-modal-wide' : '')} ref={formRef} noValidate
                  role="dialog" aria-modal="true" aria-labelledby="sv-modal-title">
                <div className="sv-modal-header">
                    <h3 id="sv-modal-title">{title}</h3>
                    <button type="button" className="sv-modal-close" onClick={onClose} aria-label="Cerrar">&times;</button>
                </div>
                <div className="sv-modal-body">
                    {children}
                    <div className="sv-form-error" hidden={!error}>{error}</div>
                </div>
                <div className="sv-modal-footer">
                    <button type="button" className="sv-btn sv-btn-secondary" onClick={onClose}>Cancelar</button>
                    <button type="submit" className="sv-btn sv-btn-primary" disabled={submitting}>{submitLabel}</button>
                </div>
            </form>
        </div>
    );
}
