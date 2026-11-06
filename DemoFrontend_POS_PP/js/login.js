// Inicio de sesión contra POST /api/auth/login
(function () {
    'use strict';

    const existing = SV.getSession();
    if (existing) {
        window.location.replace(SV.homeFor(existing));
        return;
    }

    const form = document.getElementById('loginForm');
    const identifier = document.getElementById('email');
    const password = document.getElementById('password');
    const remember = document.getElementById('remember');
    const button = document.getElementById('loginButton');
    const errorBox = document.getElementById('loginError');

    function showMessage(message, info) {
        errorBox.textContent = message;
        errorBox.classList.toggle('info', !!info);
        errorBox.hidden = false;
    }

    const reason = new URLSearchParams(window.location.search).get('motivo');
    if (reason === 'expirada') {
        showMessage('Tu sesión expiró por seguridad. Inicia sesión de nuevo.', true);
    }

    document.getElementById('passwordToggle').addEventListener('click', () => {
        password.type = password.type === 'password' ? 'text' : 'password';
    });

    document.getElementById('forgotLink').addEventListener('click', event => {
        event.preventDefault();
        showMessage('Para restablecer tu contraseña contacta al administrador del sistema.', true);
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        errorBox.hidden = true;

        if (!identifier.value.trim() || !password.value) {
            showMessage('Ingresa tu usuario o correo y tu contraseña.');
            return;
        }

        button.disabled = true;
        button.textContent = 'Verificando...';
        try {
            const token = await SV.api('/api/auth/login', {
                method: 'POST',
                body: { identifier: identifier.value.trim(), password: password.value }
            });
            const session = SV.saveSession(token, remember.checked);
            window.location.replace(SV.homeFor(session));
        } catch (error) {
            password.value = '';
            password.focus();
            showMessage(error.message);
            button.disabled = false;
            button.textContent = 'Entrar al sistema';
        }
    });
})();
