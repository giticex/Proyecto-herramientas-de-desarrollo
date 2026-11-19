import { useState } from 'react';
import { Link, Outlet } from 'react-router-dom';
import { Menu } from 'lucide-react';
import { useSesion } from '../auth/sesionStore';
import PlantillaTienda from '../components/PlantillaTienda';
import PanelNavLinks from './PanelNavLinks';
import './PanelLayout.css';

export default function PanelLayout() {
  const tienda = useSesion((estado) => estado.tienda);
  const usuario = useSesion((estado) => estado.usuario);
  const [abierto, setAbierto] = useState(false);
  const nombre = tienda?.nombre || 'Panel de negocio';

  return (
    <PlantillaTienda tienda={tienda ?? undefined}>
      <div className="panel-shell">
        <aside className="panel-sidebar" aria-label="Panel de negocio">
          <div className="panel-sidebar-cabecera">
            <Link className="panel-sidebar-marca" to="/admin" onClick={() => setAbierto(false)}>
              {tienda?.logoUrl
                ? <img className="panel-sidebar-logo" src={tienda.logoUrl} alt="" />
                : <span className="panel-sidebar-logo inicial" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</span>}
              <span className="panel-sidebar-nombre">{nombre}</span>
            </Link>
            <button type="button" className="panel-menu-toggle"
              aria-expanded={abierto} aria-controls="panel-menu" onClick={() => setAbierto(!abierto)}>
              <Menu size={18} aria-hidden="true" /> Menú
            </button>
          </div>
          <PanelNavLinks abierto={abierto} cerrar={() => setAbierto(false)} />
        </aside>
        <div className="panel-contenido">
          <div className="panel-topbar">
            <span>{usuario}</span>
            {tienda?.slug && <Link className="btn ghost small" to={'/tienda/' + encodeURIComponent(tienda.slug)}>Ver mi tienda</Link>}
          </div>
          <Outlet />
        </div>
      </div>
    </PlantillaTienda>
  );
}
