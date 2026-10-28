import { Routes, Route, Link } from 'react-router-dom';
import Login from './auth/Login';
import RegistroTienda from './admin/RegistroTienda';
import DashboardAdmin from './admin/DashboardAdmin';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import HistorialVentas from './admin/HistorialVentas';
import GestionCreditos from './admin/GestionCreditos';
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


function PaginaNoEncontrada() {
  return (
    <div style={{ padding: '100px 20px', textAlign: 'center' }}>
      <h2>Página no encontrada</h2>
      <p style={{ color: '#6e6e73', marginTop: 10 }}>
        La dirección que abriste no existe. <Link to="/">Volver al inicio</Link>
      </p>
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
        <Route path="/login" element={<Login />} />
        <Route path="/admin/login" element={<Login />} />
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
          <Route path="/admin/creditos" element={<GestionCreditos />} />
          <Route path="/admin/pos" element={<PuntoDeVenta />} />
        </Route>

        <Route path="*" element={<PaginaNoEncontrada />} />
      </Routes>
      <Toast />
    </>
  );
}