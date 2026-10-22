import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  FileCode2,
  FileText,
  LoaderCircle,
  RefreshCw,
  Search,
} from 'lucide-react';
import { listarVentas, rutaComprobantePdf, rutaComprobanteXml } from '../api/ventasApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import PanelLayout from './PanelLayout';
import './HistorialVentas.css';

const TAMANIO_PAGINA = 10;

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});

const fechaHora = new Intl.DateTimeFormat('es-PE', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function formatearFecha(valor) {
  if (!valor) return '—';
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? valor : fechaHora.format(fecha);
}

function claseEstado(estado) {
  const normalizado = (estado || '').toLowerCase();
  if (normalizado === 'emitido' || normalizado === 'aceptado') return 'ok';
  if (normalizado === 'anulado' || normalizado === 'rechazado') return 'error';
  return 'neutro';
}

export default function HistorialVentas() {
  const [ventas, setVentas] = useState([]);
  const [texto, setTexto] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalElementos, setTotalElementos] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [descargando, setDescargando] = useState('');
  const solicitud = useRef(0);

  // La busqueda se aplica con un pequeno retardo para no consultar el backend
  // en cada tecla; al cambiar el filtro se vuelve a la primera pagina.
  useEffect(() => {
    const temporizador = setTimeout(() => {
      setBusqueda(texto.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(temporizador);
  }, [texto]);

  const cargar = useCallback(async () => {
    const actual = solicitud.current + 1;
    solicitud.current = actual;
    setCargando(true);
    setError('');

    try {
      const datos = await listarVentas({ pagina, tamanio: TAMANIO_PAGINA, busqueda });
      if (actual !== solicitud.current) return;
      setVentas(Array.isArray(datos?.contenido) ? datos.contenido : []);
      setTotalPaginas(Number(datos?.totalPaginas) || 0);
      setTotalElementos(Number(datos?.totalElementos) || 0);
    } catch (problema) {
      if (actual !== solicitud.current) return;
      setError(problema?.message || 'No se pudo cargar el historial de ventas.');
      setVentas([]);
      setTotalPaginas(0);
      setTotalElementos(0);
    } finally {
      if (actual === solicitud.current) setCargando(false);
    }
  }, [pagina, busqueda]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  async function descargar(venta, tipo) {
    const clave = `${venta.idVenta}-${tipo}`;
    setDescargando(clave);
    try {
      const ruta = tipo === 'PDF' ? rutaComprobantePdf(venta.idVenta) : rutaComprobanteXml(venta.idVenta);
      await descargarArchivoSeguro(ruta, tipo, venta.numeroDocumento);
      showToast(`${tipo} descargado correctamente.`);
    } catch (problema) {
      showToast(problema?.message || `No se pudo descargar el ${tipo}.`);
    } finally {
      setDescargando('');
    }
  }

  const hayVentas = ventas.length > 0;
  const primeraFila = totalElementos === 0 ? 0 : pagina * TAMANIO_PAGINA + 1;
  const ultimaFila = Math.min((pagina + 1) * TAMANIO_PAGINA, totalElementos);

  return (
    <PanelLayout>
      <main className="page historial-page">
        <header className="hist-cabecera">
          <div>
            <h1>Historial de ventas.</h1>
            <p>Consulta los comprobantes emitidos y descarga sus archivos.</p>
          </div>
          <button
            className="hist-btn actualizar"
            type="button"
            onClick={cargar}
            disabled={cargando}
          >
            <RefreshCw size={16} aria-hidden="true" className={cargando ? 'girando' : ''} />
            Actualizar
          </button>
        </header>

        <section className="hist-toolbar" aria-label="Buscar ventas">
          <div className="hist-buscador">
            <Search size={18} aria-hidden="true" />
            <input
              type="search"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder="Buscar por N° de comprobante o DNI del cliente..."
              aria-label="Buscar por número de comprobante o DNI del cliente"
            />
          </div>
          <span className="hist-contador" aria-live="polite">
            {totalElementos === 1 ? '1 venta' : `${totalElementos} ventas`}
          </span>
        </section>

        {error && (
          <div className="hist-error" role="alert">
            <p>{error}</p>
            <button className="hist-btn" type="button" onClick={cargar}>
              Reintentar
            </button>
          </div>
        )}

        <section className="hist-panel" aria-label="Listado de ventas">
          {!error && !hayVentas && (
            <p className="hist-vacio">
              {cargando
                ? 'Cargando ventas…'
                : busqueda
                  ? `Ninguna venta coincide con “${busqueda}”.`
                  : 'Aún no hay ventas registradas en tu tienda.'}
            </p>
          )}

          {hayVentas && (
            <div className={`hist-tabla-envoltura${cargando ? ' cargando' : ''}`}>
              <table className="hist-tabla">
                <thead>
                  <tr>
                    <th scope="col">N° Comprobante</th>
                    <th scope="col">Fecha</th>
                    <th scope="col">Cliente</th>
                    <th scope="col" className="numero">Total</th>
                    <th scope="col" className="acciones">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {ventas.map((venta) => (
                    <tr key={venta.idVenta}>
                      <td>
                        <div className="hist-comprobante">
                          <strong>{venta.numeroDocumento}</strong>
                          <span className={`hist-estado ${claseEstado(venta.estado)}`}>
                            {venta.estado || 'EMITIDO'}
                          </span>
                        </div>
                      </td>
                      <td className="hist-fecha">{formatearFecha(venta.fechaEmision)}</td>
                      <td>
                        <div className="hist-cliente">
                          <span>{venta.cliente || 'Cliente'}</span>
                          {venta.documentoCliente && <small>{venta.documentoCliente}</small>}
                        </div>
                      </td>
                      <td className="numero hist-total">{dinero.format(Number(venta.total) || 0)}</td>
                      <td>
                        <div className="hist-acciones">
                          <button
                            className="hist-btn-icono"
                            type="button"
                            title="Descargar PDF"
                            aria-label={`Descargar PDF de ${venta.numeroDocumento}`}
                            disabled={descargando === `${venta.idVenta}-PDF`}
                            onClick={() => descargar(venta, 'PDF')}
                          >
                            {descargando === `${venta.idVenta}-PDF`
                              ? <LoaderCircle size={17} className="girando" aria-hidden="true" />
                              : <FileText size={17} aria-hidden="true" />}
                          </button>
                          <button
                            className="hist-btn-icono"
                            type="button"
                            title="Descargar XML"
                            aria-label={`Descargar XML de ${venta.numeroDocumento}`}
                            disabled={descargando === `${venta.idVenta}-XML`}
                            onClick={() => descargar(venta, 'XML')}
                          >
                            {descargando === `${venta.idVenta}-XML`
                              ? <LoaderCircle size={17} className="girando" aria-hidden="true" />
                              : <FileCode2 size={17} aria-hidden="true" />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {hayVentas && totalPaginas > 1 && (
          <nav className="hist-paginacion" aria-label="Paginación del historial">
            <span className="hist-rango">
              {primeraFila}–{ultimaFila} de {totalElementos}
            </span>
            <div className="hist-pagina-botones">
              <button
                className="hist-btn-icono"
                type="button"
                aria-label="Página anterior"
                title="Página anterior"
                disabled={pagina <= 0 || cargando}
                onClick={() => setPagina((valor) => Math.max(0, valor - 1))}
              >
                <ChevronLeft size={18} aria-hidden="true" />
              </button>
              <span className="hist-pagina-actual">
                Página {pagina + 1} de {totalPaginas}
              </span>
              <button
                className="hist-btn-icono"
                type="button"
                aria-label="Página siguiente"
                title="Página siguiente"
                disabled={pagina + 1 >= totalPaginas || cargando}
                onClick={() => setPagina((valor) => valor + 1)}
              >
                <ChevronRight size={18} aria-hidden="true" />
              </button>
            </div>
          </nav>
        )}
      </main>
    </PanelLayout>
  );
}
