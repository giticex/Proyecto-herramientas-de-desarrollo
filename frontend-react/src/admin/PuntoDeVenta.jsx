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
  RotateCcw,
  ScanBarcode,
  Search,
  ShoppingCart,
  Smartphone,
  Trash2,
  UserPlus,
  Wallet,
} from 'lucide-react';
import { useSesion } from '../auth/sesionStore';
import { useStore } from '../store/store';
import { showToast } from '../components/toastBus';
import {
  listarVentas,
  obtenerVentaParaDevolucion,
  registrarDevolucion,
  registrarVenta,
} from '../api/ventasApi';
import { buscarCredito } from '../api/creditosApi';
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
  { nombre: 'Crédito a Favor', icono: Wallet },
];

const METODO_CREDITO = 'Crédito a Favor';

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
  const [creditoCodigo, setCreditoCodigo] = useState('');
  const [creditoValidando, setCreditoValidando] = useState(false);
  const [creditoAplicado, setCreditoAplicado] = useState(null);
  const [creditoError, setCreditoError] = useState('');
  const [metodoPagoResto, setMetodoPagoResto] = useState('Efectivo');
  const [modalDevoluciones, setModalDevoluciones] = useState(false);
  const [busquedaDevolucion, setBusquedaDevolucion] = useState('');
  const [buscandoDevolucion, setBuscandoDevolucion] = useState(false);
  const [resultadosDevolucion, setResultadosDevolucion] = useState([]);
  const [ventaDevolucion, setVentaDevolucion] = useState(null);
  const [cantidadesDevolucion, setCantidadesDevolucion] = useState({});
  const [motivoDevolucion, setMotivoDevolucion] = useState('');
  const [procesandoDevolucion, setProcesandoDevolucion] = useState(false);
  const [devolucionExitosa, setDevolucionExitosa] = useState(null);
  const buscadorRef = useRef(null);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([listProductos(), getTienda('mi-tienda')]).finally(() => {
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
        .filter((producto) => {
          const codigo = String(producto.codigo || '').toLowerCase();
          const idProd = String(producto.idProducto || '').toLowerCase();
          const nombre = String(producto.nombre || '').toLowerCase();
          return codigo.includes(termino) || idProd.includes(termino) || nombre.includes(termino);
        })
        .sort((a, b) => {
          const aCodigo = String(a.codigo || '').toLowerCase();
          const bCodigo = String(b.codigo || '').toLowerCase();
          const aId = String(a.idProducto || '').toLowerCase();
          const bId = String(b.idProducto || '').toLowerCase();
          const aExact = aCodigo === termino || aId === termino;
          const bExact = bCodigo === termino || bId === termino;
          if (aExact && !bExact) return -1;
          if (!aExact && bExact) return 1;
          return 0;
        })
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

  // Crédito a favor: cubre hasta el total de la venta y el resto (si hay) se
  // cobra con otro método elegido por el cajero.
  const pagoConCredito = metodoPago === METODO_CREDITO;
  const creditoDisponible = creditoAplicado ? Number(creditoAplicado.montoDisponible) || 0 : 0;
  const creditoUsado = pagoConCredito && creditoAplicado
    ? redondear(Math.min(creditoDisponible, total))
    : 0;
  const restantePorCobrar = pagoConCredito ? redondear(total - creditoUsado) : total;
  const metodoCobro = pagoConCredito
    ? (restantePorCobrar > 0 ? metodoPagoResto : null)
    : metodoPago;
  const vuelto = metodoCobro === 'Efectivo'
    ? Math.max(0, redondear(montoRecibidoNum - restantePorCobrar))
    : 0;
  // Con crédito, el efectivo solo tiene sentido una vez validado el código:
  // antes de eso todavía no se sabe cuánto queda por cobrar.
  const cobroEfectivoListo = metodoCobro === 'Efectivo'
    && (!pagoConCredito || Boolean(creditoAplicado));
  const pagoInsuficiente = cobroEfectivoListo
    && lineas.length > 0
    && montoRecibidoNum < restantePorCobrar;
  const mostrarMontoEfectivo = cobroEfectivoListo;

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
          codigo: producto.codigo,
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
    if (pagoConCredito) {
      if (!creditoAplicado) {
        showToast('Valida el código del crédito a favor antes de cobrar.');
        return;
      }
      if (creditoUsado <= 0) {
        showToast('El crédito no tiene saldo para aplicar a esta venta.');
        return;
      }
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
        // Pago mixto: el crédito cubre parte del total y el medio de pago
        // registrado corresponde a lo cobrado por el resto.
        medioPago: pagoConCredito && restantePorCobrar > 0 ? metodoPagoResto : metodoPago,
        items,
      };
      if (pagoConCredito && creditoAplicado) {
        payload.creditoCodigo = creditoAplicado.codigo;
      }
      if (tipoComprobante === 'Boleta' && dniCliente.trim()) {
        payload.documentoCliente = dniCliente.trim();
      }
      if (tipoComprobante === 'Factura') {
        payload.documentoCliente = rucCliente.trim();
        payload.razonSocial = razonSocialCliente.trim();
      }
      const venta = await registrarVenta(payload);
      // El backend devuelve la venta con los getters de la entidad: el id
      // viaja como `id` y el total como `total`/`totalVenta`.
      const idVenta = venta?.id ?? venta?.idVenta;
      const nro = venta?.numeroDocumento || venta?.nroComprobante || '';
      const totalCobrado = venta?.total ?? venta?.totalVenta ?? total;
      const creditoMontoUsado = Number(venta?.creditoMontoUsado ?? creditoUsado) || 0;
      const creditoSaldoRestante = Number(
        venta?.creditoSaldoRestante ?? (creditoDisponible - creditoMontoUsado),
      ) || 0;
      const etiquetaMetodo = pagoConCredito
        ? `Crédito a Favor${restantePorCobrar > 0 ? ` + ${metodoPagoResto}` : ''}`
        : metodoPago;
      const cobraEfectivo = metodoCobro === 'Efectivo';

      setVentaExitosa({
        idVenta,
        nroComprobante: nro,
        tipoDocumento: tipoComprobante,
        total: totalCobrado,
        metodoPago: etiquetaMetodo,
        efectivo: cobraEfectivo,
        vuelto: cobraEfectivo ? vuelto : 0,
        montoRecibido: cobraEfectivo ? montoRecibidoNum : totalCobrado,
        creditoCodigo: pagoConCredito ? (venta?.creditoCodigo || creditoAplicado?.codigo) : null,
        creditoUsado: pagoConCredito ? creditoMontoUsado : 0,
        creditoSaldoRestante: pagoConCredito ? creditoSaldoRestante : 0,
      });
      setLineas([]);
      setBusqueda('');
      setCreditoCodigo('');
      setCreditoAplicado(null);
      setCreditoError('');
      listProductos();
    } catch (error) {
      showToast(error?.message || 'No se pudo registrar la venta.');
    } finally {
      setProcesando(false);
    }
  }

  async function abrirComprobante(idVenta, nombre, formato) {
    if (!idVenta) {
      showToast(formato === 'ticket'
        ? 'No se encontró el ID de la venta para imprimir el ticket.'
        : 'No se encontró el ID de la venta para descargar el comprobante.');
      return;
    }
    if (imprimiendo) return;
    setImprimiendo(true);
    try {
      await descargarArchivoSeguro(
        `/comprobantes/${idVenta}/${formato}`,
        'PDF',
        nombre || `venta-${idVenta}`,
        { abrir: true }
      );
    } catch (error) {
      showToast(error?.message || 'No se pudo obtener el comprobante PDF.');
    } finally {
      setImprimiendo(false);
    }
  }

  async function imprimirTicket() {
    const idVenta = ventaExitosa?.idVenta;
    await abrirComprobante(idVenta, `ticket-${ventaExitosa?.nroComprobante || idVenta}`, 'ticket');
  }

  async function verDescargarPdf() {
    const idVenta = ventaExitosa?.idVenta;
    await abrirComprobante(idVenta, ventaExitosa?.nroComprobante || `venta-${idVenta}`, 'pdf');
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
    setCreditoCodigo('');
    setCreditoAplicado(null);
    setCreditoError('');
    setMetodoPagoResto('Efectivo');
    buscadorRef.current?.focus();
  }

  function elegirMetodoPago(nombre) {
    setMetodoPago(nombre);
    if (nombre !== METODO_CREDITO) {
      setCreditoCodigo('');
      setCreditoAplicado(null);
      setCreditoError('');
    }
  }

  async function validarCredito(evento) {
    evento?.preventDefault();
    const codigo = creditoCodigo.trim();
    if (!codigo) {
      setCreditoError('Ingresa el código del crédito (BOL-CR-XXXXXX).');
      return;
    }
    if (creditoValidando) return;
    setCreditoValidando(true);
    setCreditoError('');
    try {
      const credito = await buscarCredito(codigo);
      const disponible = Number(credito?.montoDisponible) || 0;
      if (credito?.estado !== 'Vigente') {
        setCreditoAplicado(null);
        setCreditoError(`El crédito está en estado ${credito?.estado || 'no vigente'}.`);
        return;
      }
      if (disponible <= 0) {
        setCreditoAplicado(null);
        setCreditoError('El crédito no tiene saldo disponible.');
        return;
      }
      setCreditoAplicado({
        codigo: credito.codigo || codigo.toUpperCase(),
        clienteNombre: credito.clienteNombre,
        montoDisponible: disponible,
      });
      setCreditoCodigo(credito.codigo || codigo.toUpperCase());
      showToast(`Crédito ${credito.codigo || codigo} validado: ${dinero.format(disponible)} disponibles.`);
    } catch (error) {
      setCreditoAplicado(null);
      setCreditoError(error?.message || 'No se pudo validar el crédito.');
    } finally {
      setCreditoValidando(false);
    }
  }

  function quitarCredito() {
    setCreditoCodigo('');
    setCreditoAplicado(null);
    setCreditoError('');
  }

  function abrirModalDevoluciones() {
    setModalDevoluciones(true);
    setBusquedaDevolucion('');
    setResultadosDevolucion([]);
    setVentaDevolucion(null);
    setCantidadesDevolucion({});
    setMotivoDevolucion('');
    setDevolucionExitosa(null);
  }

  function cerrarModalDevoluciones() {
    if (procesandoDevolucion) return;
    setModalDevoluciones(false);
    setDevolucionExitosa(null);
  }

  async function buscarVentasDevolucion(evento) {
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
    } catch (error) {
      showToast(error?.message || 'No se pudo buscar la venta.');
    } finally {
      setBuscandoDevolucion(false);
    }
  }

  async function elegirVentaDevolucion(venta) {
    try {
      const detalle = await obtenerVentaParaDevolucion(venta.idVenta);
      setVentaDevolucion(detalle);
      setCantidadesDevolucion({});
      setDevolucionExitosa(null);
    } catch (error) {
      showToast(error?.message || 'No se pudo cargar la venta.');
    }
  }

  function cambiarCantidadDevolucion(idProducto, maximo, delta) {
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
      ? redondear(ventaDevolucion.detalles.reduce(
          (suma, detalle) => suma
              + (Number(detalle.precioUnitario) || 0) * (cantidadesDevolucion[detalle.idProducto] || 0),
          0))
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
      listProductos();
    } catch (error) {
      showToast(error?.message || 'No se pudo registrar la devolución.');
    } finally {
      setProcesandoDevolucion(false);
    }
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
            <button className="pos-enlace" type="button" onClick={abrirModalDevoluciones}>
              <RotateCcw size={18} aria-hidden="true" />
              Devoluciones
            </button>
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
                            <span className="pos-sugerencia-codigo">{producto.codigo || producto.idProducto}</span>
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
                      <td className="pos-codigo">{linea.codigo || linea.idProducto}</td>
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
              {pagoConCredito && creditoUsado > 0 && (
                  <>
                    <div className="pos-credito-descuento">
                      <dt>Crédito a favor</dt>
                      <dd>-{dinero.format(creditoUsado)}</dd>
                    </div>
                    <div className="pos-total pos-total-cobrar">
                      <dt>A COBRAR</dt>
                      <dd>{dinero.format(restantePorCobrar)}</dd>
                    </div>
                  </>
              )}
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
                        onClick={() => elegirMetodoPago(nombre)}
                    >
                      <Icono size={22} aria-hidden="true" />
                      {nombre}
                    </button>
                ))}
              </div>
              {pagoConCredito && (
                  <div className="pos-credito">
                    <form className="pos-cliente-form" onSubmit={validarCredito}>
                      <label className="pos-cliente-label" htmlFor="credito-codigo">Código del crédito</label>
                      <div className="pos-credito-fila">
                        <input
                            id="credito-codigo"
                            className="pos-cliente-input"
                            type="text"
                            value={creditoCodigo}
                            onChange={(evento) => setCreditoCodigo(evento.target.value.toUpperCase())}
                            placeholder="BOL-CR-000001"
                            autoComplete="off"
                            disabled={Boolean(creditoAplicado) || creditoValidando}
                        />
                        {creditoAplicado ? (
                            <button className="pos-credito-btn" type="button" onClick={quitarCredito}>
                              Quitar
                            </button>
                        ) : (
                            <button
                                className="pos-credito-btn"
                                type="submit"
                                disabled={creditoValidando || !creditoCodigo.trim()}
                            >
                              {creditoValidando
                                  ? <LoaderCircle size={16} className="pos-girando" aria-hidden="true" />
                                  : <Search size={16} aria-hidden="true" />}
                              {creditoValidando ? 'Validando...' : 'Validar'}
                            </button>
                        )}
                      </div>
                      {creditoError && <span className="pos-error" role="alert">{creditoError}</span>}
                    </form>
                    {creditoAplicado && (
                        <div className="pos-credito-resumen">
                          <div>
                            <span>Saldo del crédito</span>
                            <strong>{dinero.format(creditoDisponible)}</strong>
                          </div>
                          <div>
                            <span>Se aplicará</span>
                            <strong>{dinero.format(creditoUsado)}</strong>
                          </div>
                          {restantePorCobrar > 0 ? (
                              <div>
                                <span>Resto por cobrar</span>
                                <strong>{dinero.format(restantePorCobrar)}</strong>
                              </div>
                          ) : (
                              lineas.length > 0 && (
                                  <div>
                                    <span>Saldo restante del crédito</span>
                                    <strong>{dinero.format(redondear(creditoDisponible - creditoUsado))}</strong>
                                  </div>
                              )
                          )}
                        </div>
                    )}
                    {creditoAplicado && restantePorCobrar > 0 && (
                        <div className="pos-credito-resto">
                          <span className="pos-etiqueta">Cobrar el resto con</span>
                          <div className="pos-metodos">
                            {METODOS_PAGO
                                .filter(({ nombre }) => nombre !== METODO_CREDITO)
                                .map(({ nombre, icono: Icono }) => (
                                  <button
                                      key={nombre}
                                      type="button"
                                      className="pos-metodo"
                                      aria-pressed={metodoPagoResto === nombre}
                                      onClick={() => setMetodoPagoResto(nombre)}
                                  >
                                    <Icono size={22} aria-hidden="true" />
                                    {nombre}
                                  </button>
                            ))}
                          </div>
                          {metodoPagoResto === 'Efectivo' && (
                              <span className="pos-credito-nota">
                                Ingresa el monto recibido por el resto en la sección de abajo.
                              </span>
                          )}
                        </div>
                    )}
                  </div>
              )}
              {mostrarMontoEfectivo && (
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
                        placeholder={restantePorCobrar.toFixed(2)}
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
                disabled={lineas.length === 0 || procesando || pagoInsuficiente
                    || (pagoConCredito && !creditoAplicado)}
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

        {modalDevoluciones && (
            <div className="pos-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="devoluciones-titulo">
              <div className="pos-modal pos-modal-devolucion">
                {devolucionExitosa ? (
                    <>
                      <div className="pos-modal-icono">
                        <CheckCircle2 size={48} aria-hidden="true" />
                      </div>
                      <h2 id="devoluciones-titulo" className="pos-modal-titulo">¡Devolución registrada!</h2>
                      <p className="pos-modal-mensaje">
                        El stock de los productos devueltos fue restaurado.
                      </p>
                      <div className="pos-modal-detalle">
                        <div>
                          <span>N° de devolución</span>
                          <strong>{devolucionExitosa.idDevolucion}</strong>
                        </div>
                        <div>
                          <span>Total devuelto</span>
                          <strong>{dinero.format(Number(devolucionExitosa.total) || 0)}</strong>
                        </div>
                      </div>
                      <div className="pos-modal-acciones">
                        <button className="pos-modal-btn primario" type="button" onClick={cerrarModalDevoluciones}>
                          Cerrar
                        </button>
                      </div>
                    </>
                ) : !ventaDevolucion ? (
                    <>
                      <h2 id="devoluciones-titulo" className="pos-modal-titulo">Buscar venta a devolver</h2>
                      <p className="pos-modal-mensaje">
                        Busca por número de Boleta/Factura (ej. BOL-000123) o por DNI del cliente.
                      </p>
                      <form className="pos-devo-busqueda" onSubmit={buscarVentasDevolucion}>
                        <input
                            type="search"
                            value={busquedaDevolucion}
                            onChange={(evento) => setBusquedaDevolucion(evento.target.value)}
                            placeholder="BOL-000123 o 12345678"
                            aria-label="Buscar venta por número de comprobante o DNI"
                            autoComplete="off"
                        />
                        <button className="pos-modal-btn primario" type="submit" disabled={buscandoDevolucion}>
                          {buscandoDevolucion
                              ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                              : <Search size={18} aria-hidden="true" />}
                          {buscandoDevolucion ? 'Buscando...' : 'Buscar'}
                        </button>
                      </form>
                      {resultadosDevolucion.length > 0 && (
                          <div className="pos-devo-resultados">
                            {resultadosDevolucion.map((venta) => (
                                <button
                                    key={venta.idVenta}
                                    type="button"
                                    className="pos-devo-resultado"
                                    onClick={() => elegirVentaDevolucion(venta)}
                                >
                                  <span className="pos-devo-resultado-doc">
                                    <strong>{venta.numeroDocumento}</strong>
                                    <small>{venta.cliente || 'Cliente'}
                                      {venta.documentoCliente ? ` · ${venta.documentoCliente}` : ''}</small>
                                  </span>
                                  <span className="pos-devo-resultado-total">
                                    {dinero.format(Number(venta.total) || 0)}
                                  </span>
                                </button>
                            ))}
                          </div>
                      )}
                      <div className="pos-modal-acciones">
                        <button className="pos-modal-btn secundario" type="button" onClick={cerrarModalDevoluciones}>
                          Cerrar
                        </button>
                      </div>
                    </>
                ) : (
                    <>
                      <h2 id="devoluciones-titulo" className="pos-modal-titulo">
                        Devolver productos · {ventaDevolucion.numeroDocumento}
                      </h2>
                      <p className="pos-modal-mensaje">
                        {ventaDevolucion.tipoDocumento || 'Comprobante'} · {ventaDevolucion.receptorRazonSocial || 'Cliente'}
                        {ventaDevolucion.receptorNumDocumento ? ` (${ventaDevolucion.receptorNumDocumento})` : ''}
                        {' '}· {new Date(ventaDevolucion.fechaEmision).toLocaleString('es-PE')}
                      </p>
                      <div className="pos-devo-lineas">
                        {ventaDevolucion.detalles.map((detalle) => {
                          const maximo = detalle.cantidad - (detalle.cantidadDevuelta || 0);
                          const elegida = cantidadesDevolucion[detalle.idProducto] || 0;
                          return (
                              <div key={detalle.idDetalle ?? detalle.idProducto} className="pos-devo-linea">
                                <div className="pos-devo-linea-info">
                                  <strong>{detalle.descripcion}</strong>
                                  <small>
                                    {dinero.format(Number(detalle.precioUnitario) || 0)} c/u
                                    · Vendidas {detalle.cantidad}
                                    {(detalle.cantidadDevuelta || 0) > 0 && ` · Ya devueltas ${detalle.cantidadDevuelta}`}
                                  </small>
                                </div>
                                <div className="pos-cantidad">
                                  <button
                                      type="button"
                                      aria-label={`Quitar una unidad de ${detalle.descripcion}`}
                                      onClick={() => cambiarCantidadDevolucion(detalle.idProducto, maximo, -1)}
                                      disabled={elegida <= 0}
                                  >
                                    <Minus size={18} aria-hidden="true" />
                                  </button>
                                  <span aria-live="polite">{elegida}</span>
                                  <button
                                      type="button"
                                      aria-label={`Agregar una unidad de ${detalle.descripcion}`}
                                      onClick={() => cambiarCantidadDevolucion(detalle.idProducto, maximo, 1)}
                                      disabled={elegida >= maximo}
                                  >
                                    <Plus size={18} aria-hidden="true" />
                                  </button>
                                </div>
                              </div>
                          );
                        })}
                      </div>
                      <label className="pos-cliente-label" htmlFor="motivo-devolucion">Motivo (opcional)</label>
                      <input
                          id="motivo-devolucion"
                          className="pos-cliente-input"
                          type="text"
                          maxLength={300}
                          value={motivoDevolucion}
                          onChange={(evento) => setMotivoDevolucion(evento.target.value)}
                          placeholder="Ej. Producto defectuoso"
                      />
                      <div className="pos-devo-total">
                        <span>Total a devolver</span>
                        <strong>{dinero.format(totalDevolucion)}</strong>
                      </div>
                      <div className="pos-modal-acciones">
                        <button
                            className="pos-modal-btn primario"
                            type="button"
                            onClick={confirmarDevolucion}
                            disabled={procesandoDevolucion || itemsDevolucion.length === 0}
                        >
                          {procesandoDevolucion
                              ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                              : <RotateCcw size={18} aria-hidden="true" />}
                          {procesandoDevolucion ? 'Procesando...' : 'Confirmar Devolución'}
                        </button>
                        <button
                            className="pos-modal-btn secundario"
                            type="button"
                            onClick={() => setVentaDevolucion(null)}
                            disabled={procesandoDevolucion}
                        >
                          Volver a la búsqueda
                        </button>
                        <button className="pos-modal-btn secundario" type="button" onClick={cerrarModalDevoluciones} disabled={procesandoDevolucion}>
                          Cerrar
                        </button>
                      </div>
                    </>
                )}
              </div>
            </div>
        )}

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
                  {ventaExitosa.creditoCodigo && (
                      <>
                        <div>
                          <span>Crédito aplicado ({ventaExitosa.creditoCodigo})</span>
                          <strong>{dinero.format(Number(ventaExitosa.creditoUsado) || 0)}</strong>
                        </div>
                        <div>
                          <span>Saldo restante del crédito</span>
                          <strong>{dinero.format(Number(ventaExitosa.creditoSaldoRestante) || 0)}</strong>
                        </div>
                      </>
                  )}
                  {ventaExitosa.efectivo && (
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
                  <button className="pos-modal-btn primario" type="button" onClick={verDescargarPdf} disabled={imprimiendo || !ventaExitosa.idVenta}>
                    {imprimiendo ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" /> : <FileText size={18} aria-hidden="true" />}
                    {imprimiendo ? 'Abriendo...' : 'Ver / Descargar PDF'}
                  </button>
                  <button className="pos-modal-btn secundario" type="button" onClick={imprimirTicket} disabled={imprimiendo || !ventaExitosa.idVenta}>
                    <Printer size={18} aria-hidden="true" />
                    Imprimir Ticket
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