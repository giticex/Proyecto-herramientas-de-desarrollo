import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Banknote,
  Building2,
  ChevronLeft,
  ChevronRight,
  FileCode2,
  FileText,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Package,
  RefreshCw,
  Search,
  ShieldCheck,
  ShoppingBag,
  TrendingUp,
  TriangleAlert,
} from 'lucide-react';
import {
  listarComprobantesEmpresa,
  listarEmpresas,
  listarProductosEmpresa,
  obtenerDashboardEmpresa,
  obtenerResumenGlobal,
} from '../api/superAdminApi';
import { rutaComprobantePdf, rutaComprobanteXml } from '../api/ventasApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import { useSesion } from '../auth/sesionStore';
import './SuperAdminPanel.css';

const TAMANIO_COMPROBANTES = 8;

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});
const entero = new Intl.NumberFormat('es-PE');
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
  if (estado === 'Operativa') return 'ok';
  if (estado === 'Stock agotado') return 'alerta';
  if (estado === 'Sin catálogo') return 'error';
  return 'neutro';
}

function mensajeDe(problema, porDefecto) {
  return problema?.message || porDefecto;
}

function Kpi({ icono, etiqueta, valor, detalle }) {
  return (
    <article className="sa-kpi">
      <span className="sa-kpi-icono" aria-hidden="true">{icono}</span>
      <div>
        <p className="sa-kpi-etiqueta">{etiqueta}</p>
        <p className="sa-kpi-valor">{valor}</p>
        <p className="sa-kpi-detalle">{detalle}</p>
      </div>
    </article>
  );
}

const PESTANIAS = [
  { id: 'dashboard', etiqueta: 'Dashboard', icono: LayoutDashboard },
  { id: 'productos', etiqueta: 'Productos', icono: Package },
  { id: 'comprobantes', etiqueta: 'Comprobantes', icono: FileText },
];

export default function SuperAdminPanel() {
  const usuario = useSesion((estado) => estado.usuario);
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();

  const [refresco, setRefresco] = useState(0);
  const [resumen, setResumen] = useState(null);
  const [empresas, setEmpresas] = useState([]);
  const [seleccionada, setSeleccionada] = useState(null);
  const [cargandoGlobal, setCargandoGlobal] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState('');

  const [detalle, setDetalle] = useState(null);
  const [productos, setProductos] = useState([]);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);
  const [errorDetalle, setErrorDetalle] = useState('');

  const [pestania, setPestania] = useState('dashboard');
  const [texto, setTexto] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [comprobantes, setComprobantes] = useState({
    contenido: [],
    totalPaginas: 0,
    totalElementos: 0,
  });
  const [cargandoComprobantes, setCargandoComprobantes] = useState(false);
  const [errorComprobantes, setErrorComprobantes] = useState('');
  const [descargando, setDescargando] = useState('');

  useEffect(() => {
    let vigente = true;
    setCargandoGlobal(true);
    setErrorGlobal('');
    Promise.all([obtenerResumenGlobal(), listarEmpresas()])
      .then(([datosResumen, lista]) => {
        if (!vigente) return;
        setResumen(datosResumen || null);
        const seguras = Array.isArray(lista) ? lista : [];
        setEmpresas(seguras);
        setSeleccionada((actual) => (
          seguras.some((empresa) => empresa.idTienda === actual)
            ? actual
            : (seguras[0]?.idTienda ?? null)
        ));
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorGlobal(mensajeDe(problema, 'No se pudo cargar la consola global.'));
        setResumen(null);
        setEmpresas([]);
        setSeleccionada(null);
      })
      .finally(() => {
        if (vigente) setCargandoGlobal(false);
      });
    return () => { vigente = false; };
  }, [refresco]);

  useEffect(() => {
    if (!seleccionada) return undefined;
    let vigente = true;
    setCargandoDetalle(true);
    setErrorDetalle('');
    Promise.all([
      obtenerDashboardEmpresa(seleccionada),
      listarProductosEmpresa(seleccionada),
    ])
      .then(([datosDashboard, listaProductos]) => {
        if (!vigente) return;
        setDetalle(datosDashboard || null);
        setProductos(Array.isArray(listaProductos) ? listaProductos : []);
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorDetalle(mensajeDe(problema, 'No se pudo cargar el detalle de la empresa.'));
        setDetalle(null);
        setProductos([]);
      })
      .finally(() => {
        if (vigente) setCargandoDetalle(false);
      });
    return () => { vigente = false; };
  }, [seleccionada, refresco]);

  useEffect(() => {
    if (!seleccionada) return undefined;
    let vigente = true;
    setCargandoComprobantes(true);
    setErrorComprobantes('');
    listarComprobantesEmpresa(seleccionada, {
      pagina,
      tamanio: TAMANIO_COMPROBANTES,
      busqueda,
    })
      .then((datos) => {
        if (!vigente) return;
        setComprobantes({
          contenido: Array.isArray(datos?.contenido) ? datos.contenido : [],
          totalPaginas: Number(datos?.totalPaginas) || 0,
          totalElementos: Number(datos?.totalElementos) || 0,
        });
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorComprobantes(mensajeDe(problema, 'No se pudieron cargar los comprobantes.'));
        setComprobantes({ contenido: [], totalPaginas: 0, totalElementos: 0 });
      })
      .finally(() => {
        if (vigente) setCargandoComprobantes(false);
      });
    return () => { vigente = false; };
  }, [seleccionada, pagina, busqueda, refresco]);

  useEffect(() => {
    const temporizador = setTimeout(() => {
      setBusqueda(texto.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(temporizador);
  }, [texto]);

  const empresa = detalle?.empresa
    ?? empresas.find((item) => item.idTienda === seleccionada)
    ?? null;
  const topProductos = detalle?.topProductos ?? [];
  const maximoVendido = useMemo(
    () => Math.max(...(detalle?.topProductos ?? []).map((producto) => Number(producto.cantidad) || 0), 1),
    [detalle],
  );

  function seleccionarEmpresa(idTienda) {
    if (idTienda === seleccionada) return;
    setSeleccionada(idTienda);
    setDetalle(null);
    setProductos([]);
    setComprobantes({ contenido: [], totalPaginas: 0, totalElementos: 0 });
    setPestania('dashboard');
    setTexto('');
    setBusqueda('');
    setPagina(0);
  }

  function salir() {
    logout();
    navigate('/admin/login', { replace: true });
  }

  async function descargar(venta, tipo) {
    const clave = `${venta.idVenta}-${tipo}`;
    setDescargando(clave);
    try {
      const ruta = tipo === 'PDF'
        ? rutaComprobantePdf(venta.idVenta)
        : rutaComprobanteXml(venta.idVenta);
      await descargarArchivoSeguro(ruta, tipo, venta.numeroDocumento);
      showToast(`${tipo} descargado correctamente.`);
    } catch (problema) {
      showToast(problema?.message || `No se pudo descargar el ${tipo}.`);
    } finally {
      setDescargando('');
    }
  }

  const primeraFila = comprobantes.totalElementos === 0
    ? 0
    : pagina * TAMANIO_COMPROBANTES + 1;
  const ultimaFila = Math.min(
    (pagina + 1) * TAMANIO_COMPROBANTES,
    comprobantes.totalElementos,
  );

  return (
    <div className="sa-shell">
      <header className="sa-top">
        <div className="sa-marca">
          <span className="sa-marca-icono" aria-hidden="true"><ShieldCheck size={20} /></span>
          <span className="sa-marca-texto">
            <strong>NovaCommerce</strong>
            <small>Consola global</small>
          </span>
          <span className="sa-rol">SUPER ADMIN</span>
        </div>
        <div className="sa-cuenta">
          <span className="sa-cuenta-usuario">{usuario}</span>
          <button className="sa-salir" type="button" onClick={salir}>
            <LogOut size={16} aria-hidden="true" />
            Salir
          </button>
        </div>
      </header>

      <main className="sa-main">
        <header className="sa-cabecera">
          <div>
            <h1>Panel de la plataforma</h1>
            <p>Supervisa las empresas, sus ventas y sus comprobantes desde un solo lugar.</p>
          </div>
          <button
            className="sa-btn actualizar"
            type="button"
            onClick={() => setRefresco((valor) => valor + 1)}
            disabled={cargandoGlobal}
          >
            <RefreshCw size={16} aria-hidden="true" className={cargandoGlobal ? 'girando' : ''} />
            Actualizar
          </button>
        </header>

        {errorGlobal && (
          <div className="sa-error" role="alert">
            <TriangleAlert size={18} aria-hidden="true" />
            <p>{errorGlobal}</p>
            <button className="sa-btn" type="button" onClick={() => setRefresco((v) => v + 1)}>
              Reintentar
            </button>
          </div>
        )}

        {cargandoGlobal && !resumen && (
          <p className="sa-cargando">
            <LoaderCircle size={18} className="girando" aria-hidden="true" />
            Cargando métricas de la plataforma…
          </p>
        )}

        {resumen && (
          <section className="sa-kpis" aria-label="Métricas globales">
            <Kpi
              icono={<Building2 size={18} />}
              etiqueta="Empresas"
              valor={entero.format(resumen.totalEmpresas)}
              detalle="Registradas en la plataforma"
            />
            <Kpi
              icono={<ShoppingBag size={18} />}
              etiqueta="Ventas totales"
              valor={entero.format(resumen.totalVentas)}
              detalle="Comprobantes emitidos"
            />
            <Kpi
              icono={<TrendingUp size={18} />}
              etiqueta="Ingresos de hoy"
              valor={dinero.format(Number(resumen.ingresoHoy) || 0)}
              detalle="Todas las empresas"
            />
            <Kpi
              icono={<Banknote size={18} />}
              etiqueta="Ingresos del mes"
              valor={dinero.format(Number(resumen.ingresoMes) || 0)}
              detalle="Mes en curso"
            />
            <Kpi
              icono={<FileText size={18} />}
              etiqueta="Ingresos históricos"
              valor={dinero.format(Number(resumen.ingresoTotal) || 0)}
              detalle="Acumulado de la plataforma"
            />
            <Kpi
              icono={<Package size={18} />}
              etiqueta="Productos"
              valor={entero.format(resumen.totalProductos)}
              detalle={`${entero.format(resumen.productosSinStock)} sin stock`}
            />
          </section>
        )}

        <div className="sa-grid">
          <section className="sa-empresas" aria-label="Empresas registradas">
            <header className="sa-panel-cabecera">
              <div>
                <h2>Empresas</h2>
                <p className="sa-panel-sub">Selecciona una para inspeccionarla.</p>
              </div>
              <span className="sa-contador">{entero.format(empresas.length)}</span>
            </header>

            {!cargandoGlobal && empresas.length === 0 && !errorGlobal && (
              <p className="sa-vacio">No hay empresas registradas todavía.</p>
            )}

            <ul className="sa-lista-empresas">
              {empresas.map((item) => {
                const activa = item.idTienda === seleccionada;
                return (
                  <li key={item.idTienda}>
                    <button
                      className={`sa-empresa${activa ? ' activa' : ''}`}
                      type="button"
                      aria-pressed={activa}
                      onClick={() => seleccionarEmpresa(item.idTienda)}
                    >
                      <span
                        className="sa-empresa-avatar"
                        aria-hidden="true"
                        style={{ background: item.colorPrimario || '#2563eb' }}
                      >
                        {(item.nombre || '?').charAt(0).toUpperCase()}
                      </span>
                      <span className="sa-empresa-info">
                        <strong>{item.nombre}</strong>
                        <small>{item.rubro || 'Sin rubro'} · {item.slug}</small>
                      </span>
                      <span className="sa-empresa-metricas">
                        <strong>{dinero.format(Number(item.ventasTotales) || 0)}</strong>
                        <small>{entero.format(item.totalVentas)} comprobantes</small>
                      </span>
                      <span className={`sa-estado ${claseEstado(item.estado)}`}>{item.estado}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </section>

          <section className="sa-detalle" aria-label="Detalle de la empresa seleccionada">
            {!seleccionada && !cargandoGlobal && (
              <p className="sa-vacio">Elige una empresa para ver su dashboard, productos y comprobantes.</p>
            )}

            {seleccionada && (
              <>
                <header className="sa-detalle-cabecera">
                  <div className="sa-detalle-titulo">
                    <span
                      className="sa-empresa-avatar grande"
                      aria-hidden="true"
                      style={{ background: empresa?.colorPrimario || '#2563eb' }}
                    >
                      {(empresa?.nombre || '?').charAt(0).toUpperCase()}
                    </span>
                    <div>
                      <h2>{empresa?.nombre || 'Empresa'}</h2>
                      <p className="sa-panel-sub">
                        {empresa?.rubro || 'Sin rubro'} · {empresa?.slug}
                      </p>
                    </div>
                  </div>
                  {empresa && (
                    <div className="sa-detalle-badges">
                      <span className={`sa-estado ${claseEstado(empresa.estado)}`}>{empresa.estado}</span>
                      <span className="sa-detalle-dato">
                        Última venta: <strong>{formatearFecha(empresa.ultimaVenta)}</strong>
                      </span>
                    </div>
                  )}
                </header>

                <div className="sa-tabs" role="tablist" aria-label="Secciones de la empresa">
                  {PESTANIAS.map(({ id, etiqueta, icono: Icono }) => (
                    <button
                      key={id}
                      id={`sa-tab-${id}`}
                      className={`sa-tab${pestania === id ? ' activa' : ''}`}
                      type="button"
                      role="tab"
                      aria-selected={pestania === id}
                      aria-controls={`sa-panel-${id}`}
                      onClick={() => setPestania(id)}
                    >
                      <Icono size={16} aria-hidden="true" />
                      {etiqueta}
                    </button>
                  ))}
                </div>

                {errorDetalle && (
                  <div className="sa-error compacto" role="alert">
                    <TriangleAlert size={16} aria-hidden="true" />
                    <p>{errorDetalle}</p>
                    <button className="sa-btn" type="button" onClick={() => setRefresco((v) => v + 1)}>
                      Reintentar
                    </button>
                  </div>
                )}

                {cargandoDetalle && !detalle && (
                  <p className="sa-cargando">
                    <LoaderCircle size={16} className="girando" aria-hidden="true" />
                    Cargando empresa…
                  </p>
                )}

                {pestania === 'dashboard' && detalle && (
                  <div
                    className="sa-tab-panel"
                    id="sa-panel-dashboard"
                    role="tabpanel"
                    aria-labelledby="sa-tab-dashboard"
                  >
                    <div className="sa-detalle-kpis">
                      <div>
                        <span>Ventas de hoy</span>
                        <strong>{dinero.format(Number(empresa?.ventasHoy) || 0)}</strong>
                      </div>
                      <div>
                        <span>Ingresos del mes</span>
                        <strong>{dinero.format(Number(empresa?.ventasMes) || 0)}</strong>
                      </div>
                      <div>
                        <span>Ventas totales</span>
                        <strong>{dinero.format(Number(empresa?.ventasTotales) || 0)}</strong>
                      </div>
                      <div>
                        <span>Comprobantes</span>
                        <strong>{entero.format(empresa?.totalVentas || 0)}</strong>
                      </div>
                      <div>
                        <span>Productos</span>
                        <strong>{entero.format(empresa?.totalProductos || 0)}</strong>
                      </div>
                      <div>
                        <span>Sin stock</span>
                        <strong>{entero.format(empresa?.productosSinStock || 0)}</strong>
                      </div>
                    </div>

                    <h3 className="sa-subtitulo">Top 5 productos vendidos</h3>
                    {topProductos.length === 0 && (
                      <p className="sa-vacio">Esta empresa todavía no registra ventas.</p>
                    )}
                    {topProductos.length > 0 && (
                      <ul className="sa-top-productos">
                        {topProductos.map((producto) => {
                          const cantidad = Number(producto.cantidad) || 0;
                          return (
                            <li key={producto.idProducto}>
                              <span className="sa-top-nombre">{producto.nombre}</span>
                              <span className="sa-top-barra" aria-hidden="true">
                                <span style={{ width: `${Math.max(6, (cantidad / maximoVendido) * 100)}%` }} />
                              </span>
                              <span className="sa-top-cantidad">
                                {entero.format(cantidad)} u.
                              </span>
                              <span className="sa-top-total">{dinero.format(Number(producto.total) || 0)}</span>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                  </div>
                )}

                {pestania === 'productos' && (
                  <div
                    className="sa-tab-panel"
                    id="sa-panel-productos"
                    role="tabpanel"
                    aria-labelledby="sa-tab-productos"
                  >
                    {productos.length === 0 && !cargandoDetalle && (
                      <p className="sa-vacio">Esta empresa no tiene productos registrados.</p>
                    )}
                    {productos.length > 0 && (
                      <div className="sa-tabla-envoltura">
                        <table className="sa-tabla">
                          <thead>
                            <tr>
                              <th scope="col">Producto</th>
                              <th scope="col">Categoría</th>
                              <th scope="col" className="numero">Stock</th>
                              <th scope="col" className="numero">Precio</th>
                              <th scope="col">IGV</th>
                            </tr>
                          </thead>
                          <tbody>
                            {productos.map((producto) => (
                              <tr key={producto.idProducto}>
                                <td>
                                  <div className="sa-producto">
                                    <strong>{producto.nombre}</strong>
                                    <small>{producto.codigo || `#${producto.idProducto}`}</small>
                                  </div>
                                </td>
                                <td>{producto.categoria || '—'}</td>
                                <td className="numero">
                                  <span className={`sa-stock${Number(producto.stock) <= 0 ? ' agotado' : ''}`}>
                                    {entero.format(Number(producto.stock) || 0)}
                                  </span>
                                </td>
                                <td className="numero">{dinero.format(Number(producto.precio) || 0)}</td>
                                <td className="sa-igv">{producto.afectacionIgv || '01'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}

                {pestania === 'comprobantes' && (
                  <div
                    className="sa-tab-panel"
                    id="sa-panel-comprobantes"
                    role="tabpanel"
                    aria-labelledby="sa-tab-comprobantes"
                  >
                    <div className="sa-buscador">
                      <Search size={16} aria-hidden="true" />
                      <input
                        type="search"
                        value={texto}
                        onChange={(evento) => setTexto(evento.target.value)}
                        placeholder="Buscar por comprobante, DNI o razón social…"
                        aria-label="Buscar comprobantes de la empresa"
                      />
                      <span className="sa-contador">
                        {entero.format(comprobantes.totalElementos)} comprobantes
                      </span>
                    </div>

                    {errorComprobantes && (
                      <div className="sa-error compacto" role="alert">
                        <TriangleAlert size={16} aria-hidden="true" />
                        <p>{errorComprobantes}</p>
                      </div>
                    )}

                    {!errorComprobantes && comprobantes.contenido.length === 0 && (
                      <p className="sa-vacio">
                        {cargandoComprobantes
                          ? 'Cargando comprobantes…'
                          : busqueda
                            ? `Ningún comprobante coincide con “${busqueda}”.`
                            : 'Esta empresa todavía no emitió comprobantes.'}
                      </p>
                    )}

                    {comprobantes.contenido.length > 0 && (
                      <div className={`sa-tabla-envoltura${cargandoComprobantes ? ' cargando' : ''}`}>
                        <table className="sa-tabla">
                          <thead>
                            <tr>
                              <th scope="col">Comprobante</th>
                              <th scope="col">Fecha</th>
                              <th scope="col">Cliente</th>
                              <th scope="col" className="numero">Total</th>
                              <th scope="col" className="acciones">Archivos</th>
                            </tr>
                          </thead>
                          <tbody>
                            {comprobantes.contenido.map((venta) => (
                              <tr key={venta.idVenta}>
                                <td>
                                  <div className="sa-comprobante">
                                    <strong>{venta.numeroDocumento}</strong>
                                    <small>{venta.estado || 'EMITIDO'}</small>
                                  </div>
                                </td>
                                <td className="sa-fecha">{formatearFecha(venta.fechaEmision)}</td>
                                <td>
                                  <div className="sa-producto">
                                    <span>{venta.cliente || 'Cliente'}</span>
                                    {venta.documentoCliente && <small>{venta.documentoCliente}</small>}
                                  </div>
                                </td>
                                <td className="numero sa-total">
                                  {dinero.format(Number(venta.total) || 0)}
                                </td>
                                <td>
                                  <div className="sa-acciones">
                                    <button
                                      className="sa-icono-btn"
                                      type="button"
                                      title="Descargar PDF"
                                      aria-label={`Descargar PDF de ${venta.numeroDocumento}`}
                                      disabled={descargando === `${venta.idVenta}-PDF`}
                                      onClick={() => descargar(venta, 'PDF')}
                                    >
                                      {descargando === `${venta.idVenta}-PDF`
                                        ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                                        : <FileText size={16} aria-hidden="true" />}
                                    </button>
                                    <button
                                      className="sa-icono-btn"
                                      type="button"
                                      title="Descargar XML"
                                      aria-label={`Descargar XML de ${venta.numeroDocumento}`}
                                      disabled={descargando === `${venta.idVenta}-XML`}
                                      onClick={() => descargar(venta, 'XML')}
                                    >
                                      {descargando === `${venta.idVenta}-XML`
                                        ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                                        : <FileCode2 size={16} aria-hidden="true" />}
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {comprobantes.totalPaginas > 1 && (
                      <nav className="sa-paginacion" aria-label="Paginación de comprobantes">
                        <span className="sa-rango">
                          {primeraFila}–{ultimaFila} de {entero.format(comprobantes.totalElementos)}
                        </span>
                        <div className="sa-pagina-botones">
                          <button
                            className="sa-icono-btn"
                            type="button"
                            aria-label="Página anterior"
                            title="Página anterior"
                            disabled={pagina <= 0 || cargandoComprobantes}
                            onClick={() => setPagina((valor) => Math.max(0, valor - 1))}
                          >
                            <ChevronLeft size={18} aria-hidden="true" />
                          </button>
                          <span className="sa-pagina-actual">
                            Página {pagina + 1} de {comprobantes.totalPaginas}
                          </span>
                          <button
                            className="sa-icono-btn"
                            type="button"
                            aria-label="Página siguiente"
                            title="Página siguiente"
                            disabled={pagina + 1 >= comprobantes.totalPaginas || cargandoComprobantes}
                            onClick={() => setPagina((valor) => valor + 1)}
                          >
                            <ChevronRight size={18} aria-hidden="true" />
                          </button>
                        </div>
                      </nav>
                    )}
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
