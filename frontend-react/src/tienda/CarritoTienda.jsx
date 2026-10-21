import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import './CarritoTienda.css';

export default function CarritoTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  // Asumimos que tu clienteStore tiene carrito, eliminarDelCarrito y vaciarCarrito
  const { carrito, eliminarDelCarrito, cliente } = useClienteStore();

  const total = carrito.reduce((sum, item) => sum + (Number(item.precio) * item.cantidad), 0);

  const irAlCheckout = () => {
    if (!cliente) {
      showToast('Debes iniciar sesión para procesar tu compra.');
      navigate(`/tienda/${slug}/login`);
      return;
    }
    navigate(`/tienda/${slug}/checkout`);
  };

  const navLinks = <Link to={`/tienda/${slug}`}>← Seguir comprando</Link>;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="carrito-page">
        <div className="carrito-header">
          <h2>Tu Carrito</h2>
        </div>

        {carrito.length === 0 ? (
          <div className="carrito-vacio">
            <h3>Tu carrito está vacío</h3>
            <p>Explora nuestro catálogo y añade productos increíbles.</p>
            <Link to={`/tienda/${slug}`} className="btn" style={{ marginTop: '16px', display: 'inline-block' }}>
              Ver Catálogo
            </Link>
          </div>
        ) : (
          <>
            <div className="carrito-lista">
              {carrito.map((item, index) => (
                <div key={index} className="carrito-item">
                  <div className="item-info">
                    <h4>{item.nombre}</h4>
                    <span className="item-precio">S/ {Number(item.precio).toFixed(2)} x {item.cantidad}</span>
                  </div>
                  <div className="item-acciones">
                    <strong>S/ {(Number(item.precio) * item.cantidad).toFixed(2)}</strong>
                    <button 
                      className="btn-eliminar"
                      onClick={() => eliminarDelCarrito(item.id)}
                    >
                      Quitar
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="carrito-resumen">
              <div className="resumen-total">
                Total a Pagar: S/ {total.toFixed(2)}
              </div>
              <button className="btn" onClick={irAlCheckout}>
                Proceder al Checkout
              </button>
            </div>
          </>
        )}
      </div>
    </PlantillaTienda>
  );
}