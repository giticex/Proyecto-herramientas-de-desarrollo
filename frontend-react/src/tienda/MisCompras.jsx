import { useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import './MisCompras.css';

export default function MisCompras() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { cliente, misCompras } = useClienteStore();

  // Protegemos la ruta
  useEffect(() => {
    if (!cliente) {
      showToast('Debes iniciar sesión para ver tus compras.');
      navigate(`/tienda/${slug}/login`);
    }
  }, [cliente, navigate, slug]);

  const navLinks = (
    <Link to={`/tienda/${slug}`} className="nav-volver">
      <svg className="icono-flecha" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"></line>
        <polyline points="12 19 5 12 12 5"></polyline>
      </svg>
      Volver a la Tienda
    </Link>
  );

  if (!cliente) return null;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="mis-compras-page">
        <div className="compras-header">
          <h2>Mis Compras y Comprobantes</h2>
          <p>Hola, <strong>{cliente.nombre}</strong>. Este es tu historial de boletas y facturas electrónicas.</p>
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
            {misCompras.map((compra) => {
              const cantidadArticulos = compra.items.reduce((acc, item) => acc + item.cantidad, 0);
              const fechaFormateada = new Date(compra.fecha).toLocaleDateString('es-PE', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
              });

              return (
                <article key={compra.idOrden} className="compra-card">
                  <div className="compra-top">
                    <div>
                      <div className="compra-id">
                        {compra.tipoComprobante === 'factura' ? 'Factura' : 'Boleta'} {compra.numeroComprobante}
                      </div>
                      <div className="compra-fecha">
                        Emitido el {fechaFormateada} · {compra.tiendaNombre}
                      </div>
                    </div>
                    <span className="estado-badge emitido">
                      Emitido / Pagado
                    </span>
                  </div>

                  <div className="compra-meta">
                    <span><strong>Cliente / Razón Social:</strong> {compra.razonSocial}</span>
                    <span><strong>{compra.tipoComprobante === 'factura' ? 'RUC' : 'DNI'}:</strong> {compra.documento}</span>
                    <span><strong>Envío:</strong> {compra.direccion}</span>
                  </div>

                  <div className="compra-detalle">
                    <div className="compra-articulos">
                      {cantidadArticulos} {cantidadArticulos === 1 ? 'artículo' : 'artículos'} · Pago con {compra.metodoPago.toUpperCase()}
                    </div>
                    <div className="compra-montos">
                      <span className="compra-igv">
                        Op. Gravada: S/ {Number(compra.opGravada).toFixed(2)} · IGV: S/ {Number(compra.igv).toFixed(2)}
                      </span>
                      <div className="compra-total">
                        S/ {Number(compra.total).toFixed(2)}
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