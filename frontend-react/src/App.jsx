import { Routes, Route, Link, Navigate } from 'react-router-dom';
import Login from './auth/Login';
import RegistroTienda from './admin/RegistroTienda';
import DashboardAdmin from './admin/DashboardAdmin';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import PersonalPanel from './admin/PersonalPanel';
import ConfiguracionPanel from './admin/ConfiguracionPanel';
import ArqueoCajaPanel from './admin/ArqueoCajaPanel';
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
import PortalCliente from './portal/PortalCliente';
import RutaPortalCliente from './portal/RutaPortalCliente';
import SuperAdminPanel from './superadmin/SuperAdminPanel';
import { useClienteStore } from './tienda/clienteStore';


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

// Rutas internas antiguas: un CLIENTE que las escriba a mano vuelve a su
// portal; para el personal siguen sin existir, como antes.
function BloqueoCliente() {
  const cliente = useClienteStore((estado) => estado.cliente);
  if (cliente?.rol === 'CLIENTE') {
    return <Navigate to="/portal-cliente" replace />;
  }
  return <PaginaNoEncontrada />;
}

// /tienda sin slug: el comprador va a su portal y el visitante al catálogo.
function RedireccionTienda() {
  const cliente = useClienteStore((estado) => estado.cliente);
  if (cliente?.rol === 'CLIENTE') {
    return <Navigate to="/portal-cliente" replace />;
  }
  return <Navigate to="/tienda/mi-tienda" replace />;
}

export default function App() {
  return (
    <>
      <Routes>
        {/* === 1. LANDING SAAS PRINCIPAL (Para captar dueños de tiendas) === */}
        <Route path="/" element={<LandingPlataforma />} />

        {/* === 2. VITRINA PÚBLICA DE LA TIENDA (Abierta a todo comprador) === */}
        <Route path="/tienda" element={<RedireccionTienda />} />
        <Route path="/tienda/:slug" element={<CatalogoTienda />} />
        <Route path="/tienda/:slug/login" element={<ClienteAuth />} />
        <Route path="/tienda/:slug/carrito" element={<CarritoTienda />} />

        {/* === 2b. PORTAL DEL CLIENTE (exclusivo del rol CLIENTE) === */}
        <Route element={<RutaPortalCliente />}>
          <Route path="/portal-cliente" element={<PortalCliente />} />
        </Route>

        {/* === 3. ZONA PRIVADA DEL COMPRADOR (Requiere Login de Cliente) === */}
        <Route element={<RutaProtegidaCliente />}>
          <Route path="/tienda/:slug/checkout" element={<CheckoutTienda />} />
          <Route path="/tienda/:slug/confirmacion" element={<ConfirmacionCompra />} />
          <Route path="/tienda/:slug/mis-compras" element={<MisCompras />} />
        </Route>

        {/* === 4. CONSOLA GLOBAL DE LA PLATAFORMA (solo SUPER_ADMIN) === */}
        <Route element={<RutaProtegida roles={['SUPER_ADMIN']} />}>
          <Route path="/super-admin/dashboard" element={<SuperAdminPanel />} />
        </Route>
        <Route path="/super-admin" element={<Navigate to="/super-admin/dashboard" replace />} />

        {/* === 5. MÓDULO ADMINISTRADOR Y ONBOARDING (Kiara) === */}
        <Route path="/login" element={<Login />} />
        <Route path="/admin/login" element={<Login />} />
        <Route path="/registro-tienda" element={<RegistroTienda />} />
        {/* Catálogo y configuración: solo ADMIN (el backend también lo exige). */}
        <Route element={<RutaProtegida roles={['ADMIN']} />}>
          <Route path="/admin" element={<DashboardAdmin />} />
          <Route path="/admin/marca" element={<MarcaPanel />} />
          <Route path="/admin/productos" element={<ProductosPanel />} />
          <Route path="/admin/personal" element={<PersonalPanel />} />
          <Route path="/admin/configuracion" element={<ConfiguracionPanel />} />
          <Route path="/admin/arqueo" element={<ArqueoCajaPanel />} />
        </Route>
        {/* Operación de tienda: ADMIN y VENDEDOR. */}
        <Route element={<RutaProtegida roles={['ADMIN', 'VENDEDOR']} />}>
          <Route path="/admin/ventas" element={<HistorialVentas />} />
          <Route path="/admin/creditos" element={<GestionCreditos />} />
          <Route path="/admin/pos" element={<PuntoDeVenta />} />
        </Route>

        {/* Rutas internas antiguas que un CLIENTE no debe poder abrir. */}
        <Route path="/dashboard" element={<BloqueoCliente />} />
        <Route path="/productos" element={<BloqueoCliente />} />
        <Route path="/punto-de-venta" element={<BloqueoCliente />} />
        <Route path="/marca" element={<BloqueoCliente />} />

        <Route path="*" element={<PaginaNoEncontrada />} />
      </Routes>
      <Toast />
    </>
  );
}