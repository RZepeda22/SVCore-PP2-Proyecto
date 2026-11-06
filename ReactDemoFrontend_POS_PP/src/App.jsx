import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import { getSession, homeFor } from './session';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Pos from './pages/Pos';
import DteList from './pages/DteList';
import DteResult from './pages/DteResult';
import Inventario from './pages/Inventario';
import Clientes from './pages/Clientes';
import NotaCredito from './pages/NotaCredito';
import Reportes from './pages/Reportes';
import Anulacion from './pages/Anulacion';
import Usuarios from './pages/Usuarios';
import Bitacora from './pages/Bitacora';
import Configuracion from './pages/Configuracion';

function Home() {
    const session = getSession();
    return <Navigate to={session ? homeFor(session) : '/login'} replace />;
}

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />

            <Route element={<AppLayout roles={['VEND', 'ADMN']} />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/pos" element={<Pos />} />
                <Route path="/dte" element={<DteList />} />
                <Route path="/dte/:id" element={<DteResult />} />
                <Route path="/nota-credito" element={<NotaCredito />} />
            </Route>

            <Route element={<AppLayout />}>
                <Route path="/inventario" element={<Inventario />} />
                <Route path="/clientes" element={<Clientes />} />
            </Route>

            <Route element={<AppLayout roles={['ADMN']} />}>
                <Route path="/reportes" element={<Reportes />} />
                <Route path="/anulacion" element={<Anulacion />} />
                <Route path="/usuarios" element={<Usuarios />} />
                <Route path="/bitacora" element={<Bitacora />} />
                <Route path="/configuracion" element={<Configuracion />} />
            </Route>

            <Route path="*" element={<Home />} />
        </Routes>
    );
}
