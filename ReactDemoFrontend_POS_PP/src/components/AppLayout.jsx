import { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import { getSession, hasRole, homeFor, loginUrl, logout, storedSessionExists } from '../session';

/** Protege las rutas: sin sesión vuelve al login; sin el rol requerido va a su pantalla de inicio. */
export default function AppLayout({ roles }) {
    const session = getSession();
    const location = useLocation();

    // RNF-05: al vencer el token (15 min) la sesión se cierra sola
    useEffect(() => {
        if (!session) return undefined;
        const timer = setTimeout(() => logout('expirada'), Math.max(0, session.expiresAt - Date.now()));
        return () => clearTimeout(timer);
    }, [location.pathname]);

    if (!session) return <Navigate to={loginUrl(storedSessionExists() ? 'expirada' : '')} replace />;
    if (!hasRole(session, roles)) return <Navigate to={homeFor(session)} replace />;

    return (
        <div className="pos-container">
            <Sidebar session={session} />
            <main className="pos-main">
                <Outlet context={session} />
            </main>
        </div>
    );
}
