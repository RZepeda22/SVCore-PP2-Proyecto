import { useState } from 'react';
import { Navigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { getSession, homeFor, saveSession } from '../session';

export default function Login() {
    const existing = getSession();
    const [params] = useSearchParams();
    const [identifier, setIdentifier] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [remember, setRemember] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState(params.get('motivo') === 'expirada'
        ? { text: 'Tu sesión expiró por seguridad. Inicia sesión de nuevo.', info: true }
        : null);
    const [session, setSession] = useState(null);

    if (existing) return <Navigate to={homeFor(existing)} replace />;
    if (session) return <Navigate to={homeFor(session)} replace />;

    function showMessage(text, info = false) {
        setMessage({ text, info });
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setMessage(null);

        if (!identifier.trim() || !password) {
            showMessage('Ingresa tu usuario o correo y tu contraseña.');
            return;
        }

        setSubmitting(true);
        try {
            const token = await api('/api/auth/login', {
                method: 'POST',
                body: { identifier: identifier.trim(), password }
            });
            setSession(saveSession(token, remember));
        } catch (error) {
            setPassword('');
            showMessage(error.message);
            setSubmitting(false);
        }
    }

    return (
        <div className="login-container">
            <div className="login-left">
                <div className="login-brand">
                    <div className="brand-icon">SV</div>
                    <div className="brand-text">
                        <h2>SV-Core</h2>
                        <span>Adventure Works</span>
                    </div>
                </div>

                <div className="login-hero">
                    <h1>Tus ventas, inventario y facturación electrónica en un solo lugar.</h1>
                    <p>Centraliza la operación diaria del negocio y emite DTE conforme a la normativa del Ministerio de
                        Hacienda de El Salvador.</p>
                    <div className="login-badges">
                        <span className="badge"><span className="badge-dot"></span>Ventas integradas</span>
                        <span className="badge"><span className="badge-dot"></span>DTE con contingencia</span>
                        <span className="badge"><span className="badge-dot"></span>Stock en tiempo real</span>
                    </div>
                </div>

                <div></div>
            </div>

            <div className="login-right">
                <div className="login-form-container">
                    <h2>Inicia sesión</h2>
                    <p>Ingresa tus credenciales para continuar.</p>

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label htmlFor="email">Usuario o correo</label>
                            <input type="text" id="email" className="form-input" placeholder="usuario o correo"
                                   autoComplete="username" required value={identifier}
                                   onChange={event => setIdentifier(event.target.value)} />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Contraseña</label>
                            <div className="password-wrapper">
                                <input type={showPassword ? 'text' : 'password'} id="password" className="form-input"
                                       placeholder="••••••••" autoComplete="current-password" required value={password}
                                       onChange={event => setPassword(event.target.value)} />
                                <button type="button" className="password-toggle"
                                        aria-label="Mostrar u ocultar contraseña"
                                        onClick={() => setShowPassword(value => !value)}>
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                         strokeWidth="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                            </div>
                        </div>

                        <div className="form-options">
                            <label className="checkbox-label">
                                <input type="checkbox" checked={remember}
                                       onChange={event => setRemember(event.target.checked)} />
                                Mantener sesión iniciada
                            </label>
                            <a href="#" className="forgot-link" onClick={event => {
                                event.preventDefault();
                                showMessage('Para restablecer tu contraseña contacta al administrador del sistema.', true);
                            }}>¿Olvidaste tu contraseña?</a>
                        </div>

                        <div className={'sv-login-error' + (message && message.info ? ' info' : '')} role="alert"
                             hidden={!message}>{message && message.text}</div>

                        <button type="submit" className="btn-primary" disabled={submitting}>
                            {submitting ? 'Verificando...' : 'Entrar al sistema'}
                        </button>
                    </form>

                    <div className="login-footer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             strokeWidth="2">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <span>Conexión cifrada · El acceso se bloquea tras 5 intentos fallidos</span>
                    </div>
                </div>
            </div>
        </div>
    );
}
