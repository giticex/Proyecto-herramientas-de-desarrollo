import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import ComprobanteImpresion from './ComprobanteImpresion';
import { listarComprobantesDeCliente } from '../api/comprobantesApi';
import { showToast } from '../components/toastBus';
import { descargarArchivoSeguro } from '../utils/descargas';
import './MisCompras.css';


/** Normaliza las compras guardadas en el navegador (mock) al formato del backend. */
export default function MisCompras() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { cliente } = useClienteStore();

  const [comprobantes, setComprobantes] = useState(null);
  const [seleccion, setSeleccion] = useState(null);

  // Protegemos la ruta
  useEffect(() => {
    if (!cliente) {
      showToast('Debes iniciar sesión para ver tus compras.');
      navigate(`/tienda/${slug}/login`);
    }
  }, [cliente, navigate, slug]);

  const idCliente = cliente?.idCliente;
  const token = cliente?.token;
  const clave = idCliente && token ? JSON.stringify([slug, idCliente, token]) : null;

  useEffect(() => {
    if (!clave) return undefined;
    let vigente = true;
    listarComprobantesDeCliente(String(idCliente), slug, token)
      .then((lista) => {
        if (vigente) setComprobantes({ clave, lista: Array.isArray(lista) ? lista : [], error: '' });
      })
      .catch((error) => {
        if (vigente) setComprobantes({ clave, lista: [], error: error?.message || 'No se pudieron cargar tus compras.' });
      });
    return () => { vigente = false; };
  }, [clave, idCliente, slug, token]);

  async function descargar(comprobante, tipo) {
    try {
      const ruta = '/comprobantes/' + encodeURIComponent(comprobante.idVenta) + '/' + tipo.toLowerCase();
      await descargarArchivoSeguro(ruta, tipo, comprobante.numeroDocumento, { token });
    } catch (error) {
      showToast(error?.message || 'No se pudo descargar el comprobante.');
    }
  }

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

  const resultado = comprobantes?.clave === clave ? comprobantes : null;
  const lista = resultado?.lista ?? [];
  const seleccionado = seleccion?.clave === clave ? seleccion?.comprobante : null;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="mis-compras-page">
        <div className="compras-header">
          <h2>Mis Compras y Comprobantes</h2>
          <p>
            Hola, <strong>{cliente.nombre}</strong>. Este es tu historial de boletas y facturas electrónicas.
          </p>
        </div>

        {!clave ? (
          <p role="alert">Vuelve a iniciar sesión para ver tus compras.</p>
        ) : !resultado ? (
          <p role="status">Cargando tus compras…</p>
        ) : resultado.error ? (
          <p role="alert">{resultado.error}</p>
        ) : lista.length === 0 ? (
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
                      {comprobante.estado === 'EMITIDO' ? 'Emitido' : comprobante.estado || 'Emitido'}
                    </span>
                  </div>

                  <div className="compra-meta">
                    <span>
                      <strong>Cliente / Razón Social:</strong> {comprobante.receptor?.razonSocial}
                    </span>
                    <span>
                      <strong>
                        {comprobante.receptor?.tipoDocumento === '1' ? 'RUC' : 'DNI'}:
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
                      onClick={() => setSeleccion({ clave, comprobante })}
                    >
                      Ver representación impresa
                    </button>
                    {tieneArchivo && (
                      <>
                        <button type="button" className="btn-link" onClick={() => descargar(comprobante, 'PDF')}>
                          PDF
                        </button>
                        <button type="button" className="btn-link" onClick={() => descargar(comprobante, 'XML')}>
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
        <ComprobanteImpresion comprobante={seleccionado} token={token} onClose={() => setSeleccion(null)} />
      )}
    </PlantillaTienda>
  );
}
