import { Routes, Route, Navigate } from 'react-router-dom';
import AdminLogin from './admin/AdminLogin';
import AdminPanel from './admin/AdminPanel';
import RutaProtegida from './auth/RutaProtegida';

// Reservada para el catálogo público (la implementa Anelhi en src/tienda).
function TiendaPendiente() {
  return <p style={{ padding: 40 }}>Catálogo en construcción.</p>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/admin/login" replace />} />
      <Route path="/admin/login" element={<AdminLogin />} />
      <Route element={<RutaProtegida />}>
        <Route path="/admin" element={<AdminPanel />} />
      </Route>
      <Route path="/tienda/:slug" element={<TiendaPendiente />} />
    </Routes>
  );
}
