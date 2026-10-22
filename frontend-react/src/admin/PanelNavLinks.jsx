import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, LogOut, Package, Palette, Receipt } from 'lucide-react';
import { useSesion } from '../auth/sesionStore';

// Navegación del panel de administrador. Vive dentro de la barra lateral
// derecha (ver PanelLayout) y no en la cabecera superior.
export default function PanelNavLinks() {
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();

  function salir() {
    logout();
    navigate('/admin/login');
  }

  return (
    <nav className="panel-menu">
      <NavLink to="/admin" end className="panel-menu-link">
        <LayoutDashboard size={18} aria-hidden="true" />
        Dashboard
      </NavLink>
      <NavLink to="/admin/marca" className="panel-menu-link">
        <Palette size={18} aria-hidden="true" />
        Marca
      </NavLink>
      <NavLink to="/admin/productos" className="panel-menu-link">
        <Package size={18} aria-hidden="true" />
        Productos
      </NavLink>
      <NavLink to="/admin/ventas" className="panel-menu-link">
        <Receipt size={18} aria-hidden="true" />
        Ventas
      </NavLink>
      <button className="panel-menu-link salir" type="button" onClick={salir}>
        <LogOut size={18} aria-hidden="true" />
        Salir
      </button>
    </nav>
  );
}
