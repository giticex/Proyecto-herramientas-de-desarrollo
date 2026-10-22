import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { useStore } from '../store/store';
import { showToast } from '../components/toastBus';
import { ApiError } from '../api/ApiError';
import './CheckoutTienda.css';

export default function CheckoutTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { carrito, cliente, registrarVenta } = useClienteStore();
  const { tienda, productos, getTienda, listProductosTienda } = useStore();

  const [tipoComprobante, setTipoComprobante] = useState('boleta');
  const [documento, setDocumento] = useState('');
  const [razonSocial, setRazonSocial] = useState('');
  const [direccion, setDireccion] = useState('');
  const [metodoPago, setMetodoPago] = useState('tarjeta');
  const [procesando, setProcesando] = useState(false);

  // Proteger la ruta: si no hay carrito o cliente, regresar
  useEffect(() => {
    getTienda(slug);
   if (productos.length === 0) listProductosTienda(slug);
  }, [slug, getTienda, listProductosTienda, productos.length]);

  // Si el carrito está vacío y NO estamos procesando una compra, volver al catálogo
  useEffect(() => {
    if (!procesando && carrito.length === 0) {
      navigate(`/tienda/${slug}`);
    }
  }, [carrito.length, procesando, navigate, slug]);

  // Cálculos de Facturación Electrónica (IGV 18% incluido en el precio)
  const total = carrito.reduce((sum, item) => sum + (Number(item.precio) * item.cantidad), 0);
  const opGravada = total / 1.18;
  const igv = total - opGravada;

  const manejarCambioComprobante = (tipo) => {
    setTipoComprobante(tipo);
    setDocumento('');
    if (tipo === 'boleta') setRazonSocial('');
  };

  const manejarDocumento = (valor) => {
    const soloNumeros = valor.replace(/\D/g, '');
    setDocumento(soloNumeros);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (tipoComprobante === 'boleta' && documento.length !== 8) {
      showToast('Para Boleta Electrónica, el DNI debe tener 8 dígitos.');
      return;
    }

    if (tipoComprobante === 'factura') {
      if (documento.length !== 11 || (!documento.startsWith('10') && !documento.startsWith('20'))) {
        showToast('Ingresa un RUC válido de 11 dígitos (inicia con 10 o 20).');
        return;
      }
      if (razonSocial.trim().length < 3) {
        showToast('Ingresa la Razón Social de la empresa.');
        return;
      }
    }

    if (direccion.trim().length < 5) {
      showToast('Ingresa una dirección de entrega válida.');
      return;
    }

    setProcesando(true);

    try {
      // El stock se descuenta en el servidor, dentro de la misma
      // transacción que registra la venta (una sola llamada, no un loop).
      const carritoApi = {
        tipoDocumento: tipoComprobante === 'boleta' ? 'Boleta' : 'Factura',
        medioPago: metodoPago,
        items: carrito.map((item) => ({ idProducto: item.idProducto, cantidad: item.cantidad })),
      };
      const venta = await registrarVenta(carritoApi);

      showToast(`¡${tipoComprobante === 'boleta' ? 'Boleta' : 'Factura'} ${venta.numeroDocumento} emitida!`);
      navigate(`/tienda/${slug}/confirmacion`, {
        state: {
          idVenta: venta.id,
          numeroDocumento: venta.numeroDocumento,
          total: venta.total,
          // Detalle tributario que el backend no guarda: solo para esta
          // pantalla de confirmación, no viaja con la venta.
          tipoComprobante,
          documento,
          razonSocial: tipoComprobante === 'factura' ? razonSocial.trim() : cliente?.nombre || cliente?.correo || 'Cliente',
          direccion: direccion.trim(),
          metodoPago,
          opGravada: Number(opGravada.toFixed(2)),
          igv: Number(igv.toFixed(2)),
        },
      });
    } catch (error) {
      setProcesando(false);
      if (error instanceof ApiError && error.status === 409) {
        showToast(error.message || 'No hay stock suficiente para completar la compra.');
      } else if (error instanceof ApiError) {
        showToast(error.message);
      } else {
        showToast('Ocurrió un error al procesar tu compra.');
      }
    }
  };

  const navLinks = (
    <Link to={`/tienda/${slug}/carrito`} className="nav-volver">
      <svg className="icono-flecha" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"></line>
        <polyline points="12 19 5 12 12 5"></polyline>
      </svg>
      Volver al Carrito
    </Link>
  );

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="checkout-page-bg">
        <div className="checkout-container">
          <div className="checkout-header">
            <h2>Finalizar Compra y Facturación</h2>
            <p>Completa tus datos tributarios y de envío para emitir tu comprobante electrónico.</p>
          </div>

          <form className="checkout-grid" onSubmit={handleSubmit} noValidate>
            {/* COLUMNA IZQUIERDA: FORMULARIO TRIBUTARIO Y ENVÍO */}
            <div className="checkout-card">
              <h3>1. Comprobante Electrónico</h3>
              <div className="comprobante-selector">
                <button
                  type="button"
                  className={`comprobante-btn ${tipoComprobante === 'boleta' ? 'activo' : ''}`}
                  onClick={() => manejarCambioComprobante('boleta')}
                >
                  <strong>Boleta de Venta</strong>
                  <span>Con DNI (Personas)</span>
                </button>
                <button
                  type="button"
                  className={`comprobante-btn ${tipoComprobante === 'factura' ? 'activo' : ''}`}
                  onClick={() => manejarCambioComprobante('factura')}
                >
                  <strong>Factura Comercial</strong>
                  <span>Con RUC (Empresas)</span>
                </button>
              </div>

              <div className="checkout-fields">
                <div className="field">
                  <label>{tipoComprobante === 'boleta' ? 'Número de DNI (8 dígitos)' : 'Número de RUC (11 dígitos)'}</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder={tipoComprobante === 'boleta' ? 'Ej. 74859612' : 'Ej. 20556677881'}
                    value={documento}
                    onChange={(e) => manejarDocumento(e.target.value)}
                    maxLength={tipoComprobante === 'boleta' ? 8 : 11}
                  />
                </div>

                {tipoComprobante === 'factura' && (
                  <div className="field">
                    <label>Razón Social</label>
                    <input
                      type="text"
                      placeholder="Ej. Inversiones Tecnológicas S.A.C."
                      value={razonSocial}
                      onChange={(e) => setRazonSocial(e.target.value)}
                    />
                  </div>
                )}

                <h3 style={{ marginTop: '16px' }}>2. Dirección y Método de Pago</h3>

                <div className="field">
                  <label>Dirección de Entrega</label>
                  <input
                    type="text"
                    placeholder="Ej. Av. Arequipa 1234, Dpto 302, Lima"
                    value={direccion}
                    onChange={(e) => setDireccion(e.target.value)}
                  />
                </div>

                <div className="field">
                  <label>Método de Pago</label>
                  <div className="pago-opciones">
                    <label className={`pago-chip ${metodoPago === 'tarjeta' ? 'activo' : ''}`}>
                      <input
                        type="radio"
                        name="metodoPago"
                        checked={metodoPago === 'tarjeta'}
                        onChange={() => setMetodoPago('tarjeta')}
                      />
                      💳 Tarjeta Débito / Crédito
                    </label>
                    <label className={`pago-chip ${metodoPago === 'yape' ? 'activo' : ''}`}>
                      <input
                        type="radio"
                        name="metodoPago"
                        checked={metodoPago === 'yape'}
                        onChange={() => setMetodoPago('yape')}
                      />
                      📱 Yape / Plin
                    </label>
                  </div>
                </div>
              </div>
            </div>

            {/* COLUMNA DERECHA: DESGLOSE TRIBUTARIO Y CONFIRMACIÓN */}
            <div className="checkout-resumen-card">
              <h3>Resumen Tributario</h3>
              <div className="checkout-items-mini">
                {carrito.map((item) => (
                  <div key={item.idProducto} className="mini-item">
                    <span>{item.cantidad}x {item.nombre}</span>
                    <strong>S/ {(item.precio * item.cantidad).toFixed(2)}</strong>
                  </div>
                ))}
              </div>

              <div className="desglose-tributario">
                <div className="desglose-fila">
                  <span>Op. Gravada (Subtotal)</span>
                  <span>S/ {opGravada.toFixed(2)}</span>
                </div>
                <div className="desglose-fila">
                  <span>I.G.V. (18%)</span>
                  <span>S/ {igv.toFixed(2)}</span>
                </div>
                <div className="desglose-fila total">
                  <span>Total a Pagar</span>
                  <span>S/ {total.toFixed(2)}</span>
                </div>
              </div>

              <button type="submit" className="btn-confirmar-pago" disabled={procesando}>
                {procesando ? 'Emitiendo comprobante...' : `Confirmar y Pagar S/ ${total.toFixed(2)}`}
              </button>
            </div>
          </form>
        </div>
      </div>
    </PlantillaTienda>
  );
}

  