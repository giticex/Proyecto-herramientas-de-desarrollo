import { useState } from 'react';
import { useOutletContext, useParams, Link, useNavigate } from 'react-router-dom';
import { useClienteStore } from './clienteStore';
import { rutaTienda } from './rutasTienda';
import './CarritoTienda.css';

export default function CarritoTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { productos, carrito, catalogoListo, errorCatalogo } = useOutletContext();
  const { quitarDelCarrito, cambiarCantidad, cliente } = useClienteStore();
  const base = rutaTienda(slug);

  // Estado para controlar nuestro nuevo modal animado
  const [modalLogin, setModalLogin] = useState(false);

  const total = carrito.reduce((sum, item) => sum + (Number(item.precio) * item.cantidad), 0);

  const irAlCheckout = () => {
    if (!cliente?.token) {
      setModalLogin(true); // Activamos el modal en lugar del toast
      return;
    }
    navigate(base + '/checkout');
  };

  return (
    <>
      <main className="carrito-page-bg">
        <div className="carrito-page">
          <div className="carrito-header">
            <h1>Tu carrito</h1>
          </div>

          {!catalogoListo ? (
            <p className="portal-status" role="status">Cargando carrito…</p>
          ) : errorCatalogo ? (
            <p className="portal-status" role="alert">No se pudo cargar el catálogo de esta tienda. Recarga la página.</p>
          ) : carrito.length === 0 ? (
            <div className="carrito-vacio-card">
              <div className="icono-vacio">🛍️</div>
              <h3>Tu carrito está vacío</h3>
              <p>Explora nuestro catálogo y añade productos increíbles.</p>
              <Link to={base} className="btn">
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
                            aria-label={`Reducir cantidad de ${item.nombre}`}
                            onClick={() => cambiarCantidad(slug, item.idProducto, item.cantidad - 1, stockMax)}
                          >
                            −
                          </button>
                          <span className="qty-valor">{item.cantidad}</span>
                          <button
                            type="button"
                            className="btn-qty"
                            aria-label={`Aumentar cantidad de ${item.nombre}`}
                            onClick={() => cambiarCantidad(slug, item.idProducto, item.cantidad + 1, stockMax)}
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
                          onClick={() => quitarDelCarrito(slug, item.idProducto)}
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
                <button type="button" className="btn block" onClick={irAlCheckout}>
                  Proceder al Checkout
                </button>
              </div>
            </div>
          )}
        </div>
      </main>

      {modalLogin && (
        <div className="modal-backdrop fade-in">
          <div className="modal-card slide-up" role="dialog" aria-modal="true" aria-labelledby="carrito-login-titulo"
            onKeyDown={(event) => { if (event.key === 'Escape') setModalLogin(false); }}>
            <div className="modal-icono">👋</div>
            <h3 id="carrito-login-titulo">Inicia sesión</h3>
            <p>Para proteger tus datos y emitir tu comprobante electrónico, ingresa a tu cuenta.</p>
            <div className="modal-acciones">
              <button type="button" className="btn ghost" onClick={() => setModalLogin(false)}>
                Cancelar
              </button>
              <button type="button" className="btn" onClick={() => navigate(base + '/login', { state: { from: base + '/checkout' } })}>
                Iniciar sesión
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}