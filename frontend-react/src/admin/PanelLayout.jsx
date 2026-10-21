import { useEffect } from 'react';
import { useStore } from '../store/store';
import PlantillaTienda from '../components/PlantillaTienda';
import PanelNavLinks from './PanelNavLinks';

// Envoltorio común de los paneles protegidos: carga la tienda al montar
// (equivalente al evento tienda-lista de nav.js) y aplica PlantillaTienda
// con los nav-links del panel.
export default function PanelLayout({ children }) {
  const getTienda = useStore((estado) => estado.getTienda);

  useEffect(() => {
    getTienda();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <PlantillaTienda navLinks={<PanelNavLinks />}>
      {children}
    </PlantillaTienda>
  );
}
