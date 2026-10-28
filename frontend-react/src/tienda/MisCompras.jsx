import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import ComprobanteImpresion from './ComprobanteImpresion';
import { listarComprobantesDeCliente } from '../api/comprobantesApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import './MisCompras.css';

const IGV_PORCENTAJE = 0.18;

/** Normaliza las compras guardadas en el navegador (mock) al formato del backend. */
function aComprobante(compra) {
  if (compra?.comprobante) return compra.comprobante;

  const items = compra?.items || [];
  const esFactura = compra?.tipoComprobante === 'factura';

  // El desglose se recalcula con la misma regla del backend: el precio del
  // catalogo incluye IGV, asi que la base es precio / (1 + IGV).
  const detalles = items.map((item, indice) => {
    const cantidad = Number(item.cantidad || 0);
    const precio = Number(item.precio || 0);
    const afectacion = item.afectacionIgv || '01';
    const gravada = afectacion === '01';
    const valorUnitario = gravada ? precio / (1 + IGV_PORCENTAJE) : precio;
    const base = valorUnitario * cantidad;
    const igv = gravada ? base * IGV_PORCENTAJE : 0;

    return {
      numero: indice + 1,
      descripcion: item.nombre,
      cantidad,
      unidadMedida: item.unidadMedida || 'NIU',
      valorUnitario,
      precioUnitario: precio,
      afectacionIgv: afectacion,
      base,
      igv,
      totalItem: base + igv,
    };
  });

  const suma = (predicado) =>
    detalles.filter(predicado).reduce((acc, detalle) => acc + detalle.base, 0);
  const opGravada = suma((detalle) => detalle.afectacionIgv === '01');
  const opInafecta = suma((detalle) => detalle.afectacionIgv === '02');
  const igv = detalles.reduce((acc, detalle) => acc + detalle.igv, 0);
  const total = opGravada + opInafecta + igv;

  return {
    idVenta: compra?.idOrden,
    tipoComprobante: esFactura ? '01' : '03',
    tipoComprobanteDescripcion: esFactura ? 'Factura' : 'Boleta',
    numeroDocumento: compra?.numeroComprobante,
    fechaEmision: (compra?.fecha || '').slice(0, 10),
    horaEmision: (compra?.fecha || '').slice(11, 19),
    moneda: 'PEN',
    estado: 'EMITIDO',
    receptor: {
      tipoDocumento: esFactura ? '6' : '1',
      numeroDocumento: compra?.documento,
      razonSocial: compra?.razonSocial,
      domicilioFiscal: { direccion: compra?.direccion, pais: 'PE' },
    },
    entrega: { direccion: compra?.direccion },
    detalles,
    totales: {
      opGravada,
      opInafecta,
      opExonerada: 0,
      opGratuita: 0,
      igv,
      totalDescuentos: 0,
      totalVenta: total,
      montoTotalPagar: total,
    },
    leyendas: [],
    mediosPago: [{ codigo: '', descripcion: compra?.metodoPago, monto: total }],
    hashComprobante: '',
  };
}

export default function MisCompras() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { cliente, misCompras } = useClienteStore();

  const [comprobantes, setComprobantes] = useState([]);
  const [seleccionado, setSeleccionado] = useState(null);
  const [descargando, setDescargando] = useState('');

  // Protegemos la ruta
  useEffect(() => {
    if (!cliente) {
      showToast('Debes iniciar sesión para ver tus compras.');
      navigate(`/tienda/${slug}/login`);
    }
  }, [cliente, navigate, slug]);

  // El backend es la fuente de verdad: se listan sus comprobantes del cliente.
  useEffect(() => {
    if (!cliente?.idCliente) return undefined;
    let vigente = true;

    listarComprobantesDeCliente(String(cliente.idCliente))
      .then((lista) => {
        if (vigente && Array.isArray(lista) && lista.length > 0) {
          setComprobantes(lista);
        }
      })
      .catch(() => {
        // Sin backend disponible se muestran las compras guardadas en el navegador.
      });

    return () => {
      vigente = false;
    };
  }, [cliente]);

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

  const lista = comprobantes.length > 0 ? comprobantes : misCompras.map(aComprobante);

  // Los endpoints de archivos exigen el Bearer token: se descargan con el
  // cliente HTTP y no con un enlace directo.
  async function descargar(comprobante, tipo) {
    const clave = `${comprobante.idVenta}-${tipo}`;
    setDescargando(clave);
    try {
      await descargarArchivoSeguro(
        `/comprobantes/${comprobante.idVenta}/${tipo.toLowerCase()}`,
        tipo,
        comprobante.numeroDocumento || comprobante.idVenta
      );
      showToast(`${tipo} descargado correctamente.`);
    } catch (error) {
      showToast(error?.message || `No se pudo descargar el ${tipo}.`);
    } finally {
      setDescargando('');
    }
  }

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="mis-compras-page">
        <div className="compras-header">
          <h2>Mis Compras y Comprobantes</h2>
          <p>
            Hola, <strong>{cliente.nombre}</strong>. Este es tu historial de boletas y facturas electrónicas.
          </p>
        </div>

        {lista.length === 0 ? (
          <div className="compras-vacio">
            <h3>Aún no tienes compras registradas</h3>
            <p>Explora el catálogo y realiza tu primer pedido para ver tus comprobantes aquí.</p>
            <Link to={`/tienda/${slug}`} className="btn">
              Ir al Catálogo
            </Link>
          </div>
        ) : (
          <div className="lista-compras">
            {lista.map((comprobante, indice) => {
              const detalles = comprobante.detalles || [];
              const totales = comprobante.totales || {};
              const cantidadArticulos = detalles.reduce(
                (acc, detalle) => acc + Number(detalle.cantidad || 0),
                0
              );
              const fechaFormateada = new Date(
                `${comprobante.fechaEmision}T${(comprobante.horaEmision || '').slice(0, 8) || '00:00:00'}`
              ).toLocaleDateString('es-PE', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
              });
              const tieneArchivo = comprobante.idVenta && !String(comprobante.idVenta).startsWith('ORD-');

              return (
                <article key={comprobante.idVenta || indice} className="compra-card">
                  <div className="compra-top">
                    <div>
                      <div className="compra-id">
                        {comprobante.tipoComprobanteDescripcion} {comprobante.numeroDocumento}
                      </div>
                      <div className="compra-fecha">
                        Emitido el {fechaFormateada}
                        {comprobante.horaEmision ? ` · ${comprobante.horaEmision}` : ''}
                      </div>
                    </div>
                    <span className="estado-badge emitido">
                      {comprobante.estado === 'EMITIDO' ? 'Emitido / Pagado' : comprobante.estado || 'Emitido'}
                    </span>
                  </div>

                  <div className="compra-meta">
                    <span>
                      <strong>Cliente / Razón Social:</strong> {comprobante.receptor?.razonSocial}
                    </span>
                    <span>
                      <strong>
                        {comprobante.receptor?.tipoDocumento === '6' ? 'RUC' : 'DNI'}:
                      </strong>{' '}
                      {comprobante.receptor?.numeroDocumento}
                    </span>
                    {comprobante.entrega?.direccion && (
                      <span>
                        <strong>Envío:</strong> {comprobante.entrega.direccion}
                      </span>
                    )}
                  </div>

                  <div className="compra-detalle">
                    <div className="compra-articulos">
                      {cantidadArticulos} {cantidadArticulos === 1 ? 'artículo' : 'artículos'} · Pago con{' '}
                      {(comprobante.mediosPago?.[0]?.descripcion || '').toUpperCase()}
                    </div>
                    <ul className="compra-desglose">
                      <li>
                        <span>Op. Gravada</span>
                        <span>S/ {Number(totales.opGravada || 0).toFixed(2)}</span>
                      </li>
                      {Number(totales.opInafecta || 0) > 0 && (
                        <li>
                          <span>Op. Inafecta</span>
                          <span>S/ {Number(totales.opInafecta || 0).toFixed(2)}</span>
                        </li>
                      )}
                      <li>
                        <span>I.G.V. (18%)</span>
                        <span>S/ {Number(totales.igv || 0).toFixed(2)}</span>
                      </li>
                      <li className="compra-desglose-total">
                        <span>Total</span>
                        <span>S/ {Number(totales.montoTotalPagar || 0).toFixed(2)}</span>
                      </li>
                    </ul>
                  </div>

                  <div className="compra-acciones">
                    <button
                      type="button"
                      className="btn-compra"
                      onClick={() => setSeleccionado(comprobante)}
                    >
                      Ver representación impresa
                    </button>
                    {tieneArchivo && (
                      <>
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => descargar(comprobante, 'PDF')}
                          disabled={descargando === `${comprobante.idVenta}-PDF`}
                        >
                          PDF
                        </button>
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => descargar(comprobante, 'XML')}
                          disabled={descargando === `${comprobante.idVenta}-XML`}
                        >
                          XML
                        </button>
                      </>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>

      {seleccionado && (
        <ComprobanteImpresion comprobante={seleccionado} onClose={() => setSeleccionado(null)} />
      )}
    </PlantillaTienda>
  );
}
