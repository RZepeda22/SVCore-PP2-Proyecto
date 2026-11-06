import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { api } from '../api';
import { hasRole, logout, ROLE_LABELS } from '../session';
import { initials } from '../format';

const NAV_SECTIONS = [
    {
        title: 'Operación',
        items: [
            { to: '/dashboard', icon: 'shopping-cart.png', label: 'Punto de venta', roles: ['VEND', 'ADMN'] },
            { to: '/dte', icon: 'logistics.png', label: 'Facturación (DTE)', roles: ['VEND', 'ADMN'] },
            { to: '/inventario', icon: 'online-shop.png', label: 'Inventario', badge: true },
            { to: '/clientes', icon: 'user.png', label: 'Clientes' },
            { to: '/nota-credito', icon: 'report.png', label: 'Nota de crédito', roles: ['VEND', 'ADMN'] }
        ]
    },
    {
        title: 'Gestión',
        items: [
            { to: '/reportes', icon: 'report.png', label: 'Reportes', roles: ['ADMN'] },
            { to: '/anulacion', icon: 'logistics.png', label: 'Anulación de DTE', roles: ['ADMN'] },
            { to: '/usuarios', icon: 'user.png', label: 'Usuarios', roles: ['ADMN'] },
            { to: '/bitacora', icon: 'setting.png', label: 'Bitácora', roles: ['ADMN'] },
            { to: '/configuracion', icon: 'setting.png', label: 'Configuración', roles: ['ADMN'] }
        ]
    }
];

export default function Sidebar({ session }) {
    const [lowStock, setLowStock] = useState(0);
    const isSeller = session.roles.includes('VEND');
    const role = ROLE_LABELS[session.roles.find(r => ROLE_LABELS[r])] || '';

    useEffect(() => {
        let active = true;
        api('/api/product/all')
            .then(products => {
                if (active) setLowStock((products || []).filter(p => p.stock < p.minStock).length);
            })
            .catch(() => { /* el badge es informativo: si falla no se interrumpe la pantalla */ });
        return () => { active = false; };
    }, []);

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-icon">SV</div>
                <div className="brand-text">
                    <h2>SV-Core</h2>
                    <span>Adventure Works</span>
                </div>
            </div>

            <nav className="sidebar-nav">
                {NAV_SECTIONS.map(section => (
                    <div className="nav-section" key={section.title}>
                        <div className="nav-section-title">{section.title}</div>
                        {section.items.filter(item => hasRole(session, item.roles)).map(item => (
                            <NavLink key={item.to} to={item.to}
                                     className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
                                <span className="icon"><img src={'/assets/' + item.icon} alt={item.label} /></span>
                                {item.label}
                                {item.badge && <span className="nav-badge">{lowStock > 0 ? lowStock : ''}</span>}
                            </NavLink>
                        ))}
                    </div>
                ))}
            </nav>

            <div className="sidebar-user">
                <div className="user-avatar">{initials(session.fullName)}</div>
                <div className="user-info">
                    <div className="user-name">{session.fullName}</div>
                    <div className="user-role">{role + (isSeller ? ' · Caja 02' : '')}</div>
                </div>
                <button type="button" className="sv-logout" title="Cerrar sesión" aria-label="Cerrar sesión"
                        onClick={() => logout()}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                        <polyline points="16 17 21 12 16 7"></polyline>
                        <line x1="21" y1="12" x2="9" y2="12"></line>
                    </svg>
                </button>
            </div>
        </aside>
    );
}
