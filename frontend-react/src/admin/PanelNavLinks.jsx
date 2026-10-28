import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, LogOut, Package, Palette, Receipt, ScanBarcode, Wallet } from 'lucide-react';
import { useSesion } from '../auth/sesionStore';

// Navegación del panel de administrador. Vive dentro de la barra lateral
// derecha (ver PanelLayout) y no en la cabecera superior. Cada opción se
// muestra según el rol: el catálogo es solo ADMIN; POS y ventas son para
// ADMIN y VENDEDOR, así el vendedor aterriza directo en su trabajo.
export default function PanelNavLinks() {
  const rol = useSesion((estado) => estado.rol);
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();
  const esAdmin = rol === 'ADMIN';

  function salir() {
    logout();
    navigate('/admin/login');
  }

  return (
    <nav className="panel-menu">
      {esAdmin && (
        <NavLink to="/admin" end className="panel-menu-link">
          <LayoutDashboard size={18} aria-hidden="true" />
          Dashboard
        </NavLink>
      )}
      <NavLink to="/admin/pos" className="panel-menu-link">
        <ScanBarcode size={18} aria-hidden="true" />
        Punto de venta
      </NavLink>
      {esAdmin && (
        <NavLink to="/admin/marca" className="panel-menu-link">
          <Palette size={18} aria-hidden="true" />
          Marca
        </NavLink>
      )}
      {esAdmin && (
        <NavLink to="/admin/productos" className="panel-menu-link">
          <Package size={18} aria-hidden="true" />
          Productos
        </NavLink>
      )}
      <NavLink to="/admin/ventas" className="panel-menu-link">
        <Receipt size={18} aria-hidden="true" />
        {esAdmin ? 'Ventas' : 'Historial de ventas'}
      </NavLink>
      <NavLink to="/admin/creditos" className="panel-menu-link creditos">
        <Wallet size={18} aria-hidden="true" />
        Créditos
      </NavLink>
      <button className="panel-menu-link salir" type="button" onClick={salir}>
        <LogOut size={18} aria-hidden="true" />
        Salir
      </button>
    </nav>
  );
}
