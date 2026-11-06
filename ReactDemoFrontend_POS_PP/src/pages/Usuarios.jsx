import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { formatDate, initials } from '../format';
import { ROLE_LABELS } from '../session';
import Modal from '../components/Modal';
import { useToast } from '../components/Toast';

function roleOptions(roles, selected) {
    return roles.map(role => (
        <label key={role.id} className="sv-check-option">
            <input type="checkbox" name={'role_' + role.id} defaultChecked={selected.includes(role.id)} />
            {role.id} · {ROLE_LABELS[role.id] || role.name || role.id}
        </label>
    ));
}

function selectedRoles(roles, values) {
    return roles.map(r => r.id).filter(id => values['role_' + id]);
}

export default function Usuarios() {
    const toast = useToast();
    const [users, setUsers] = useState([]);
    const [roles, setRoles] = useState([]);
    const [error, setError] = useState(null);
    const [modal, setModal] = useState(null); // { mode: 'create' | 'edit' | 'reset', user }

    const load = useCallback(async () => {
        try {
            const [userList, roleList] = await Promise.all([api('/api/user/all'), api('/api/rol/all')]);
            setUsers(userList);
            setRoles(roleList);
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    async function unlock(user) {
        if (!window.confirm('¿Desbloquear la cuenta de ' + user.username + '?')) return;
        try {
            await api('/api/user/unlock?userId=' + encodeURIComponent(user.id), { method: 'PATCH' });
            toast('Usuario ' + user.username + ' desbloqueado.', 'success');
            load();
        } catch (err) {
            toast(err.message, 'error');
        }
    }

    return (
        <>
            <header className="inventory-header">
                <div className="header-left">
                    <h1>Usuarios y roles</h1>
                    <p>CU-10 · Administración de accesos al sistema</p>
                </div>
                <div className="header-right">
                    <button className="btn-add-entry" onClick={() => setModal({ mode: 'create' })}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        Nuevo usuario
                    </button>
                </div>
            </header>

            <div className="inventory-content">
                <div className="inventory-table-container">
                    <div className="table-header">
                        <div className="table-title">
                            <h2>Cuentas del sistema</h2>
                            <span className="table-subtitle">· La contraseña se cifra con BCrypt; bloqueo tras 5 intentos fallidos</span>
                        </div>
                    </div>
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>USUARIO</th>
                                <th>CORREO</th>
                                <th>ROLES</th>
                                <th>ESTADO</th>
                                <th>CREADO</th>
                                <th>ACCIONES</th>
                            </tr>
                        </thead>
                        <tbody>
                            {error ? (
                                <tr><td colSpan="6" className="sv-empty">{error}</td></tr>
                            ) : users.length ? users.map(user => (
                                <tr key={user.id}>
                                    <td>
                                        <div className="sv-user-cell">
                                            <div className="dropdown-avatar">{initials(user.fullName)}</div>
                                            <div>
                                                <div className="product-name-cell">{user.fullName}</div>
                                                <div className="code-cell">@{user.username}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>{user.email}</td>
                                    <td>
                                        <div className="sv-role-list">
                                            {user.roles.map(role => (
                                                <span key={role} className="type-badge">{ROLE_LABELS[role] || role}</span>
                                            ))}
                                        </div>
                                    </td>
                                    <td>
                                        {user.locked ? (
                                            <span className="status-badge danger">Bloqueado</span>
                                        ) : user.active ? (
                                            <span className="status-badge success">Activo</span>
                                        ) : (
                                            <span className="status-badge neutral">Inactivo</span>
                                        )}
                                    </td>
                                    <td>{formatDate(user.createdAt)}</td>
                                    <td>
                                        <div className="sv-actions">
                                            <button type="button" className="action-link"
                                                    onClick={() => setModal({ mode: 'edit', user })}>Editar</button>
                                            <button type="button" className="action-link"
                                                    onClick={() => setModal({ mode: 'reset', user })}>Restablecer clave</button>
                                            {user.locked && (
                                                <button type="button" className="action-link correct"
                                                        onClick={() => unlock(user)}>Desbloquear</button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td colSpan="6" className="sv-empty">Cargando usuarios...</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {modal && modal.mode === 'create' && (
                <Modal
                    title="Crear usuario"
                    submitLabel="Crear usuario"
                    onClose={() => setModal(null)}
                    onSubmit={async values => {
                        const roleIds = selectedRoles(roles, values);
                        if (!roleIds.length) throw new Error('Asigna al menos un rol.');
                        await api('/api/user/save', {
                            method: 'POST',
                            body: {
                                username: values.username,
                                email: values.email,
                                fullName: values.fullName,
                                password: values.password,
                                roles: roleIds
                            }
                        });
                        toast('Usuario ' + values.username + ' creado.', 'success');
                        load();
                    }}
                >
                    <div className="sv-form-row">
                        <div className="form-group">
                            <label htmlFor="us-username">Usuario</label>
                            <input id="us-username" name="username" className="form-input" required
                                   placeholder="minúsculas, números, . y _" maxLength={50} />
                        </div>
                        <div className="form-group">
                            <label htmlFor="us-fullname">Nombre completo</label>
                            <input id="us-fullname" name="fullName" className="form-input" required maxLength={100} />
                        </div>
                    </div>
                    <div className="form-group">
                        <label htmlFor="us-email">Correo electrónico</label>
                        <input id="us-email" name="email" type="email" className="form-input" required maxLength={100} />
                    </div>
                    <div className="form-group">
                        <label htmlFor="us-password">Contraseña inicial</label>
                        <input id="us-password" name="password" type="password" className="form-input" required
                               autoComplete="new-password" />
                    </div>
                    <div className="form-group">
                        <label>Roles</label>
                        <div className="sv-check-group">{roleOptions(roles, [])}</div>
                    </div>
                </Modal>
            )}

            {modal && modal.mode === 'edit' && (
                <Modal
                    title={'Editar usuario · @' + modal.user.username}
                    submitLabel="Guardar cambios"
                    onClose={() => setModal(null)}
                    onSubmit={async values => {
                        const roleIds = selectedRoles(roles, values);
                        if (!roleIds.length) throw new Error('Asigna al menos un rol.');
                        await api('/api/user/edit?userId=' + encodeURIComponent(modal.user.id), {
                            method: 'PATCH',
                            body: {
                                email: values.email,
                                fullName: values.fullName,
                                roles: roleIds,
                                active: values.active === 'true'
                            }
                        });
                        toast('Usuario actualizado.', 'success');
                        load();
                    }}
                >
                    <div className="form-group">
                        <label htmlFor="ue-fullname">Nombre completo</label>
                        <input id="ue-fullname" name="fullName" className="form-input" required maxLength={100}
                               defaultValue={modal.user.fullName} />
                    </div>
                    <div className="form-group">
                        <label htmlFor="ue-email">Correo electrónico</label>
                        <input id="ue-email" name="email" type="email" className="form-input" required maxLength={100}
                               defaultValue={modal.user.email} />
                    </div>
                    <div className="form-group">
                        <label htmlFor="ue-active">Estado de la cuenta</label>
                        <select id="ue-active" name="active" className="form-input"
                                defaultValue={String(modal.user.active)}>
                            <option value="true">Activa (puede iniciar sesión)</option>
                            <option value="false">Inactiva (acceso deshabilitado)</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Roles</label>
                        <div className="sv-check-group">{roleOptions(roles, modal.user.roles)}</div>
                    </div>
                </Modal>
            )}

            {modal && modal.mode === 'reset' && (
                <Modal
                    title={'Restablecer contraseña · @' + modal.user.username}
                    submitLabel="Restablecer contraseña"
                    onClose={() => setModal(null)}
                    onSubmit={async values => {
                        await api('/api/user/reset-password?userId=' + encodeURIComponent(modal.user.id), {
                            method: 'PATCH',
                            body: { newPassword: values.newPassword }
                        });
                        toast('Contraseña restablecida para ' + modal.user.username + '.', 'success');
                        load();
                    }}
                >
                    <div className="form-group">
                        <label htmlFor="ur-password">Nueva contraseña</label>
                        <input id="ur-password" name="newPassword" type="password" className="form-input" required
                               autoComplete="new-password" />
                        <small className="sv-help">Debe cumplir la política de contraseñas del sistema.</small>
                    </div>
                </Modal>
            )}
        </>
    );
}
