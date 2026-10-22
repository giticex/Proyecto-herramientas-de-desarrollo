import { useParams, useLocation, Link, Navigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import './ConfirmacionCompra.css';

export default function ConfirmacionCompra() {
  const { slug } = useParams();
  const { state } = useLocation();

  // Sin datos de una compra recién hecha (ej. se recargó la página o se
  // entró directo por URL) no hay nada real que mostrar: vuelve al catálogo.
  if (!state?.numeroDocumento) {
    return <Navigate to={`/tienda/${slug}`} replace />;
  }

  return (
    <PlantillaTienda>
      <div className="confirmacion-page">
        <div className="confirmacion-card">
          <div className="icono-exito">🎉</div>
          <h2>¡Compra exitosa!</h2>
          <p>Tu pedido ha sido procesado correctamente.</p>
          <p>Te hemos enviado un correo con los detalles de tu comprobante.</p>

          <div className="numero-pedido">
            {state.tipoComprobante === 'factura' ? 'Factura' : 'Boleta'} {state.numeroDocumento}
          </div>
          {typeof state.total === 'number' && (
            <p className="muted">Total: S/ {Number(state.total).toFixed(2)}</p>
          )}

          <div className="confirmacion-acciones">
            <Link to={`/tienda/${slug}/mis-compras`} className="btn">
              Ver mis compras
            </Link>
            <Link to={`/tienda/${slug}`} className="btn" style={{ background: '#e5e5ea', color: '#1d1d1f' }}>
              Seguir comprando
            </Link>
          </div>
        </div>
      </div>
    </PlantillaTienda>
  );
}
