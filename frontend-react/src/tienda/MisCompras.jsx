import { useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import { ApiError } from '../api/ApiError';
import './MisCompras.css';

export default function MisCompras() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { cliente, misCompras, cargarMisCompras } = useClienteStore();

  // Protegemos la ruta y cargamos el historial real del backend.
  useEffect(() => {
    if (!cliente?.token) {
      showToast('Debes iniciar sesión para ver tus compras.');
      navigate(`/tienda/${slug}/login`);
      return;
    }
    cargarMisCompras().catch((error) => {
      // Si fue un 401, conAutenticacion ya mostró "tu sesión expiró"; no lo
      // pisamos con un mensaje genérico.
      if (!(error instanceof ApiError && error.status === 401)) {
        showToast('No se pudieron cargar tus compras.');
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cliente, slug]);

  const navLinks = (
    <Link to={`/tienda/${slug}`} className="nav-volver">
      <svg className="icono-flecha" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"></line>
        <polyline points="12 19 5 12 12 5"></polyline>
      </svg>
      Volver a la Tienda
    </Link>
  );

  if (!cliente?.token) return null;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="mis-compras-page">
        <div className="compras-header">
          <h2>Mis Compras y Comprobantes</h2>
          <p>Hola, <strong>{cliente.nombre || cliente.correo}</strong>. Este es tu historial de boletas y facturas electrónicas.</p>
        </div>

        {misCompras.length === 0 ? (
          <div className="compras-vacio">
            <h3>Aún no tienes compras registradas</h3>
            <p>Explora el catálogo y realiza tu primer pedido para ver tus comprobantes aquí.</p>
            <Link to={`/tienda/${slug}`} className="btn">
              Ir al Catálogo
            </Link>
          </div>
        ) : (
          <div className="lista-compras">
            {misCompras.map((venta) => {
              const cantidadArticulos = (venta.detalles || []).reduce((acc, item) => acc + item.cantidad, 0);
              const fechaFormateada = new Date(venta.fechaEmision).toLocaleDateString('es-PE', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
              });

              return (
                <article key={venta.id} className="compra-card">
                  <div className="compra-top">
                    <div>
                      <div className="compra-id">
                        {venta.tipoDocumento} {venta.numeroDocumento}
                      </div>
                      <div className="compra-fecha">
                        Emitido el {fechaFormateada}
                      </div>
                    </div>
                    <span className="estado-badge emitido">
                      Emitido / Pagado
                    </span>
                  </div>

                  <div className="compra-detalle">
                    <div className="compra-articulos">
                      {cantidadArticulos} {cantidadArticulos === 1 ? 'artículo' : 'artículos'} · Pago con {venta.medioPago}
                    </div>
                    <div className="compra-montos">
                      <div className="compra-total">
                        S/ {Number(venta.total).toFixed(2)}
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </PlantillaTienda>
  );
}
