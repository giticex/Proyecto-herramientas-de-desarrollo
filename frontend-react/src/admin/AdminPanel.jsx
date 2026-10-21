import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useStore } from '../store/store';
import { useSesion } from '../auth/sesionStore';
import PlantillaTienda from '../components/PlantillaTienda';

// Dos marcas de ejemplo para probar que PlantillaTienda cambia el tema al
// instante (criterio de aceptación del bloque 1). Los paneles reales
// (productos, marca, etc.) van en el bloque 2.
const MARCA_A = {
  nombre: 'Moda Urbana',
  rubro: 'moda',
  slug: 'moda-urbana',
  colorPrimario: '#0071e3',
  colorSecundario: '#1d1d1f',
  logoUrl: '',
};

const MARCA_B = {
  nombre: 'Tech Store',
  rubro: 'tecnologia',
  slug: 'tech-store',
  colorPrimario: '#ff5a1f',
  colorSecundario: '#102a43',
  logoUrl: '',
};

export default function AdminPanel() {
  const tienda = useStore((estado) => estado.tienda);
  const getTienda = useStore((estado) => estado.getTienda);
  const saveTienda = useStore((estado) => estado.saveTienda);
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();

  useEffect(() => {
    getTienda();
    // Solo al montar: getTienda ya resuelve el slug actual del store.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function cerrarSesion() {
    logout();
    navigate('/admin/login');
  }

  return (
    <PlantillaTienda>
      <main className="page">
        <header className="head">
          <h1>{tienda.nombre}</h1>
          <p>Panel de administrador. Los paneles de productos y marca llegan en el bloque 2.</p>
        </header>

        <div className="section" style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <button className="btn secondary small" onClick={() => saveTienda(MARCA_A)}>Probar marca A</button>
          <button className="btn secondary small" onClick={() => saveTienda(MARCA_B)}>Probar marca B</button>
          <button className="btn danger small" onClick={cerrarSesion}>Cerrar sesión</button>
        </div>
      </main>
    </PlantillaTienda>
  );
}
