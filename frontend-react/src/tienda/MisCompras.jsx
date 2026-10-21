import { useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import './MisCompras.css';

export default function MisCompras() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { cliente } = useClienteStore();

  // Protegemos la ruta
  useEffect(() => {
    if (!cliente) {
      showToast('Debes iniciar sesión para ver tus compras.');
      navigate(`/tienda/${slug}/login`);
    }
  }, [cliente, navigate, slug]);

  // Historial simulado (Mock Data)
  const historialMock = [
    { id: '102938', fecha: '20 Oct 2026', total: 148.70, items: 3, estado: 'En camino' },
    { id: '102844', fecha: '05 Oct 2026', total: 59.00, items: 1, estado: 'Entregado' }
  ];

  const navLinks = <Link to={`/tienda/${slug}`}>← Volver a la Tienda</Link>;

  if (!cliente) return null;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="mis-compras-page">
        <div className="compras-header">
          <h2>Mis Compras</h2>
          <p>Hola, {cliente.nombre}. Este es tu historial de pedidos.</p>
        </div>

        <div className="lista-compras">
          {historialMock.map((compra) => (
            <div key={compra.id} className="compra-card">
              <div className="compra-top">
                <div>
                  <div className="compra-id">Pedido #{compra.id}</div>
                  <div className="compra-fecha">{compra.fecha}</div>
                </div>
                <div className={`estado-badge ${compra.estado.toLowerCase()}`}>
                  {compra.estado}
                </div>
              </div>
              <div className="compra-detalle">
                <div className="compra-articulos">
                  {compra.items} {compra.items === 1 ? 'artículo' : 'artículos'}
                </div>
                <div className="compra-total">
                  S/ {compra.total.toFixed(2)}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </PlantillaTienda>
  );
}