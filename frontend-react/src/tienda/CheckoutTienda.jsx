import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { useStore } from '../store/store';
import { showToast } from '../components/toastBus';
import {
  emitirComprobante,
  construirPayloadComprobante,
  validarDatosFacturacion,
  MEDIO_PAGO,
  TIPO_COMPROBANTE,
  LIMITE_BOLETA_SIN_RUC,
} from '../api/comprobantesApi';
import './CheckoutTienda.css';

const IGV_PORCENTAJE = 0.18;

/**
 * Vista previa del desglose tributario. Es solo informacion para el cliente:
 * el backend recalcula los importes con el catalogo y es el que manda.
 */
function calcularResumen(carrito, productos) {
  let opGravada = 0;
  let opInafecta = 0;
  let opExonerada = 0;
  let opGratuita = 0;
  let igv = 0;
  let total = 0;

  const lineas = carrito.map((item) => {
    const producto = productos.find((p) => p.idProducto === item.idProducto);
    const afectacion = producto?.afectacionIgv || item.afectacionIgv || '01';
    const precio = Number(item.precio) || 0;
    const importe = precio * item.cantidad;
    total += importe;

    if (afectacion === '01') {
      const base = importe / (1 + IGV_PORCENTAJE);
      igv += importe - base;
      opGravada += base;
    } else if (afectacion === '02') {
      opInafecta += importe;
    } else if (afectacion === '03') {
      opExonerada += importe;
    } else {
      opGratuita += importe;
    }

    return { ...item, afectacion, importe };
  });

  return { lineas, opGravada, opInafecta, opExonerada, opGratuita, igv, total };
}

const centavos = (valor) => Number(valor).toFixed(2);

export default function CheckoutTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { carrito, cliente, registrarCompra } = useClienteStore();
  const { tienda, productos, getTienda, listProductos } = useStore();

  const [tipoComprobante, setTipoComprobante] = useState('boleta');
  const [documento, setDocumento] = useState('');
  const [nombreAdquirente, setNombreAdquirente] = useState('');
  const [domicilioFiscal, setDomicilioFiscal] = useState('');
  const [direccionEntrega, setDireccionEntrega] = useState('');
  const [metodoPago, setMetodoPago] = useState('tarjeta');
  const [procesando, setProcesando] = useState(false);

  // Proteger la ruta: si no hay carrito o cliente, regresar
  useEffect(() => {
    getTienda(slug);
    if (productos.length === 0) listProductos();
  }, [slug, getTienda, listProductos, productos.length]);

  // Si el carrito está vacío y NO estamos procesando una compra, volver al catálogo
  useEffect(() => {
    if (!procesando && carrito.length === 0) {
      navigate(`/tienda/${slug}`);
    }
  }, [carrito.length, procesando, navigate, slug]);

  const resumen = useMemo(() => calcularResumen(carrito, productos), [carrito, productos]);

  const correoCliente = cliente?.correo || cliente?.email || '';
  const nombreCliente = cliente?.nombre || 'Consumidor Final';

  const manejarCambioComprobante = (tipo) => {
    setTipoComprobante(tipo);
    setDocumento('');
    if (tipo === 'boleta') {
      // La boleta se emite a personas: se completa con los datos del cliente
      // que inicio sesion.
      setNombreAdquirente(nombreCliente === 'Consumidor Final' ? '' : nombreCliente);
      setDomicilioFiscal('');
    } else {
      setNombreAdquirente('');
    }
  };

  const manejarDocumento = (valor) => {
    const soloNumeros = valor.replace(/\D/g, '');
    setDocumento(soloNumeros);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const esFactura = tipoComprobante === 'factura';
    const { valido, errores } = validarDatosFacturacion({
      tipoComprobante: esFactura ? TIPO_COMPROBANTE.FACTURA : TIPO_COMPROBANTE.BOLETA,
      numeroDocumento: documento,
      nombreAdquirente,
      domicilioFiscal: esFactura ? domicilioFiscal : direccionEntrega,
      direccionEntrega,
      total: resumen.total,
    });

    if (!valido) {
      showToast(errores[0]);
      return;
    }

    setProcesando(true);

    try {
      const medio = MEDIO_PAGO[metodoPago] || MEDIO_PAGO.efectivo;

      // El backend es la fuente de verdad: se envian solo los identificadores
      // de los productos, los datos del adquirente y el medio de pago.
      const payload = construirPayloadComprobante({
        tipoComprobante: esFactura ? TIPO_COMPROBANTE.FACTURA : TIPO_COMPROBANTE.BOLETA,
        idCliente: cliente?.idCliente,
        numeroDocumento: documento,
        nombreAdquirente,
        correo: correoCliente,
        domicilioFiscal: esFactura ? domicilioFiscal : direccionEntrega,
        direccionEntrega,
        medioPago: medio,
        items: carrito.map((item) => ({
          idProducto: item.idProducto,
          cantidad: item.cantidad,
        })),
      });

      const comprobante = await emitirComprobante(payload);
      const totales = comprobante.totales || {};

      const nuevaOrden = {
        idOrden: comprobante.idVenta,
        numeroComprobante: comprobante.numeroDocumento,
        fecha: `${comprobante.fechaEmision}T${comprobante.horaEmision}`,
        tiendaSlug: slug || tienda.slug,
        tiendaNombre: tienda.nombre,
        clienteNombre: comprobante.receptor?.razonSocial || nombreCliente,
        clienteCorreo: correoCliente,
        tipoComprobante,
        documento: comprobante.receptor?.numeroDocumento || documento,
        razonSocial: comprobante.receptor?.razonSocial || nombreCliente,
        direccion: comprobante.entrega?.direccion || direccionEntrega.trim(),
        metodoPago: comprobante.mediosPago?.[0]?.descripcion || medio.descripcion,
        items: (comprobante.detalles || []).map((detalle) => ({
          idProducto: detalle.idProducto,
          nombre: detalle.descripcion,
          cantidad: Number(detalle.cantidad),
          precio: Number(detalle.precioUnitario),
          afectacionIgv: detalle.afectacionIgv,
        })),
        opGravada: Number(totales.opGravada || 0),
        igv: Number(totales.igv || 0),
        total: Number(totales.montoTotalPagar || 0),
        comprobante,
      };

      registrarCompra(nuevaOrden);
      // El backend descuenta el stock al emitir: se refresca el catalogo.
      listProductos();
      showToast(
        `¡${tipoComprobante === 'boleta' ? 'Boleta' : 'Factura'} ${nuevaOrden.numeroComprobante} emitida!`
      );
      navigate(`/tienda/${slug}/confirmacion`);
    } catch (error) {
      setProcesando(false);
      showToast(error?.message || 'Ocurrió un error al procesar tu compra.');
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

                <div className="field">
                  <label>
                    {tipoComprobante === 'boleta' ? 'Nombres y Apellidos' : 'Razón Social'}
                  </label>
                  <input
                    type="text"
                    placeholder={
                      tipoComprobante === 'boleta'
                        ? 'Ej. Juan Carlos Pérez'
                        : 'Ej. Inversiones Tecnológicas S.A.C.'
                    }
                    value={nombreAdquirente}
                    onChange={(e) => setNombreAdquirente(e.target.value)}
                  />
                </div>

                {tipoComprobante === 'factura' && (
                  <div className="field">
                    <label>Domicilio Fiscal (obligatorio)</label>
                    <input
                      type="text"
                      placeholder="Ej. Av. Javier Prado Este 4200, Lima"
                      value={domicilioFiscal}
                      onChange={(e) => setDomicilioFiscal(e.target.value)}
                    />
                  </div>
                )}

                {tipoComprobante === 'boleta' && (
                  <p className="checkout-nota">
                    Las boletas sin RUC admiten hasta S/ {LIMITE_BOLETA_SIN_RUC}.00 de importe total.
                  </p>
                )}

                {tipoComprobante === 'boleta' && resumen.total > LIMITE_BOLETA_SIN_RUC && (
                  <p className="checkout-alerta">
                    El importe supera S/ {LIMITE_BOLETA_SIN_RUC}.00: elige Factura para emitir el
                    comprobante con RUC, razón social y domicilio fiscal.
                  </p>
                )}

                <h3 style={{ marginTop: '16px' }}>2. Dirección y Método de Pago</h3>

                <div className="field">
                  <label>Dirección de Entrega</label>
                  <input
                    type="text"
                    placeholder="Ej. Av. Arequipa 1234, Dpto 302, Lima"
                    value={direccionEntrega}
                    onChange={(e) => setDireccionEntrega(e.target.value)}
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
                {resumen.lineas.map((item) => (
                  <div key={item.idProducto} className="mini-item">
                    <span>
                      {item.cantidad}x {item.nombre}
                      {item.afectacion !== '01' && (
                        <em className="mini-item-nota"> ({afectacionTexto(item.afectacion)})</em>
                      )}
                    </span>
                    <strong>S/ {centavos(item.importe)}</strong>
                  </div>
                ))}
              </div>

              <div className="desglose-tributario">
                {resumen.opGravada > 0 && (
                  <div className="desglose-fila">
                    <span>Op. Gravada (Subtotal)</span>
                    <span>S/ {centavos(resumen.opGravada)}</span>
                  </div>
                )}
                {resumen.opInafecta > 0 && (
                  <div className="desglose-fila">
                    <span>Op. Inafecta</span>
                    <span>S/ {centavos(resumen.opInafecta)}</span>
                  </div>
                )}
                {resumen.opExonerada > 0 && (
                  <div className="desglose-fila">
                    <span>Op. Exonerada</span>
                    <span>S/ {centavos(resumen.opExonerada)}</span>
                  </div>
                )}
                {resumen.opGratuita > 0 && (
                  <div className="desglose-fila">
                    <span>Op. Gratuitas</span>
                    <span>S/ {centavos(resumen.opGratuita)}</span>
                  </div>
                )}
                <div className="desglose-fila">
                  <span>I.G.V. (18%)</span>
                  <span>S/ {centavos(resumen.igv)}</span>
                </div>
                <div className="desglose-fila total">
                  <span>Total a Pagar</span>
                  <span>S/ {centavos(resumen.total)}</span>
                </div>
              </div>

              <button type="submit" className="btn-confirmar-pago" disabled={procesando}>
                {procesando ? 'Emitiendo comprobante...' : `Confirmar y Pagar S/ ${centavos(resumen.total)}`}
              </button>
            </div>
          </form>
        </div>
      </div>
    </PlantillaTienda>
  );
}

function afectacionTexto(afectacion) {
  if (afectacion === '02') return 'Inafecta';
  if (afectacion === '03') return 'Exonerada';
  if (afectacion === '04') return 'Gratuita';
  return 'Gravada';
}
