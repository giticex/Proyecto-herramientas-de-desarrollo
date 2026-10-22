import PlantillaTienda from '../components/PlantillaTienda';
import PanelNavLinks from './PanelNavLinks';

// Envoltorio común de los paneles protegidos. La tienda ya está en el store
// (vino con el token al hacer login/registro, o se restauró de la caché
// local al recargar la página): el panel usa solo la tienda del token, no
// vuelve a pedirla por slug.
export default function PanelLayout({ children }) {
  return (
    <PlantillaTienda navLinks={<PanelNavLinks />}>
      {children}
    </PlantillaTienda>
  );
}
