import { Routes, Route, Navigate } from 'react-router-dom';
import AdminLogin from './admin/AdminLogin';
import RegistroTienda from './admin/RegistroTienda';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import RutaProtegida from './auth/RutaProtegida';
import Toast from './components/Toast';
import LandingPlataforma from './tienda/LandingPlataforma';
import CatalogoTienda from './tienda/CatalogoTienda';

// Componentes temporales o en desarrollo para las siguientes tareas de cliente
function EnConstruccion({ titulo }) {
  return (
    <div style={{ padding: '100px 20px', textAlign: 'center' }}>
      <h2>{titulo}</h2>
      <p style={{ color: '#6e6e73', marginTop: 10 }}>Esta pantalla está en proceso de construcción dentro de tu sprint.</p>
    </div>
  );
}

export default function App() {
  return (
    <>
      <Routes>
        {/* === MÓDULO PÚBLICO / PLATAFORMA (Anelhi) === */}
        <Route path="/" element={<LandingPlataforma />} />
        
        {/* === MÓDULO TIENDA Y COMPRADOR (Anelhi) === */}
        <Route path="/tienda/:slug" element={<CatalogoTienda />} />
        <Route path="/tienda/:slug/login" element={<EnConstruccion titulo="Login y Registro de Cliente" />} />
        <Route path="/tienda/:slug/carrito" element={<EnConstruccion titulo="Carrito de Compras con Stock" />} />
        <Route path="/tienda/:slug/checkout" element={<EnConstruccion titulo="Checkout (Boleta / Factura - DNI / RUC)" />} />
        <Route path="/tienda/:slug/confirmacion" element={<EnConstruccion titulo="Confirmación de Compra" />} />
        <Route path="/tienda/:slug/mis-compras" element={<EnConstruccion titulo="Historial de Mis Compras" />} />

        {/* === MÓDULO ADMINISTRADOR Y ONBOARDING (Kiara) === */}
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/registro-tienda" element={<RegistroTienda />} />
        <Route element={<RutaProtegida />}>
          <Route path="/admin" element={<Navigate to="/admin/productos" replace />} />
          <Route path="/admin/marca" element={<MarcaPanel />} />
          <Route path="/admin/productos" element={<ProductosPanel />} />
        </Route>
      </Routes>
      <Toast />
    </>
  );
}