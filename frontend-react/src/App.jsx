import { Routes, Route, Navigate } from 'react-router-dom';
import AdminLogin from './admin/AdminLogin';
import RegistroTienda from './admin/RegistroTienda';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import RutaProtegida from './auth/RutaProtegida';
import Toast from './components/Toast';

// Reservada para el catálogo público (la implementa Anelhi en src/tienda).
function TiendaPendiente() {
  return <p style={{ padding: 40 }}>Catálogo en construcción.</p>;
}

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Navigate to="/admin/login" replace />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/registro-tienda" element={<RegistroTienda />} />
        <Route element={<RutaProtegida />}>
          <Route path="/admin" element={<Navigate to="/admin/productos" replace />} />
          <Route path="/admin/marca" element={<MarcaPanel />} />
          <Route path="/admin/productos" element={<ProductosPanel />} />
        </Route>
        <Route path="/tienda/:slug" element={<TiendaPendiente />} />
      </Routes>
      <Toast />
    </>
  );
}
