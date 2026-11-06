import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { ToastProvider } from './components/Toast';

import '../css/styles.css';
import '../css/dashboard.css';
import '../css/dte.css';
import '../css/dte-emitido.css';
import '../css/dte-rechazado.css';
import '../css/dte-contingencia.css';
import '../css/inventario.css';
import '../css/app.css';
import '../css/react.css';

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrowserRouter>
            <ToastProvider>
                <App />
            </ToastProvider>
        </BrowserRouter>
    </React.StrictMode>
);
