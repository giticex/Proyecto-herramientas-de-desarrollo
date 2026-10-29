import { useParams, useLocation, Link, Navigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import './ConfirmacionCompra.css';

export default function ConfirmacionCompra() {
  const { slug } = useParams();
  const { state } = useLocation();

  // Sin los datos de la compra recién emitida (recarga o entrada directa por
  // URL) no hay comprobante real que mostrar: se vuelve al catálogo.
  if (!state?.numeroDocumento) {
    return <Navigate to={`/tienda/${slug}`} replace />;
  }

  const etiqueta = state.tipoComprobante === 'factura' ? 'Factura' : 'Boleta';

  return (
    <PlantillaTienda>
      <div className="confirmacion-page">
        <div className="confirmacion-card">
          <div className="icono-exito">🎉</div>
          <h2>¡Compra exitosa!</h2>
          <p>Tu pedido ha sido procesado correctamente.</p>
          <p>Te hemos enviado un correo con los detalles de tu comprobante.</p>

          <div className="numero-pedido">
            {etiqueta} {state.numeroDocumento}
          </div>
          {Number.isFinite(state.total) && (
            <p className="confirmacion-total">Total: S/ {state.total.toFixed(2)}</p>
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
