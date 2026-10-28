import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  BadgeCheck,
  HandCoins,
  LoaderCircle,
  Minus,
  Plus,
  PlusCircle,
  RefreshCw,
  RotateCcw,
  Search,
  X,
} from 'lucide-react';
import {
  canjearCredito,
  listarCreditos,
  reembolsarCredito,
} from '../api/creditosApi';
import {
  listarVentas,
  obtenerVentaParaDevolucion,
  registrarDevolucion,
} from '../api/ventasApi';
import { showToast } from '../components/toastBus';
import PanelLayout from './PanelLayout';
import './HistorialVentas.css';
import './GestionCreditos.css';

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});

const fechaCorta = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' });

function formatearFecha(valor) {
  if (!valor) return '—';
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? '—' : fechaCorta.format(fecha);
}

function claseEstado(estado) {
  const normalizado = (estado || '').toLowerCase();
  if (normalizado === 'vigente') return 'ok';
  if (normalizado === 'reembolsado') return 'error';
  return 'neutro';
}

function puedeCanjear(credito) {
  return credito.estado === 'Vigente' && Number(credito.montoDisponible) > 0;
}

export default function GestionCreditos() {
  const [creditos, setCreditos] = useState([]);
  const [texto, setTexto] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const solicitud = useRef(0);

  // Canje
  const [canje, setCanje] = useState(null);
  const [montoCanje, setMontoCanje] = useState('');
  const [canjeando, setCanjeando] = useState(false);

  // Reembolso
  const [reembolso, setReembolso] = useState(null);
  const [reembolsando, setReembolsando] = useState(false);

  // Devolución (origen del crédito)
  const [modalDevolucion, setModalDevolucion] = useState(false);
  const [busquedaDevolucion, setBusquedaDevolucion] = useState('');
  const [buscandoDevolucion, setBuscandoDevolucion] = useState(false);
  const [resultadosDevolucion, setResultadosDevolucion] = useState([]);
  const [ventaDevolucion, setVentaDevolucion] = useState(null);
  const [cantidadesDevolucion, setCantidadesDevolucion] = useState({});
  const [motivoDevolucion, setMotivoDevolucion] = useState('');
  const [procesandoDevolucion, setProcesandoDevolucion] = useState(false);
  const [devolucionExitosa, setDevolucionExitosa] = useState(null);

  useEffect(() => {
    const temporizador = setTimeout(() => setBusqueda(texto.trim()), 300);
    return () => clearTimeout(temporizador);
  }, [texto]);

  const cargar = useCallback(async () => {
    const actual = solicitud.current + 1;
    solicitud.current = actual;
    setCargando(true);
    setError('');
    try {
      const datos = await listarCreditos();
      if (actual !== solicitud.current) return;
      setCreditos(Array.isArray(datos) ? datos : []);
    } catch (problema) {
      if (actual !== solicitud.current) return;
      setError(problema?.message || 'No se pudieron cargar los créditos.');
      setCreditos([]);
    } finally {
      if (actual === solicitud.current) setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const filtrados = useMemo(() => {
    const termino = busqueda.toLowerCase();
    if (!termino) return creditos;
    return creditos.filter((credito) => [credito.codigo, credito.numeroDocumentoOrigen,
      credito.clienteNombre, credito.dniCliente, credito.estado]
        .some((valor) => String(valor || '').toLowerCase().includes(termino)));
  }, [creditos, busqueda]);

  const saldoVigente = creditos
    .filter(puedeCanjear)
    .reduce((suma, credito) => suma + (Number(credito.montoDisponible) || 0), 0);

  function abrirCanje(credito) {
    setCanje(credito);
    setMontoCanje(String(Number(credito.montoDisponible) || 0));
  }

  async function confirmarCanje(evento) {
    evento.preventDefault();
    if (!canje || canjeando) return;
    const monto = Number(String(montoCanje).replace(',', '.'));
    const disponible = Number(canje.montoDisponible) || 0;
    if (!Number.isFinite(monto) || monto <= 0) {
      showToast('Ingresa un monto mayor a cero.');
      return;
    }
    if (monto > disponible) {
      showToast(`El saldo disponible es ${dinero.format(disponible)}.`);
      return;
    }

    setCanjeando(true);
    try {
      const actualizado = await canjearCredito({ codigo: canje.codigo, monto });
      showToast(actualizado.estado === 'Usado'
        ? `Crédito ${actualizado.codigo} canjeado en su totalidad.`
        : `Crédito ${actualizado.codigo}: saldo restante ${dinero.format(Number(actualizado.montoDisponible) || 0)}.`);
      setCanje(null);
      cargar();
    } catch (problema) {
      showToast(problema?.message || 'No se pudo canjear el crédito.');
    } finally {
      setCanjeando(false);
    }
  }

  async function confirmarReembolso() {
    if (!reembolso || reembolsando) return;
    setReembolsando(true);
    try {
      await reembolsarCredito({ codigo: reembolso.codigo });
      showToast(`Crédito ${reembolso.codigo} marcado como reembolsado.`);
      setReembolso(null);
      cargar();
    } catch (problema) {
      showToast(problema?.message || 'No se pudo reembolsar el crédito.');
    } finally {
      setReembolsando(false);
    }
  }

  function abrirModalDevolucion() {
    setModalDevolucion(true);
    setBusquedaDevolucion('');
    setResultadosDevolucion([]);
    setVentaDevolucion(null);
    setCantidadesDevolucion({});
    setMotivoDevolucion('');
    setDevolucionExitosa(null);
  }

  function cerrarModalDevolucion() {
    if (procesandoDevolucion) return;
    setModalDevolucion(false);
    setDevolucionExitosa(null);
    cargar();
  }

  async function buscarVentas(evento) {
    evento?.preventDefault();
    const filtro = busquedaDevolucion.trim();
    if (!filtro) {
      showToast('Ingresa el número de boleta/factura o el DNI del cliente.');
      return;
    }
    if (buscandoDevolucion) return;
    setBuscandoDevolucion(true);
    try {
      const pagina = await listarVentas({ pagina: 0, tamanio: 10, busqueda: filtro });
      const encontradas = Array.isArray(pagina?.contenido) ? pagina.contenido : [];
      setResultadosDevolucion(encontradas);
      setVentaDevolucion(null);
      if (encontradas.length === 0) {
        showToast(`No se encontraron ventas para “${filtro}”.`);
      }
    } catch (problema) {
      showToast(problema?.message || 'No se pudo buscar la venta.');
    } finally {
      setBuscandoDevolucion(false);
    }
  }

  async function elegirVenta(venta) {
    try {
      const detalle = await obtenerVentaParaDevolucion(venta.idVenta);
      setVentaDevolucion(detalle);
      setCantidadesDevolucion({});
      setDevolucionExitosa(null);
    } catch (problema) {
      showToast(problema?.message || 'No se pudo cargar la venta.');
    }
  }

  function cambiarCantidad(idProducto, maximo, delta) {
    setCantidadesDevolucion((previas) => {
      const actual = previas[idProducto] || 0;
      return { ...previas, [idProducto]: Math.min(Math.max(0, actual + delta), maximo) };
    });
  }

  const itemsDevolucion = ventaDevolucion
    ? ventaDevolucion.detalles
        .filter((detalle) => (cantidadesDevolucion[detalle.idProducto] || 0) > 0)
        .map((detalle) => ({
          idProducto: detalle.idProducto,
          cantidad: cantidadesDevolucion[detalle.idProducto],
        }))
    : [];

  const totalDevolucion = ventaDevolucion
    ? Math.round(ventaDevolucion.detalles.reduce(
      (suma, detalle) => suma
        + (Number(detalle.precioUnitario) || 0) * (cantidadesDevolucion[detalle.idProducto] || 0),
      0) * 100) / 100
    : 0;

  async function confirmarDevolucion() {
    if (!ventaDevolucion) return;
    if (itemsDevolucion.length === 0) {
      showToast('Selecciona al menos un producto y la cantidad a devolver.');
      return;
    }
    if (procesandoDevolucion) return;
    setProcesandoDevolucion(true);
    try {
      const devolucion = await registrarDevolucion({
        idVenta: ventaDevolucion.id ?? ventaDevolucion.idVenta,
        motivo: motivoDevolucion.trim(),
        items: itemsDevolucion,
      });
      setDevolucionExitosa(devolucion);
      setCantidadesDevolucion({});
    } catch (problema) {
      showToast(problema?.message || 'No se pudo registrar la devolución.');
    } finally {
      setProcesandoDevolucion(false);
    }
  }

  return (
    <PanelLayout>
      <main className="page historial-page">
        <header className="hist-cabecera">
          <div>
            <h1>Créditos a favor.</h1>
            <p>Saldo generado por devoluciones. Los comprobantes originales no se modifican.</p>
          </div>
          <div className="gc-cabecera-acciones">
            <button className="gc-btn rosa" type="button" onClick={abrirModalDevolucion}>
              <PlusCircle size={16} aria-hidden="true" />
              Registrar Nueva Devolución
            </button>
            <button className="hist-btn actualizar" type="button" onClick={cargar} disabled={cargando}>
              <RefreshCw size={16} aria-hidden="true" className={cargando ? 'girando' : ''} />
              Actualizar
            </button>
          </div>
        </header>

        <section className="hist-toolbar" aria-label="Buscar créditos">
          <div className="hist-buscador">
            <Search size={18} aria-hidden="true" />
            <input
              type="search"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder="Buscar por código, boleta, cliente o DNI..."
              aria-label="Buscar créditos por código, boleta, cliente o DNI"
            />
          </div>
          <div className="gc-resumen" aria-live="polite">
            <span>{creditos.length === 1 ? '1 crédito' : `${creditos.length} créditos`}</span>
            <span className="gc-resumen-saldo">
              Saldo vigente: <strong>{dinero.format(saldoVigente)}</strong>
            </span>
          </div>
        </section>

        {error && (
          <div className="hist-error" role="alert">
            <p>{error}</p>
            <button className="hist-btn" type="button" onClick={cargar}>Reintentar</button>
          </div>
        )}

        <section className="hist-panel" aria-label="Listado de créditos">
          {!error && filtrados.length === 0 && (
            <p className="hist-vacio">
              {cargando
                ? 'Cargando créditos…'
                : busqueda
                  ? `Ningún crédito coincide con “${busqueda}”.`
                  : 'Aún no hay créditos generados. Registra una devolución para crear saldo a favor.'}
            </p>
          )}

          {filtrados.length > 0 && (
            <div className={`hist-tabla-envoltura${cargando ? ' cargando' : ''}`}>
              <table className="hist-tabla">
                <thead>
                  <tr>
                    <th scope="col">Cód. Crédito</th>
                    <th scope="col">Boleta</th>
                    <th scope="col">Cliente</th>
                    <th scope="col" className="numero">Monto</th>
                    <th scope="col" className="numero">Disponible</th>
                    <th scope="col">Estado</th>
                    <th scope="col" className="acciones">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((credito) => (
                    <tr key={credito.idCredito || credito.codigo}>
                      <td>
                        <div className="gc-codigo">
                          <strong>{credito.codigo}</strong>
                          <small>{formatearFecha(credito.fecha)}</small>
                        </div>
                      </td>
                      <td>{credito.numeroDocumentoOrigen || '—'}</td>
                      <td>
                        <div className="hist-cliente">
                          <span>{credito.clienteNombre || 'Cliente'}</span>
                          {credito.dniCliente && <small>{credito.dniCliente}</small>}
                        </div>
                      </td>
                      <td className="numero">{dinero.format(Number(credito.montoTotal) || 0)}</td>
                      <td className="numero gc-disponible">
                        {dinero.format(Number(credito.montoDisponible) || 0)}
                      </td>
                      <td>
                        <span className={`hist-estado ${claseEstado(credito.estado)}`}>
                          {credito.estado || 'Vigente'}
                        </span>
                      </td>
                      <td>
                        <div className="gc-acciones">
                          <button
                            className="gc-btn canjear"
                            type="button"
                            disabled={!puedeCanjear(credito)}
                            onClick={() => abrirCanje(credito)}
                          >
                            <HandCoins size={15} aria-hidden="true" />
                            Canjear
                          </button>
                          <button
                            className="gc-btn reembolsar"
                            type="button"
                            disabled={credito.estado !== 'Vigente'}
                            onClick={() => setReembolso(credito)}
                          >
                            <RotateCcw size={15} aria-hidden="true" />
                            Reembolsar
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
      </main>

      {canje && (
        <div className="gc-overlay" role="dialog" aria-modal="true" aria-labelledby="canje-titulo">
          <div className="gc-modal">
            <h2 id="canje-titulo" className="gc-titulo">Canjear crédito</h2>
            <p className="gc-mensaje">
              {canje.codigo} · {canje.clienteNombre || 'Cliente'} · disponible{' '}
              {dinero.format(Number(canje.montoDisponible) || 0)}
            </p>
            <form className="gc-form" onSubmit={confirmarCanje}>
              <label className="gc-label" htmlFor="monto-canje">Monto a canjear</label>
              <input
                id="monto-canje"
                className="gc-input"
                type="number"
                min="0.01"
                step="0.01"
                max={Number(canje.montoDisponible) || undefined}
                value={montoCanje}
                onChange={(evento) => setMontoCanje(evento.target.value)}
                autoFocus
              />
              <button
                className="gc-btn suave"
                type="button"
                onClick={() => setMontoCanje(String(Number(canje.montoDisponible) || 0))}
              >
                Usar todo el saldo
              </button>
              <div className="gc-acciones-modal">
                <button className="gc-modal-btn primario" type="submit" disabled={canjeando}>
                  {canjeando
                    ? <LoaderCircle className="gc-girando" size={18} aria-hidden="true" />
                    : <BadgeCheck size={18} aria-hidden="true" />}
                  {canjeando ? 'Canjeando...' : 'Confirmar canje'}
                </button>
                <button
                  className="gc-modal-btn secundario"
                  type="button"
                  onClick={() => setCanje(null)}
                  disabled={canjeando}
                >
                  <X size={18} aria-hidden="true" />
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {reembolso && (
        <div className="gc-overlay" role="dialog" aria-modal="true" aria-labelledby="reembolso-titulo">
          <div className="gc-modal">
            <h2 id="reembolso-titulo" className="gc-titulo">Reembolsar crédito</h2>
            <p className="gc-mensaje">
              El crédito {reembolso.codigo} quedará en estado Reembolsado y su saldo disponible
              pasará a S/ 0.00. El comprobante original no se modifica.
            </p>
            <div className="gc-acciones-modal">
              <button
                className="gc-modal-btn primario"
                type="button"
                onClick={confirmarReembolso}
                disabled={reembolsando}
              >
                {reembolsando
                  ? <LoaderCircle className="gc-girando" size={18} aria-hidden="true" />
                  : <RotateCcw size={18} aria-hidden="true" />}
                {reembolsando ? 'Reembolsando...' : 'Confirmar reembolso'}
              </button>
              <button
                className="gc-modal-btn secundario"
                type="button"
                onClick={() => setReembolso(null)}
                disabled={reembolsando}
              >
                <X size={18} aria-hidden="true" />
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {modalDevolucion && (
        <div className="gc-overlay" role="dialog" aria-modal="true" aria-labelledby="devolucion-titulo">
          <div className="gc-modal amplio">
            {devolucionExitosa ? (
              <div className="gc-exito">
                <BadgeCheck size={44} aria-hidden="true" />
                <h2 id="devolucion-titulo" className="gc-titulo">Devolución registrada</h2>
                <p className="gc-mensaje">
                  Se generó el crédito <strong>{devolucionExitosa.creditoCodigo || 'a favor'}</strong> por{' '}
                  {dinero.format(Number(devolucionExitosa.total) || 0)}.
                </p>
                <div className="gc-acciones-modal">
                  <button className="gc-modal-btn secundario" type="button" onClick={abrirModalDevolucion}>
                    Registrar otra devolución
                  </button>
                  <button className="gc-modal-btn primario" type="button" onClick={cerrarModalDevolucion}>
                    Cerrar
                  </button>
                </div>
              </div>
            ) : !ventaDevolucion ? (
              <>
                <h2 id="devolucion-titulo" className="gc-titulo">Registrar nueva devolución</h2>
                <p className="gc-mensaje">
                  Busca la boleta o factura por su número (ej. BOL-000123) o por el DNI del cliente.
                </p>
                <form className="gc-devo-busqueda" onSubmit={buscarVentas}>
                  <input
                    type="search"
                    value={busquedaDevolucion}
                    onChange={(evento) => setBusquedaDevolucion(evento.target.value)}
                    placeholder="BOL-000123 o 12345678"
                    aria-label="Buscar venta por número de comprobante o DNI"
                    autoComplete="off"
                  />
                  <button className="gc-modal-btn primario" type="submit" disabled={buscandoDevolucion}>
                    {buscandoDevolucion
                      ? <LoaderCircle className="gc-girando" size={18} aria-hidden="true" />
                      : <Search size={18} aria-hidden="true" />}
                    {buscandoDevolucion ? 'Buscando...' : 'Buscar'}
                  </button>
                </form>
                {resultadosDevolucion.length > 0 && (
                  <div className="gc-devo-resultados">
                    {resultadosDevolucion.map((venta) => (
                      <button
                        key={venta.idVenta}
                        type="button"
                        className="gc-devo-resultado"
                        onClick={() => elegirVenta(venta)}
                      >
                        <span className="gc-devo-resultado-doc">
                          <strong>{venta.numeroDocumento}</strong>
                          <small>{venta.cliente || 'Cliente'}
                            {venta.documentoCliente ? ` · ${venta.documentoCliente}` : ''}</small>
                        </span>
                        <span className="gc-devo-resultado-total">
                          {dinero.format(Number(venta.total) || 0)}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
                <div className="gc-acciones-modal">
                  <button className="gc-modal-btn secundario" type="button" onClick={cerrarModalDevolucion}>
                    Cerrar
                  </button>
                </div>
              </>
            ) : (
              <>
                <h2 id="devolucion-titulo" className="gc-titulo">
                  Devolver productos · {ventaDevolucion.numeroDocumento}
                </h2>
                <p className="gc-mensaje">
                  {ventaDevolucion.tipoDocumento || 'Comprobante'} · {ventaDevolucion.receptorRazonSocial || 'Cliente'}
                  {ventaDevolucion.receptorNumDocumento ? ` (${ventaDevolucion.receptorNumDocumento})` : ''}
                </p>
                <div className="gc-devo-lineas">
                  {ventaDevolucion.detalles.map((detalle) => {
                    const maximo = detalle.cantidad - (detalle.cantidadDevuelta || 0);
                    const elegida = cantidadesDevolucion[detalle.idProducto] || 0;
                    return (
                      <div key={detalle.idDetalle ?? detalle.idProducto} className="gc-devo-linea">
                        <div className="gc-devo-linea-info">
                          <strong>{detalle.descripcion}</strong>
                          <small>
                            {dinero.format(Number(detalle.precioUnitario) || 0)} c/u
                            · Vendidas {detalle.cantidad}
                            {(detalle.cantidadDevuelta || 0) > 0 && ` · Ya devueltas ${detalle.cantidadDevuelta}`}
                          </small>
                        </div>
                        <div className="gc-cantidad">
                          <button
                            type="button"
                            aria-label={`Quitar una unidad de ${detalle.descripcion}`}
                            onClick={() => cambiarCantidad(detalle.idProducto, maximo, -1)}
                            disabled={elegida <= 0}
                          >
                            <Minus size={18} aria-hidden="true" />
                          </button>
                          <span aria-live="polite">{elegida}</span>
                          <button
                            type="button"
                            aria-label={`Agregar una unidad de ${detalle.descripcion}`}
                            onClick={() => cambiarCantidad(detalle.idProducto, maximo, 1)}
                            disabled={elegida >= maximo}
                          >
                            <Plus size={18} aria-hidden="true" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
                <label className="gc-label" htmlFor="motivo-devolucion">Motivo (opcional)</label>
                <input
                  id="motivo-devolucion"
                  className="gc-input"
                  type="text"
                  maxLength={300}
                  value={motivoDevolucion}
                  onChange={(evento) => setMotivoDevolucion(evento.target.value)}
                  placeholder="Producto defectuoso, cambio de talla..."
                />
                <div className="gc-devo-total">
                  <span>Total a devolver</span>
                  <strong>{dinero.format(totalDevolucion)}</strong>
                </div>
                <div className="gc-acciones-modal">
                  <button
                    className="gc-modal-btn primario"
                    type="button"
                    onClick={confirmarDevolucion}
                    disabled={procesandoDevolucion || itemsDevolucion.length === 0}
                  >
                    {procesandoDevolucion
                      ? <LoaderCircle className="gc-girando" size={18} aria-hidden="true" />
                      : <RotateCcw size={18} aria-hidden="true" />}
                    {procesandoDevolucion ? 'Procesando...' : 'Confirmar Devolución'}
                  </button>
                  <button
                    className="gc-modal-btn secundario"
                    type="button"
                    onClick={() => setVentaDevolucion(null)}
                    disabled={procesandoDevolucion}
                  >
                    Volver a la búsqueda
                  </button>
                  <button
                    className="gc-modal-btn secundario"
                    type="button"
                    onClick={cerrarModalDevolucion}
                    disabled={procesandoDevolucion}
                  >
                    Cerrar
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </PanelLayout>
  );
}
