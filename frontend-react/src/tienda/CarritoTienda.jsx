import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { useStore } from '../store/store';
import './CarritoTienda.css';

export default function CarritoTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { carrito, quitarDelCarrito, cambiarCantidad, cliente } = useClienteStore();
  const { productos, getTienda, listProductos } = useStore();

  // Estado para controlar nuestro nuevo modal animado
  const [modalLogin, setModalLogin] = useState(false);
  useEffect(() => {
    getTienda(slug);
    if (productos.length === 0) listProductos();
  }, [slug, getTienda, listProductos, productos.length]);

  const total = carrito.reduce((sum, item) => sum + (Number(item.precio) * item.cantidad), 0);

  const irAlCheckout = () => {
    if (!cliente) {
      setModalLogin(true); // Activamos el modal en lugar del toast
      return;
    }
    navigate(`/tienda/${slug}/checkout`);
  };

  const navLinks = (
    <Link to={`/tienda/${slug}`} className="nav-volver">
      <svg className="icono-flecha" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"></line>
        <polyline points="12 19 5 12 12 5"></polyline>
      </svg>
      Seguir comprando
    </Link>
  );

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="carrito-page-bg">
        <div className="carrito-page">
          <div className="carrito-header">
            <h2>Tu Carrito</h2>
          </div>

          {carrito.length === 0 ? (
            <div className="carrito-vacio-card">
              <div className="icono-vacio">🛍️</div>
              <h3>Tu carrito está vacío</h3>
              <p>Explora nuestro catálogo y añade productos increíbles.</p>
              <Link to={`/tienda/${slug}`} className="btn-premium">
                Ver Catálogo
              </Link>
            </div>
          ) : (
            <div className="carrito-contenedor-grid">
              <div className="carrito-lista">
                {carrito.map((item) => {
                  const prodOriginal = productos.find((p) => p.idProducto === item.idProducto);
                  const stockMax = prodOriginal ? Number(prodOriginal.stock) : item.cantidad;

                  return (
                    <div key={item.idProducto} className="carrito-item-card">
                      <div className="item-info">
                        <h4>{item.nombre}</h4>
                        <span className="item-precio">
                          S/ {Number(item.precio).toFixed(2)} c/u · Stock máx: {stockMax}
                        </span>
                        <div className="item-controles-cantidad">
                          <button
                            type="button"
                            className="btn-qty"
                            onClick={() => cambiarCantidad(item.idProducto, item.cantidad - 1, stockMax)}
                          >
                            −
                          </button>
                          <span className="qty-valor">{item.cantidad}</span>
                          <button
                            type="button"
                            className="btn-qty"
                            onClick={() => cambiarCantidad(item.idProducto, item.cantidad + 1, stockMax)}
                          >
                            +
                          </button>
                        </div>
                      </div>
                      <div className="item-acciones">
                        <strong className="item-subtotal">
                          S/ {(Number(item.precio) * item.cantidad).toFixed(2)}
                        </strong>
                        <button
                          type="button"
                          className="btn-eliminar-minimal"
                          onClick={() => quitarDelCarrito(item.idProducto)}
                        >
                          Quitar
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="carrito-resumen-card">
                <h3>Resumen de compra</h3>
                <div className="resumen-fila total">
                  <span>Total estimado</span>
                  <span>S/ {total.toFixed(2)}</span>
                </div>
                <button type="button" className="btn-premium w-100" onClick={irAlCheckout}>
                  Proceder al Checkout
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {modalLogin && (
        <div className="modal-backdrop fade-in">
          <div className="modal-card slide-up">
            <div className="modal-icono">👋</div>
            <h3>Inicia sesión</h3>
            <p>Para proteger tus datos y emitir tu comprobante electrónico, ingresa a tu cuenta.</p>
            <div className="modal-acciones">
              <button type="button" className="btn-secundario" onClick={() => setModalLogin(false)}>
                Cancelar
              </button>
              <button type="button" className="btn-premium" onClick={() => navigate(`/tienda/${slug}/login`)}>
                Ir a Login
              </button>
            </div>
          </div>
        </div>
      )}
    </PlantillaTienda>
  );
}