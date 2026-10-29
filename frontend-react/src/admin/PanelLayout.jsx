import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useStore } from '../store/store';
import { useClienteStore } from '../tienda/clienteStore';
import PlantillaTienda from '../components/PlantillaTienda';
import PanelNavLinks from './PanelNavLinks';
import './PanelLayout.css';

// Envoltorio común de los paneles protegidos: carga la tienda al montar
// (equivalente al evento tienda-lista de nav.js), aplica PlantillaTienda sin
// la cabecera superior y coloca la navegación en una barra lateral derecha.
export default function PanelLayout({ children }) {
  const getTienda = useStore((estado) => estado.getTienda);
  const tienda = useStore((estado) => estado.tienda);
  const cliente = useClienteStore((estado) => estado.cliente);

  useEffect(() => {
    getTienda();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Última barrera: el sidebar corporativo jamás se dibuja para un comprador.
  if (cliente?.rol === 'CLIENTE') {
    return <Navigate to="/portal-cliente" replace />;
  }

  const inicial = (tienda.nombre || '?').charAt(0).toUpperCase();

  return (
    <PlantillaTienda sinNav>
      <div className="panel-shell">
        <aside className="panel-sidebar" aria-label="Navegación del panel">
          <div className="panel-sidebar-marca">
            {tienda.logoUrl
              ? <img className="panel-sidebar-logo" src={tienda.logoUrl} alt="" />
              : <span className="panel-sidebar-logo inicial" aria-hidden="true">{inicial}</span>}
            <span className="panel-sidebar-nombre">{tienda.nombre}</span>
          </div>
          <PanelNavLinks />
        </aside>

        <div className="panel-contenido">{children}</div>
      </div>
    </PlantillaTienda>
  );
}
