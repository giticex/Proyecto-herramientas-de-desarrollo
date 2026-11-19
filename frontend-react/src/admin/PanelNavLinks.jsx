import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, LogOut, Package, Palette, Receipt, FileText } from 'lucide-react';
import { useSesion } from '../auth/sesionStore';

export default function PanelNavLinks({ abierto, cerrar }) {
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();

  function salir() {
    logout();
    navigate('/admin/login', { replace: true });
  }

  return (
    <nav className="panel-menu" id="panel-menu" data-abierto={abierto} aria-label="Módulos de administración">
      <NavLink to="/admin" end className="panel-menu-link" onClick={cerrar}>
        <LayoutDashboard size={18} aria-hidden="true" />
        Resumen
      </NavLink>
      <NavLink to="/admin/marca" className="panel-menu-link" onClick={cerrar}>
        <Palette size={18} aria-hidden="true" />
        Marca
      </NavLink>
      <NavLink to="/admin/productos" className="panel-menu-link" onClick={cerrar}>
        <Package size={18} aria-hidden="true" />
        Productos
      </NavLink>
      <NavLink to="/admin/ventas" className="panel-menu-link" onClick={cerrar}>
        <Receipt size={18} aria-hidden="true" />
        Ventas
      </NavLink>
      <NavLink to="/admin/facturacion" className="panel-menu-link" onClick={cerrar}>
        <FileText size={18} aria-hidden="true" />
        Facturación
      </NavLink>
      <button className="panel-menu-link salir" type="button" onClick={salir}>
        <LogOut size={18} aria-hidden="true" />
        Salir
      </button>
    </nav>
  );
}
