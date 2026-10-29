import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Download,
  Eye,
  LogOut,
  ReceiptText,
  Search,
  ShoppingBag,
  ShoppingCart,
  Store,
  Ticket,
  UserRound,
} from 'lucide-react';
import { useStore } from '../store/store';
import { TIENDA_DEFAULT } from '../api/tiendaApi';
import { useClienteStore } from '../tienda/clienteStore';
import { useSesion } from '../auth/sesionStore';
import { listarComprobantesDeCliente } from '../api/comprobantesApi';
import { apiClient } from '../api/client';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import './PortalCliente.css';

const SECCIONES = [
  { id: 'catalogo', etiqueta: 'Catálogo / Tienda', icono: Store },
  { id: 'compras', etiqueta: 'Mis Compras', icono: ReceiptText },
  { id: 'creditos', etiqueta: 'Mis Créditos', icono: Ticket },
  { id: 'perfil', etiqueta: 'Mi Perfil', icono: UserRound },
];

// La tienda por defecto vive en un solo sitio (tiendaApi) para que el portal
// no dependa de un slug escrito a mano en varios archivos.
const SLUG_POR_DEFECTO = TIENDA_DEFAULT.slug;

// Un 404/409 al pedir el catálogo significa que el slug no apunta a una tienda
// (o que la cuenta no tiene tienda asociada), no un fallo del servidor.
function tiendaInexistente(problema) {
  return problema?.status === 404 || problema?.status === 409;
}

// Para el comprador, no tener cliente asociado o no tener vales no es un
// error: es el estado normal de "aún no tienes créditos a favor".
function sinCreditos(problema) {
  return problema?.status === 404 || problema?.status === 409;
}

function iniciales(nombre) {
  const partes = String(nombre || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (partes.length === 0) return '?';
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
  return (partes[0][0] + partes[1][0]).toUpperCase();
}

function moneda(valor) {
  return `S/ ${Number(valor || 0).toFixed(2)}`;
}

function fechaLegible(fecha, hora) {
  if (!fecha) return 'Sin fecha';
  const texto = hora ? `${fecha}T${String(hora).slice(0, 8)}` : `${fecha}T00:00:00`;
  const valor = new Date(texto);
  if (Number.isNaN(valor.getTime())) return fecha;
  return valor.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

export default function PortalCliente() {
  const navigate = useNavigate();
  const { tienda, productos, getTienda, listProductos } = useStore();
  const { carrito, cliente, agregarAlCarrito, logoutCliente, sincronizarCarrito } = useClienteStore();

  const [seccion, setSeccion] = useState('catalogo');
  const [busqueda, setBusqueda] = useState('');
  const [categoria, setCategoria] = useState('Todas');

  const [cargandoCatalogo, setCargandoCatalogo] = useState(true);
  const [errorCatalogo, setErrorCatalogo] = useState('');
  const [catalogoNoDisponible, setCatalogoNoDisponible] = useState(false);
  const [recargaCatalogo, setRecargaCatalogo] = useState(0);

  const [comprobantes, setComprobantes] = useState([]);
  const [cargandoCompras, setCargandoCompras] = useState(false);
  const [errorCompras, setErrorCompras] = useState('');
  const [recargaCompras, setRecargaCompras] = useState(0);

  const [creditos, setCreditos] = useState([]);
  const [cargandoCreditos, setCargandoCreditos] = useState(false);
  const [errorCreditos, setErrorCreditos] = useState('');
  const [recargaCreditos, setRecargaCreditos] = useState(0);

  const [perfil, setPerfil] = useState(null);
  const [cargandoPerfil, setCargandoPerfil] = useState(false);
  const [errorPerfil, setErrorPerfil] = useState('');
  const [recargaPerfil, setRecargaPerfil] = useState(0);

  const [descargando, setDescargando] = useState('');

  // El catalogo del portal es el publico de la tienda: no usa /productos, que
  // esta reservado al personal. Si el slug guardado no corresponde a una
  // tienda, se reintenta con la tienda por defecto antes de darlo por caido.
  useEffect(() => {
    let vigente = true;
    (async () => {
      try {
        const datosTienda = await getTienda(SLUG_POR_DEFECTO);
        const slug = datosTienda?.slug || SLUG_POR_DEFECTO;
        let catalogo;
        try {
          catalogo = await listProductos(slug);
        } catch (problema) {
          if (!tiendaInexistente(problema) || slug === SLUG_POR_DEFECTO) throw problema;
          catalogo = await listProductos(SLUG_POR_DEFECTO);
        }
        if (vigente) {
          // El carrito es persistente: al abrir el portal se reconcilia con el
          // catalogo vigente para descartar productos eliminados o agotados.
          sincronizarCarrito(catalogo);
          setErrorCatalogo('');
          setCatalogoNoDisponible(false);
        }
      } catch (problema) {
        if (!vigente) return;
        if (tiendaInexistente(problema)) {
          setCatalogoNoDisponible(true);
          setErrorCatalogo('');
        } else {
          setErrorCatalogo(problema?.message || 'No se pudo cargar el catálogo de la tienda.');
        }
      } finally {
        if (vigente) setCargandoCatalogo(false);
      }
    })();
    return () => {
      vigente = false;
    };
  }, [getTienda, listProductos, sincronizarCarrito, recargaCatalogo]);

  useEffect(() => {
    if (seccion !== 'compras' || !cliente?.idCliente) return undefined;
    let vigente = true;
    listarComprobantesDeCliente(String(cliente.idCliente))
      .then((lista) => {
        if (vigente) setComprobantes(Array.isArray(lista) ? lista : []);
      })
      .catch((problema) => {
        if (vigente) setErrorCompras(problema?.message || 'No se pudieron cargar tus compras.');
      })
      .finally(() => {
        if (vigente) setCargandoCompras(false);
      });
    return () => {
      vigente = false;
    };
  }, [seccion, cliente, recargaCompras]);

  useEffect(() => {
    if (seccion !== 'creditos') return undefined;
    let vigente = true;
    apiClient
      .get('/creditos/mios')
      .then((lista) => {
        if (!vigente) return;
        setCreditos(Array.isArray(lista) ? lista : []);
        setErrorCreditos('');
      })
      .catch((problema) => {
        if (!vigente) return;
        if (sinCreditos(problema)) {
          // Sin cliente asociado o sin vales: se muestra el estado vacío
          // amigable en lugar del error genérico del servidor.
          setCreditos([]);
          setErrorCreditos('');
        } else {
          setErrorCreditos(problema?.message || 'No se pudieron cargar tus créditos.');
        }
      })
      .finally(() => {
        if (vigente) setCargandoCreditos(false);
      });
    return () => {
      vigente = false;
    };
  }, [seccion, recargaCreditos]);

  useEffect(() => {
    if (seccion !== 'perfil') return undefined;
    let vigente = true;
    apiClient
      .get('/clientes/mi-perfil')
      .then((datos) => {
        if (vigente) setPerfil(datos);
      })
      .catch((problema) => {
        if (vigente) setErrorPerfil(problema?.message || 'No se pudo cargar tu perfil.');
      })
      .finally(() => {
        if (vigente) setCargandoPerfil(false);
      });
    return () => {
      vigente = false;
    };
  }, [seccion, recargaPerfil]);

  const categorias = useMemo(() => {
    const unicas = new Set(productos.map((producto) => producto.categoria).filter(Boolean));
    return ['Todas', ...unicas];
  }, [productos]);

  // Si la categoría elegida desaparece del catálogo, se muestra "Todas" sin
  // necesidad de un efecto que vuelva a pintar.
  const categoriaActiva = categorias.includes(categoria) ? categoria : 'Todas';

  const productosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return productos.filter((producto) => {
      const nombre = String(producto.nombre || '').toLowerCase();
      const rubro = String(producto.categoria || '').toLowerCase();
      const coincideTexto = !texto || nombre.includes(texto) || rubro.includes(texto);
      const coincideCategoria = categoriaActiva === 'Todas' || producto.categoria === categoriaActiva;
      return coincideTexto && coincideCategoria;
    });
  }, [productos, busqueda, categoriaActiva]);

  const unidadesCarrito = carrito.reduce((total, item) => total + Number(item.cantidad || 0), 0);
  const saldoDisponible = creditos
    .filter((credito) => credito.estado === 'Vigente')
    .reduce((total, credito) => total + Number(credito.montoDisponible || 0), 0);

  function irAlCatalogo() {
    setSeccion('catalogo');
  }

  function abrirCarrito() {
    navigate(`/tienda/${tienda.slug || SLUG_POR_DEFECTO}/carrito`);
  }

  function reintentarCatalogo() {
    setCargandoCatalogo(true);
    setErrorCatalogo('');
    setCatalogoNoDisponible(false);
    setRecargaCatalogo((n) => n + 1);
  }

  function marcarCarga(id) {
    if (id === 'compras') {
      setCargandoCompras(true);
      setErrorCompras('');
    } else if (id === 'creditos') {
      setCargandoCreditos(true);
      setErrorCreditos('');
    } else if (id === 'perfil') {
      setCargandoPerfil(true);
      setErrorPerfil('');
    }
  }

  function cambiarSeccion(id) {
    setSeccion(id);
    marcarCarga(id);
  }

  function reintentarCompras() {
    marcarCarga('compras');
    setRecargaCompras((n) => n + 1);
  }

  function reintentarCreditos() {
    marcarCarga('creditos');
    setRecargaCreditos((n) => n + 1);
  }

  function reintentarPerfil() {
    marcarCarga('perfil');
    setRecargaPerfil((n) => n + 1);
  }

  async function descargarPdf(comprobante, abrir = false) {
    const clave = `${comprobante.idVenta}-${abrir ? 'ver' : 'descargar'}`;
    setDescargando(clave);
    try {
      await descargarArchivoSeguro(
        `/comprobantes/${comprobante.idVenta}/pdf`,
        'PDF',
        comprobante.numeroDocumento || comprobante.idVenta,
        { abrir }
      );
      if (!abrir) showToast('PDF descargado correctamente.');
    } catch (problema) {
      showToast(problema?.message || 'No se pudo obtener el PDF del comprobante.');
    } finally {
      setDescargando('');
    }
  }

  function cerrarSesion() {
    logoutCliente();
    useSesion.getState().logout();
    navigate('/login', { replace: true });
  }

  const nombreCliente = cliente?.nombre || 'Cliente';

  return (
    <div className="portal-pagina">
      <header className="portal-topbar">
        <button type="button" className="portal-marca" onClick={irAlCatalogo}>
          {tienda.logoUrl ? (
            <img className="portal-marca-logo" src={tienda.logoUrl} alt={`Logo de ${tienda.nombre}`} />
          ) : (
            <span className="portal-marca-logo inicial">{iniciales(tienda.nombre)}</span>
          )}
          <span className="portal-marca-texto">
            <strong>{tienda.nombre || 'Mi Tienda'}</strong>
            <small>Portal de Cliente</small>
          </span>
        </button>

        <div className="portal-buscador">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Buscar productos..."
            aria-label="Buscar productos"
            value={busqueda}
            onChange={(evento) => {
              setBusqueda(evento.target.value);
              setSeccion('catalogo');
            }}
          />
        </div>

        <select
          className="portal-categorias"
          aria-label="Filtrar por categoría"
          value={categoriaActiva}
          onChange={(evento) => {
            setCategoria(evento.target.value);
            setSeccion('catalogo');
          }}
        >
          {categorias.map((opcion) => (
            <option key={opcion} value={opcion}>
              {opcion === 'Todas' ? 'Todas las categorías' : opcion}
            </option>
          ))}
        </select>

        <button
          type="button"
          className="portal-carrito"
          onClick={abrirCarrito}
          aria-label={`Carrito de compras, ${unidadesCarrito} unidades`}
        >
          <ShoppingCart size={22} aria-hidden="true" />
          {unidadesCarrito > 0 && (
            <span className="portal-carrito-badge">{unidadesCarrito > 99 ? '99+' : unidadesCarrito}</span>
          )}
        </button>
      </header>

      <div className="portal-cuerpo">
        <aside className="portal-sidebar">
          <nav className="portal-menu" aria-label="Secciones del portal">
            {SECCIONES.map(({ id, etiqueta, icono: Icono }) => (
              <button
                key={id}
                type="button"
                className={`portal-menu-link ${seccion === id ? 'active' : ''}`}
                aria-current={seccion === id ? 'page' : undefined}
                onClick={() => cambiarSeccion(id)}
              >
                <Icono size={18} aria-hidden="true" />
                {etiqueta}
              </button>
            ))}
          </nav>
          <div className="portal-sidebar-pie">
            <span className="portal-avatar">{iniciales(nombreCliente)}</span>
            <span className="portal-sidebar-datos">
              <strong>{nombreCliente}</strong>
              <small>{cliente?.correo}</small>
            </span>
          </div>
        </aside>

        <main className="portal-main">
          {seccion === 'catalogo' && (
            <section className="portal-seccion">
              <div className="portal-banner">
                <div className="portal-banner-texto">
                  <span className="portal-banner-etiqueta">Bienvenido/a, {nombreCliente}</span>
                  <h1>Tu tienda, siempre a mano.</h1>
                  <p>
                    Explora el catálogo de {tienda.nombre || 'la tienda'}, arma tu carrito y
                    gestiona tus compras, créditos y datos desde un solo lugar.
                  </p>
                </div>
                <div className="portal-banner-visual" aria-hidden="true">
                  <ShoppingBag size={72} strokeWidth={1.2} />
                </div>
              </div>

              <div className="portal-seccion-cabecera">
                <div>
                  <h2>Catálogo</h2>
                  <p>Productos disponibles de {tienda.nombre || 'la tienda'}.</p>
                </div>
                {!cargandoCatalogo && !errorCatalogo && !catalogoNoDisponible && (
                  <span className="portal-contador">
                    {productosFiltrados.length} {productosFiltrados.length === 1 ? 'producto' : 'productos'}
                  </span>
                )}
              </div>

              {cargandoCatalogo ? (
                <div className="portal-estado">Cargando catálogo...</div>
              ) : errorCatalogo ? (
                <div className="portal-aviso error">{errorCatalogo}</div>
              ) : catalogoNoDisponible ? (
                <div className="portal-vacio">
                  <Store size={36} aria-hidden="true" />
                  <h3>El catálogo aún no está disponible</h3>
                  <p>La tienda todavía no tiene productos publicados. Vuelve a intentarlo en un momento.</p>
                  <button type="button" className="btn secondary" onClick={reintentarCatalogo}>
                    Reintentar
                  </button>
                </div>
              ) : productosFiltrados.length === 0 ? (
                <div className="portal-vacio">
                  <Store size={36} aria-hidden="true" />
                  <h3>No encontramos productos</h3>
                  <p>Prueba con otra búsqueda o selecciona otra categoría.</p>
                  <button
                    type="button"
                    className="btn secondary"
                    onClick={() => {
                      setBusqueda('');
                      setCategoria('Todas');
                    }}
                  >
                    Ver todo el catálogo
                  </button>
                </div>
              ) : (
                <div className="portal-grid">
                  {productosFiltrados.map((producto) => (
                    <article key={producto.idProducto} className="portal-producto">
                      <div className="portal-producto-imagen" aria-hidden="true">
                        <span>{iniciales(producto.nombre)}</span>
                      </div>
                      <div className="portal-producto-cuerpo">
                        <span className="portal-producto-categoria">
                          {producto.categoria || 'General'}
                        </span>
                        <h3 className="portal-producto-nombre">{producto.nombre}</h3>
                        <div className="portal-producto-pie">
                          <strong className="portal-producto-precio">
                            {moneda(producto.precio)}
                          </strong>
                          <button
                            type="button"
                            className="btn small"
                            onClick={() => agregarAlCarrito(producto, 1)}
                          >
                            <ShoppingCart size={14} aria-hidden="true" />
                            Agregar al Carrito
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          )}

          {seccion === 'compras' && (
            <section className="portal-seccion">
              <div className="portal-seccion-cabecera">
                <div>
                  <h2>Mis Compras</h2>
                  <p>Boletas y facturas emitidas a tu nombre.</p>
                </div>
              </div>

              {cargandoCompras ? (
                <div className="portal-estado">Cargando compras...</div>
              ) : errorCompras ? (
                <div className="portal-aviso error">
                  <span>{errorCompras}</span>
                  <button type="button" className="btn secondary small" onClick={reintentarCompras}>
                    Reintentar
                  </button>
                </div>
              ) : comprobantes.length === 0 ? (
                <div className="portal-vacio">
                  <ReceiptText size={36} aria-hidden="true" />
                  <h3>Aún no tienes compras registradas</h3>
                  <p>Cuando emitas tu primer comprobante, aparecerá aquí con su PDF.</p>
                  <button type="button" className="btn" onClick={irAlCatalogo}>
                    Ir al catálogo
                  </button>
                </div>
              ) : (
                <div className="portal-lista">
                  {comprobantes.map((comprobante) => {
                    const total = comprobante.totales?.montoTotalPagar
                      ?? comprobante.totales?.totalVenta
                      ?? 0;
                    return (
                      <article key={comprobante.idVenta} className="portal-compra">
                        <div className="portal-compra-principal">
                          <span className="portal-doc">
                            {comprobante.tipoComprobanteDescripcion || 'Comprobante'}{' '}
                            {comprobante.numeroDocumento}
                          </span>
                          <span className="portal-meta">
                            {fechaLegible(comprobante.fechaEmision, comprobante.horaEmision)}
                          </span>
                        </div>
                        <div className="portal-compra-lado">
                          <strong className="portal-total">{moneda(total)}</strong>
                          <span className="portal-badge">{comprobante.estado || 'Emitido'}</span>
                        </div>
                        <div className="portal-compra-acciones">
                          <button
                            type="button"
                            className="btn ghost small"
                            onClick={() => descargarPdf(comprobante, true)}
                            disabled={descargando === `${comprobante.idVenta}-ver`}
                          >
                            <Eye size={14} aria-hidden="true" />
                            Ver PDF
                          </button>
                          <button
                            type="button"
                            className="btn secondary small"
                            onClick={() => descargarPdf(comprobante, false)}
                            disabled={descargando === `${comprobante.idVenta}-descargar`}
                          >
                            <Download size={14} aria-hidden="true" />
                            Descargar PDF
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          )}

          {seccion === 'creditos' && (
            <section className="portal-seccion">
              <div className="portal-seccion-cabecera">
                <div>
                  <h2>Mis Créditos</h2>
                  <p>Vales a favor generados por tus devoluciones.</p>
                </div>
              </div>

              {cargandoCreditos ? (
                <div className="portal-estado">Cargando créditos...</div>
              ) : errorCreditos ? (
                <div className="portal-aviso error">
                  <span>{errorCreditos}</span>
                  <button type="button" className="btn secondary small" onClick={reintentarCreditos}>
                    Reintentar
                  </button>
                </div>
              ) : creditos.length === 0 ? (
                <div className="portal-vacio">
                  <Ticket size={36} aria-hidden="true" />
                  <h3>No tienes créditos a favor</h3>
                  <p>Si devuelves un producto, aquí verás tu vale BOL-CR con el saldo disponible.</p>
                </div>
              ) : (
                <>
                  <div className="portal-saldo">
                    <div>
                      <span className="portal-meta">Saldo total disponible</span>
                      <strong>{moneda(saldoDisponible)}</strong>
                    </div>
                    <Ticket size={28} aria-hidden="true" />
                  </div>
                  <div className="portal-lista">
                    {creditos.map((credito) => (
                      <article key={credito.idCredito || credito.codigo} className="portal-credito">
                        <div className="portal-credito-cabecera">
                          <span className="portal-doc">{credito.codigo}</span>
                          <span
                            className={`portal-badge ${credito.estado === 'Vigente' ? 'vigente' : 'neutral'}`}
                          >
                            {credito.estado || 'Vigente'}
                          </span>
                        </div>
                        <div className="portal-credito-datos">
                          <div>
                            <span className="portal-meta">Saldo disponible</span>
                            <strong className="portal-total">{moneda(credito.montoDisponible)}</strong>
                          </div>
                          <div>
                            <span className="portal-meta">Monto del vale</span>
                            <span>{moneda(credito.montoTotal)}</span>
                          </div>
                          <div>
                            <span className="portal-meta">Comprobante de origen</span>
                            <span>{credito.numeroDocumentoOrigen || '—'}</span>
                          </div>
                          <div>
                            <span className="portal-meta">Fecha</span>
                            <span>{fechaLegible(credito.fecha)}</span>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                </>
              )}
            </section>
          )}

          {seccion === 'perfil' && (
            <section className="portal-seccion">
              <div className="portal-seccion-cabecera">
                <div>
                  <h2>Mi Perfil</h2>
                  <p>Tus datos personales y direcciones registradas.</p>
                </div>
              </div>

              {cargandoPerfil ? (
                <div className="portal-estado">Cargando perfil...</div>
              ) : errorPerfil ? (
                <div className="portal-aviso error">
                  <span>{errorPerfil}</span>
                  <button type="button" className="btn secondary small" onClick={reintentarPerfil}>
                    Reintentar
                  </button>
                </div>
              ) : (
                <>
                  <div className="portal-perfil">
                    <div className="portal-perfil-cabecera">
                      <span className="portal-avatar grande">{iniciales(
                        `${perfil?.nombre || ''} ${perfil?.apellido || ''}`
                      )}</span>
                      <div>
                        <h3>{perfil?.nombre} {perfil?.apellido}</h3>
                        <span className="portal-meta">Cliente {perfil?.idCliente}</span>
                      </div>
                    </div>
                    <dl className="portal-perfil-datos">
                      <div>
                        <dt>Nombres</dt>
                        <dd>{perfil?.nombre || '—'}</dd>
                      </div>
                      <div>
                        <dt>Apellidos</dt>
                        <dd>{perfil?.apellido || '—'}</dd>
                      </div>
                      <div>
                        <dt>Correo</dt>
                        <dd>{perfil?.correo || '—'}</dd>
                      </div>
                      <div>
                        <dt>Teléfono</dt>
                        <dd>{perfil?.telefono || '—'}</dd>
                      </div>
                    </dl>
                  </div>

                  <div className="portal-bloque">
                    <h3>Direcciones</h3>
                    {perfil?.direcciones?.length > 0 ? (
                      <ul className="portal-direcciones">
                        {perfil.direcciones.map((direccion) => (
                          <li key={`${direccion.tipo}-${direccion.direccion}`}>
                            <span className="portal-meta">{direccion.tipo}</span>
                            <span>{direccion.direccion}</span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="portal-meta">
                        Aún no registramos direcciones en tus comprobantes.
                      </p>
                    )}
                  </div>

                  <div className="portal-bloque">
                    <button type="button" className="btn ghost" onClick={cerrarSesion}>
                      <LogOut size={16} aria-hidden="true" />
                      Cerrar sesión
                    </button>
                  </div>
                </>
              )}
            </section>
          )}
        </main>
      </div>
    </div>
  );
}
