import { useParams, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import './ConfirmacionCompra.css';

export default function ConfirmacionCompra() {
  const { slug } = useParams();
  
  // Generamos un número de pedido aleatorio para simular la compra
  const numeroPedido = Math.floor(100000 + Math.random() * 900000);

  return (
    <PlantillaTienda>
      <div className="confirmacion-page">
        <div className="confirmacion-card">
          <div className="icono-exito">🎉</div>
          <h2>¡Compra exitosa!</h2>
          <p>Tu pedido ha sido procesado correctamente.</p>
          <p>Te hemos enviado un correo con los detalles de tu comprobante.</p>
          
          <div className="numero-pedido">
            Pedido #{numeroPedido}
          </div>

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