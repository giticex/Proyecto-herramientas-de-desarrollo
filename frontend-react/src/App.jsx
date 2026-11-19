import { Link, NavLink, Outlet, Routes, Route } from 'react-router-dom';
import AdminLogin from './admin/AdminLogin';
import RegistroTienda from './admin/RegistroTienda';
import DashboardAdmin from './admin/DashboardAdmin';
import MarcaPanel from './admin/MarcaPanel';
import ProductosPanel from './admin/ProductosPanel';
import HistorialVentas from './admin/HistorialVentas';
import FacturacionPanel from './admin/FacturacionPanel';
import RutaProtegida from './auth/RutaProtegida';
import PanelLayout from './admin/PanelLayout';
import PlantillaTienda from './components/PlantillaTienda';
import TiendaLayout from './tienda/TiendaLayout';
import Toast from './components/Toast';
import LandingPlataforma from './tienda/LandingPlataforma';
import CatalogoTienda from './tienda/CatalogoTienda';
import ClienteAuth from './tienda/ClienteAuth';
import CarritoTienda from './tienda/CarritoTienda';
import CheckoutTienda from './tienda/CheckoutTienda';
import ConfirmacionCompra from './tienda/ConfirmacionCompra';
import MisCompras from './tienda/MisCompras';
import RutaProtegidaCliente from './tienda/RutaProtegidaCliente';


function AccesoPlataforma() {
  const enlaces = (
    <>
      <NavLink to="/admin/login">Iniciar sesión</NavLink>
      <NavLink to="/registro-tienda">Crear tienda</NavLink>
    </>
  );

  return (
    <PlantillaTienda tienda={{ nombre: 'NovaCommerce' }} inicio="/" navLinks={enlaces}>
      <Outlet />
    </PlantillaTienda>
  );
}

export default function App() {
  return (
    <>
      <Routes>
        {/* === 1. LANDING SAAS PRINCIPAL (Para captar dueños de tiendas) === */}
        <Route path="/" element={<LandingPlataforma />} />

        <Route path="/tienda/:slug" element={<TiendaLayout />}>
          <Route index element={<CatalogoTienda />} />
          <Route path="login" element={<ClienteAuth />} />
          <Route path="carrito" element={<CarritoTienda />} />
          <Route element={<RutaProtegidaCliente />}>
            <Route path="checkout" element={<CheckoutTienda />} />
            <Route path="confirmacion" element={<ConfirmacionCompra />} />
            <Route path="mis-compras" element={<MisCompras />} />
          </Route>
        </Route>

        <Route element={<AccesoPlataforma />}>
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/registro-tienda" element={<RegistroTienda />} />
        </Route>
        <Route element={<RutaProtegida />}>
          <Route path="/admin" element={<PanelLayout />}>
            <Route index element={<DashboardAdmin />} />
            <Route path="marca" element={<MarcaPanel />} />
            <Route path="productos" element={<ProductosPanel />} />
            <Route path="ventas" element={<HistorialVentas />} />
            <Route path="facturacion" element={<FacturacionPanel />} />
          </Route>
        </Route>
        <Route path="*" element={
          <main className="page">
            <h1>Página no encontrada</h1>
            <Link className="btn ghost" to="/">Volver al inicio</Link>
          </main>
        } />
      </Routes>
      <Toast />
    </>
  );
}