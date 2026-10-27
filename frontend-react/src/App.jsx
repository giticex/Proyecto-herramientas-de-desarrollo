import { Routes, Route } from 'react-router-dom';
import AdminLogin from './admin/AdminLogin';
import RegistroTienda from './admin/RegistroTienda';
import DashboardAdmin from './admin/DashboardAdmin';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import HistorialVentas from './admin/HistorialVentas';
import PuntoDeVenta from './admin/PuntoDeVenta';
import RutaProtegida from './auth/RutaProtegida';
import Toast from './components/Toast';
import LandingPlataforma from './tienda/LandingPlataforma';
import CatalogoTienda from './tienda/CatalogoTienda';
import ClienteAuth from './tienda/ClienteAuth';
import CarritoTienda from './tienda/CarritoTienda';
import CheckoutTienda from './tienda/CheckoutTienda';
import ConfirmacionCompra from './tienda/ConfirmacionCompra';
import MisCompras from './tienda/MisCompras';
import RutaProtegidaCliente from './tienda/RutaProtegidaCliente';


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
        {/* === 1. LANDING SAAS PRINCIPAL (Para captar dueños de tiendas) === */}
        <Route path="/" element={<LandingPlataforma />} />

        {/* === 2. VITRINA PÚBLICA DE LA TIENDA (Abierta a todo comprador) === */}
        <Route path="/tienda/:slug" element={<CatalogoTienda />} />
        <Route path="/tienda/:slug/login" element={<ClienteAuth />} />
        <Route path="/tienda/:slug/carrito" element={<CarritoTienda />} />

        {/* === 3. ZONA PRIVADA DEL COMPRADOR (Requiere Login de Cliente) === */}
        <Route element={<RutaProtegidaCliente />}>
          <Route path="/tienda/:slug/checkout" element={<CheckoutTienda />} />
          <Route path="/tienda/:slug/confirmacion" element={<ConfirmacionCompra />} />
          <Route path="/tienda/:slug/mis-compras" element={<MisCompras />} />
        </Route>

        {/* === 4. MÓDULO ADMINISTRADOR Y ONBOARDING (Kiara) === */}
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/registro-tienda" element={<RegistroTienda />} />
        {/* Catálogo y configuración: solo ADMIN (el backend también lo exige). */}
        <Route element={<RutaProtegida roles={['ADMIN']} />}>
          <Route path="/admin" element={<DashboardAdmin />} />
          <Route path="/admin/marca" element={<MarcaPanel />} />
          <Route path="/admin/productos" element={<ProductosPanel />} />
        </Route>
        {/* Operación de tienda: ADMIN y VENDEDOR. */}
        <Route element={<RutaProtegida roles={['ADMIN', 'VENDEDOR']} />}>
          <Route path="/admin/ventas" element={<HistorialVentas />} />
          <Route path="/admin/pos" element={<PuntoDeVenta />} />
        </Route>
      </Routes>
      <Toast />
    </>
  );
}