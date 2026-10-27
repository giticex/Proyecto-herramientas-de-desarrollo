import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Banknote,
  CheckCircle2,
  CreditCard,
  FileText,
  History,
  LoaderCircle,
  LogOut,
  Minus,
  Plus,
  Printer,
  ScanBarcode,
  Search,
  ShoppingCart,
  Smartphone,
  Trash2,
  UserPlus,
} from 'lucide-react';
import { useSesion } from '../auth/sesionStore';
import { useStore } from '../store/store';
import { showToast } from '../components/toastBus';
import { registrarVenta, rutaComprobantePdf } from '../api/ventasApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import './PuntoDeVenta.css';

const IGV_RATE = 0.18;
const IGV_PORCENTAJE = 18;

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});

const METODOS_PAGO = [
  { nombre: 'Efectivo', icono: Banknote },
  { nombre: 'Tarjeta', icono: CreditCard },
  { nombre: 'Yape', icono: Smartphone },
];

function redondear(valor) {
  return Math.round(valor * 100) / 100;
}

function precioDe(producto) {
  return Number(producto.precio) || 0;
}

export default function PuntoDeVenta() {
  const productos = useStore((estado) => estado.productos);
  const listProductos = useStore((estado) => estado.listProductos);
  const tienda = useStore((estado) => estado.tienda);
  const getTienda = useStore((estado) => estado.getTienda);
  const rol = useSesion((estado) => estado.rol);
  const logout = useSesion((estado) => estado.logout);
  const navigate = useNavigate();
  const esAdmin = rol === 'ADMIN';

  function salir() {
    logout();
    navigate('/admin/login');
  }

  const [lineas, setLineas] = useState([]);
  const [busqueda, setBusqueda] = useState('');
  const [metodoPago, setMetodoPago] = useState('Efectivo');
  const [tipoComprobante, setTipoComprobante] = useState('Boleta');
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState(false);
  const [ventaExitosa, setVentaExitosa] = useState(null);
  const [imprimiendo, setImprimiendo] = useState(false);
  const [dniCliente, setDniCliente] = useState('');
  const [rucCliente, setRucCliente] = useState('');
  const [razonSocialCliente, setRazonSocialCliente] = useState('');
  const [montoRecibido, setMontoRecibido] = useState('');
  const buscadorRef = useRef(null);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([listProductos(), getTienda()]).finally(() => {
      if (vigente) {
        setCargando(false);
        buscadorRef.current?.focus();
      }
    });
    return () => {
      vigente = false;
    };
  }, [listProductos, getTienda]);

  const termino = busqueda.trim().toLowerCase();
  const sugerencias = useMemo(() => {
    if (!termino) return [];
    return productos
        .filter((producto) => (
            String(producto.idProducto).toLowerCase().includes(termino)
            || (producto.nombre || '').toLowerCase().includes(termino)
        ))
        .sort((a, b) => (
            Number(String(b.idProducto) === termino) - Number(String(a.idProducto) === termino)
        ))
        .slice(0, 6);
  }, [productos, termino]);

  // CORRECCIÓN MATEMÁTICA APLICADA AQUÍ
  const total = useMemo(
      () => redondear(lineas.reduce((suma, linea) => suma + (linea.precio * linea.cantidad), 0)),
      [lineas],
  );
  const subtotal = redondear(total / (1 + IGV_RATE));
  const igv = redondear(total - subtotal);
  const unidades = lineas.reduce((suma, linea) => suma + linea.cantidad, 0);
  const montoRecibidoNum = Number(String(montoRecibido).replace(',', '.')) || 0;
  const vuelto = metodoPago === 'Efectivo' ? Math.max(0, redondear(montoRecibidoNum - total)) : 0;
  const pagoInsuficiente = metodoPago === 'Efectivo' && lineas.length > 0 && montoRecibidoNum < total;

  function agregar(producto) {
    const stock = Number(producto.stock) || 0;
    if (stock <= 0) {
      showToast(`${producto.nombre} no tiene stock disponible.`);
      return;
    }

    const existente = lineas.find((linea) => linea.idProducto === producto.idProducto);
    if (existente && existente.cantidad >= stock) {
      showToast(`Solo hay ${stock} unidades de ${producto.nombre}.`);
      return;
    }

    if (existente) {
      setLineas((previas) => previas.map((linea) => (
          linea.idProducto === producto.idProducto
              ? { ...linea, cantidad: Math.min(linea.cantidad + 1, stock) }
              : linea
      )));
    } else {
      setLineas((previas) => [
        ...previas,
        {
          idProducto: producto.idProducto,
          nombre: producto.nombre,
          precio: precioDe(producto),
          stock,
          cantidad: 1,
        },
      ]);
    }

    setBusqueda('');
    buscadorRef.current?.focus();
  }

  function cambiarCantidad(idProducto, delta) {
    setLineas((previas) => previas.map((linea) => {
      if (linea.idProducto !== idProducto) return linea;
      const cantidad = Math.min(Math.max(1, linea.cantidad + delta), linea.stock);
      return { ...linea, cantidad };
    }));
  }

  function eliminar(idProducto) {
    setLineas((previas) => previas.filter((linea) => linea.idProducto !== idProducto));
  }

  function alPresionarTecla(evento) {
    if (evento.key === 'Escape') {
      setBusqueda('');
      return;
    }
    if (evento.key === 'Enter' && sugerencias.length > 0) {
      evento.preventDefault();
      agregar(sugerencias[0]);
    }
  }

  async function generarComprobante() {
    if (lineas.length === 0) {
      showToast('Agrega al menos un producto para generar el comprobante.');
      return;
    }
    if (tipoComprobante === 'Factura') {
      if (!rucCliente.trim() || !razonSocialCliente.trim()) {
        showToast('Completa RUC y Razón Social para emitir Factura.');
        return;
      }
      if (rucCliente.trim().length !== 11) {
        showToast('El RUC debe tener 11 dígitos.');
        return;
      }
    }
    if (tipoComprobante === 'Boleta' && dniCliente.trim() && dniCliente.trim().length !== 8) {
      showToast('El DNI debe tener 8 dígitos.');
      return;
    }
    if (procesando) return;

    setProcesando(true);
    try {
      const items = lineas.map((linea) => ({
        idProducto: Number(linea.idProducto),
        cantidad: Number(linea.cantidad),
      }));
      const payload = {
        tipoDocumento: tipoComprobante,
        medioPago: metodoPago,
        items,
      };
      if (tipoComprobante === 'Boleta' && dniCliente.trim()) {
        payload.dni = dniCliente.trim();
        payload.documentoCliente = dniCliente.trim();
      }
      if (tipoComprobante === 'Factura') {
        payload.ruc = rucCliente.trim();
        payload.razonSocial = razonSocialCliente.trim();
        payload.documentoCliente = rucCliente.trim();
      }
      const venta = await registrarVenta(payload);
      const nro = venta?.nroComprobante || venta?.numeroDocumento || venta?.idVenta || '';
      const idVenta = venta?.idVenta;
      const totalCobrado = venta?.total != null ? Number(venta.total) : total;

      setVentaExitosa({
        idVenta,
        nroComprobante: nro,
        tipoDocumento: tipoComprobante,
        total: totalCobrado,
        metodoPago,
        vuelto: metodoPago === 'Efectivo' ? vuelto : 0,
        montoRecibido: metodoPago === 'Efectivo' ? montoRecibidoNum : totalCobrado,
      });
      setLineas([]);
      setBusqueda('');
      listProductos();
    } catch (error) {
      showToast(error?.message || 'No se pudo registrar la venta.');
    } finally {
      setProcesando(false);
    }
  }

  async function descargarPdfConToken(idVenta, nombre, abrir = true) {
    if (!idVenta || imprimiendo) return;
    setImprimiendo(true);
    try {
      const { tokenActivo, apiUrl } = await import('../api/client');
      const token = tokenActivo();
      const url = apiUrl(`/comprobantes/${idVenta}/pdf`);
      const response = await fetch(url, {
        headers: {
          Authorization: token ? `Bearer ${token}` : '',
          Accept: 'application/pdf',
        },
      });
      if (!response.ok) {
        throw new Error(`Error ${response.status} al descargar comprobante`);
      }
      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      if (abrir) {
        window.open(blobUrl, '_blank');
      } else {
        const enlace = document.createElement('a');
        enlace.href = blobUrl;
        enlace.download = `${nombre || `venta-${idVenta}`}.pdf`;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
      }
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
    } catch (error) {
      showToast(error?.message || 'No se pudo obtener el comprobante PDF.');
    } finally {
      setImprimiendo(false);
    }
  }

  async function imprimirTicket() {
    if (!ventaExitosa?.idVenta) return;
    await descargarPdfConToken(ventaExitosa.idVenta, ventaExitosa.nroComprobante || `venta-${ventaExitosa.idVenta}`, true);
  }

  async function verDescargarPdf() {
    if (!ventaExitosa?.idVenta) return;
    await descargarPdfConToken(ventaExitosa.idVenta, ventaExitosa.nroComprobante || `venta-${ventaExitosa.idVenta}`, true);
  }

  function siguienteCliente() {
    setVentaExitosa(null);
    setBusqueda('');
    setMetodoPago('Efectivo');
    setTipoComprobante('Boleta');
    setDniCliente('');
    setRucCliente('');
    setRazonSocialCliente('');
    setMontoRecibido('');
    buscadorRef.current?.focus();
  }

  const inicial = (tienda.nombre || '?').charAt(0).toUpperCase();

  return (
      <div className="pos">
        <header className="pos-topbar">
          <div className="pos-marca">
            {tienda.logoUrl
                ? <img className="pos-logo" src={tienda.logoUrl} alt="" />
                : <span className="pos-logo inicial" aria-hidden="true">{inicial}</span>}
            <div className="pos-marca-texto">
              <strong>{tienda.nombre}</strong>
              <span>Punto de venta</span>
            </div>
          </div>
          <div className="pos-topbar-acciones">
            <Link className="pos-enlace" to="/admin/ventas">
              <History size={18} aria-hidden="true" />
              Historial
            </Link>
            {esAdmin ? (
                <Link className="pos-enlace" to="/admin">
                  <ArrowLeft size={18} aria-hidden="true" />
                  Volver al panel
                </Link>
            ) : (
                <button className="pos-enlace" type="button" onClick={salir}>
                  <LogOut size={18} aria-hidden="true" />
                  Salir
                </button>
            )}
          </div>
        </header>

        <div className="pos-cuerpo">
          <section className="pos-izquierda" aria-label="Punto de venta">
            <div className="pos-buscador-envoltura">
              <div className="pos-buscador">
                <ScanBarcode size={24} aria-hidden="true" />
                <input
                    ref={buscadorRef}
                    type="search"
                    value={busqueda}
                    onChange={(evento) => setBusqueda(evento.target.value)}
                    onKeyDown={alPresionarTecla}
                    placeholder="Escanea un código o busca por nombre…"
                    aria-label="Buscar producto por código o nombre"
                    autoComplete="off"
                />
                <Search className="pos-buscador-lupa" size={20} aria-hidden="true" />
              </div>

              {termino && (
                  <div className="pos-sugerencias" role="listbox" aria-label="Productos encontrados">
                    {sugerencias.length === 0 && (
                        <p className="pos-sin-resultados">Sin coincidencias para “{busqueda.trim()}”.</p>
                    )}
                    {sugerencias.map((producto) => {
                      const stock = Number(producto.stock) || 0;
                      return (
                          <button
                              key={producto.idProducto}
                              className="pos-sugerencia"
                              type="button"
                              role="option"
                              aria-selected="false"
                              disabled={stock <= 0}
                              onClick={() => agregar(producto)}
                          >
                            <span className="pos-sugerencia-codigo">{producto.idProducto}</span>
                            <span className="pos-sugerencia-nombre">{producto.nombre}</span>
                            <span className={`pos-sugerencia-stock${stock <= 0 ? ' agotado' : ''}`}>
                        {stock <= 0 ? 'Agotado' : `Stock ${stock}`}
                      </span>
                            <span className="pos-sugerencia-precio">{dinero.format(precioDe(producto))}</span>
                            <Plus size={20} aria-hidden="true" />
                          </button>
                      );
                    })}
                  </div>
              )}
            </div>

            <div className="pos-tabla-envoltura">
              <table className="pos-tabla">
                <thead>
                <tr>
                  <th scope="col">Código</th>
                  <th scope="col">Descripción</th>
                  <th scope="col" className="centro">Cantidad</th>
                  <th scope="col" className="derecha">Subtotal</th>
                  <th scope="col"><span className="pos-oculto">Acciones</span></th>
                </tr>
                </thead>
                <tbody>
                {lineas.length === 0 && (
                    <tr>
                      <td colSpan="5">
                        <div className="pos-vacio">
                          {cargando
                              ? <LoaderCircle className="pos-girando" size={36} aria-hidden="true" />
                              : <ShoppingCart size={36} aria-hidden="true" />}
                          <strong>{cargando ? 'Cargando catálogo…' : 'El carrito está vacío'}</strong>
                          {!cargando && <span>Escanea un código o usa el buscador para agregar productos.</span>}
                        </div>
                      </td>
                    </tr>
                )}

                {lineas.map((linea) => (
                    <tr key={linea.idProducto}>
                      <td className="pos-codigo">{linea.idProducto}</td>
                      <td>
                        <div className="pos-descripcion">
                          <strong>{linea.nombre}</strong>
                          <span>{dinero.format(linea.precio)} c/u · Stock {linea.stock}</span>
                        </div>
                      </td>
                      <td>
                        <div className="pos-cantidad">
                          <button
                              type="button"
                              aria-label={`Quitar una unidad de ${linea.nombre}`}
                              title="Quitar una unidad"
                              onClick={() => cambiarCantidad(linea.idProducto, -1)}
                              disabled={linea.cantidad <= 1}
                          >
                            <Minus size={18} aria-hidden="true" />
                          </button>
                          <span aria-live="polite">{linea.cantidad}</span>
                          <button
                              type="button"
                              aria-label={`Agregar una unidad de ${linea.nombre}`}
                              title="Agregar una unidad"
                              onClick={() => cambiarCantidad(linea.idProducto, 1)}
                              disabled={linea.cantidad >= linea.stock}
                          >
                            <Plus size={18} aria-hidden="true" />
                          </button>
                        </div>
                      </td>
                      {/* CORRECCIÓN MATEMÁTICA APLICADA AQUÍ */}
                      <td className="pos-subtotal">{dinero.format(linea.precio * linea.cantidad)}</td>
                      <td className="pos-eliminar-celda">
                        <button
                            className="pos-eliminar"
                            type="button"
                            aria-label={`Eliminar ${linea.nombre}`}
                            title="Eliminar producto"
                            onClick={() => eliminar(linea.idProducto)}
                        >
                          <Trash2 size={19} aria-hidden="true" />
                        </button>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
            </div>

            <p className="pos-ayuda">
              Escanea el código de barras o escribe el código/nombre y presiona <kbd>Enter</kbd>.
            </p>
          </section>

          <aside className="pos-facturacion" aria-label="Resumen de facturación">
            <div className="pos-facturacion-cabecera">
              <div>
                <span className="pos-etiqueta">Resumen de venta</span>
                <h2>Facturación</h2>
              </div>
              <span className="pos-contador">
              {unidades === 1 ? '1 unidad' : `${unidades} unidades`}
            </span>
            </div>

            <dl className="pos-totales">
              <div>
                <dt>Subtotal</dt>
                <dd>{dinero.format(subtotal)}</dd>
              </div>
              <div>
                <dt>IGV ({IGV_PORCENTAJE}%)</dt>
                <dd>{dinero.format(igv)}</dd>
              </div>
              <div className="pos-total">
                <dt>TOTAL</dt>
                <dd>{dinero.format(total)}</dd>
              </div>
            </dl>

            <div className="pos-bloque">
              <span className="pos-etiqueta">Método de pago</span>
              <div className="pos-metodos">
                {METODOS_PAGO.map(({ nombre, icono: Icono }) => (
                    <button
                        key={nombre}
                        type="button"
                        className="pos-metodo"
                        aria-pressed={metodoPago === nombre}
                        onClick={() => setMetodoPago(nombre)}
                    >
                      <Icono size={22} aria-hidden="true" />
                      {nombre}
                    </button>
                ))}
              </div>
              {metodoPago === 'Efectivo' && (
                  <div className="pos-cliente-form">
                    <label className="pos-cliente-label" htmlFor="monto-recibido">Monto Recibido</label>
                    <input
                        id="monto-recibido"
                        className="pos-cliente-input"
                        type="number"
                        min="0"
                        step="0.01"
                        value={montoRecibido}
                        onChange={(e) => setMontoRecibido(e.target.value)}
                        placeholder={total.toFixed(2)}
                    />
                    <div className="pos-vuelto">
                      <span>Vuelto</span>
                      <strong>{dinero.format(vuelto)}</strong>
                    </div>
                    {pagoInsuficiente && <span className="pos-error">El monto recibido es menor al total.</span>}
                  </div>
              )}
            </div>

            <div className="pos-bloque">
              <span className="pos-etiqueta">Comprobante</span>
              <div className="pos-comprobantes">
                {['Boleta', 'Factura'].map((tipo) => (
                    <button
                        key={tipo}
                        type="button"
                        className="pos-comprobante"
                        aria-pressed={tipoComprobante === tipo}
                        onClick={() => setTipoComprobante(tipo)}
                    >
                      {tipo}
                    </button>
                ))}
              </div>
              {tipoComprobante === 'Boleta' && (
                  <div className="pos-cliente-form">
                    <label className="pos-cliente-label" htmlFor="dni-cliente">DNI (Opcional)</label>
                    <input
                        id="dni-cliente"
                        className="pos-cliente-input"
                        type="text"
                        inputMode="numeric"
                        maxLength={8}
                        value={dniCliente}
                        onChange={(e) => setDniCliente(e.target.value.replace(/\D/g, ''))}
                        placeholder="12345678"
                    />
                  </div>
              )}
              {tipoComprobante === 'Factura' && (
                  <div className="pos-cliente-form">
                    <label className="pos-cliente-label" htmlFor="ruc-cliente">RUC (11 dígitos) *</label>
                    <input
                        id="ruc-cliente"
                        className="pos-cliente-input"
                        type="text"
                        inputMode="numeric"
                        maxLength={11}
                        value={rucCliente}
                        onChange={(e) => setRucCliente(e.target.value.replace(/\D/g, ''))}
                        placeholder="20123456789"
                    />
                    <label className="pos-cliente-label" htmlFor="razon-cliente">Razón Social *</label>
                    <input
                        id="razon-cliente"
                        className="pos-cliente-input"
                        type="text"
                        value={razonSocialCliente}
                        onChange={(e) => setRazonSocialCliente(e.target.value)}
                        placeholder="Razón social de la empresa"
                    />
                  </div>
              )}
            </div>

            <button
                className="pos-generar"
                type="button"
                onClick={generarComprobante}
                disabled={lineas.length === 0 || procesando || pagoInsuficiente}
            >
              {procesando
                  ? <LoaderCircle className="pos-girando" size={22} aria-hidden="true" />
                  : <FileText size={22} aria-hidden="true" />}
              {procesando ? 'Procesando...' : 'Generar Comprobante (Boleta/Factura)'}
            </button>

            <p className="pos-nota">
              El importe incluye IGV. El comprobante se emite con los datos del cliente registrados en la venta.
            </p>
          </aside>
        </div>

        {ventaExitosa && (
            <div className="pos-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="venta-exitosa-titulo">
              <div className="pos-modal">
                <div className="pos-modal-icono">
                  <CheckCircle2 size={48} aria-hidden="true" />
                </div>
                <h2 id="venta-exitosa-titulo" className="pos-modal-titulo">¡Venta Exitosa!</h2>
                <p className="pos-modal-mensaje">La venta se registró correctamente.</p>
                <div className="pos-modal-detalle">
                  <div>
                    <span>Documento</span>
                    <strong>{ventaExitosa.tipoDocumento} {ventaExitosa.nroComprobante || '-'}</strong>
                  </div>
                  <div>
                    <span>Total cobrado</span>
                    <strong>{dinero.format(Number(ventaExitosa.total) || 0)}</strong>
                  </div>
                  <div>
                    <span>Método de pago</span>
                    <strong>{ventaExitosa.metodoPago}</strong>
                  </div>
                  {ventaExitosa.metodoPago === 'Efectivo' && (
                      <>
                        <div>
                          <span>Monto recibido</span>
                          <strong>{dinero.format(Number(ventaExitosa.montoRecibido) || 0)}</strong>
                        </div>
                        <div>
                          <span>Vuelto</span>
                          <strong>{dinero.format(Number(ventaExitosa.vuelto) || 0)}</strong>
                        </div>
                      </>
                  )}
                </div>
                <div className="pos-modal-acciones">
                  <button className="pos-modal-btn primario" type="button" onClick={verDescargarPdf} disabled={!ventaExitosa.idVenta || imprimiendo}>
                    {imprimiendo ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" /> : <FileText size={18} aria-hidden="true" />}
                    {imprimiendo ? 'Abriendo...' : '👁️ Ver / Descargar PDF'}
                  </button>
                  <button className="pos-modal-btn secundario" type="button" onClick={imprimirTicket} disabled={!ventaExitosa.idVenta || imprimiendo}>
                    <Printer size={18} aria-hidden="true" />
                    🖨️ Imprimir Ticket
                  </button>
                  <button className="pos-modal-btn secundario" type="button" onClick={siguienteCliente}>
                    <UserPlus size={18} aria-hidden="true" />
                    Siguiente Cliente
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}