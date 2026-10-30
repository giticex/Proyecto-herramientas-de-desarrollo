import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Banknote,
  Calculator,
  CheckCircle2,
  CreditCard,
  FileText,
  History,
  LoaderCircle,
  Lock,
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
  Unlock,
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
import { buscarClientePorDocumento, registrarClienteRapido } from '../api/clientesApi';
import { abrirCaja, cerrarCaja, obtenerCaja } from '../api/cajaApi';
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
  const [accionArchivo, setAccionArchivo] = useState(null);
  const [dniCliente, setDniCliente] = useState('');
  const [rucCliente, setRucCliente] = useState('');
  const [razonSocialCliente, setRazonSocialCliente] = useState('');
  const [documentoCliente, setDocumentoCliente] = useState('');
  const [clienteEncontrado, setClienteEncontrado] = useState(null);
  const [estadoCliente, setEstadoCliente] = useState('');
  const [buscandoCliente, setBuscandoCliente] = useState(false);
  const [clienteError, setClienteError] = useState('');
  const [direccionCliente, setDireccionCliente] = useState('');
  const [registroRapido, setRegistroRapido] = useState(false);
  const [nuevoCliente, setNuevoCliente] = useState({ nombre: '', direccion: '', telefono: '' });
  const [registrandoCliente, setRegistrandoCliente] = useState(false);
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
  const [caja, setCaja] = useState(null);
  const [cajaCargando, setCajaCargando] = useState(true);
  const [cajaError, setCajaError] = useState('');
  const [montoApertura, setMontoApertura] = useState('');
  const [abriendoCaja, setAbriendoCaja] = useState(false);
  const [modalCierre, setModalCierre] = useState(false);
  const [efectivoContado, setEfectivoContado] = useState('');
  const [observacionCierre, setObservacionCierre] = useState('');
  const [cerrandoCaja, setCerrandoCaja] = useState(false);
  const [cierreResultado, setCierreResultado] = useState(null);
  const buscadorRef = useRef(null);
  const ultimoDocumentoBuscado = useRef('');

  const turnoAbierto = Boolean(caja?.turnoAbierto);

  const cargarCaja = useCallback(async () => {
    try {
      const estado = await obtenerCaja();
      setCaja(estado ?? null);
      setCajaError('');
    } catch (error) {
      setCajaError(error?.message || 'No se pudo consultar el turno de caja.');
    } finally {
      setCajaCargando(false);
    }
  }, []);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([listProductos(), getTienda('mi-tienda'), cargarCaja()]).finally(() => {
      if (vigente) {
        setCargando(false);
        buscadorRef.current?.focus();
      }
    });
    return () => {
      vigente = false;
    };
  }, [listProductos, getTienda, cargarCaja]);

  // La caja abierta es requisito para vender: al enfocar el buscador solo se
  // suelta el foco cuando el turno esta registrado.
  useEffect(() => {
    if (turnoAbierto) buscadorRef.current?.focus();
  }, [turnoAbierto]);

  // Búsqueda de comprador por DNI/RUC en la tienda del token. Al aplicar
  // una ficha se sincronizan el tipo de comprobante, el documento y la
  // dirección que viajan en la venta.
  const aplicarCliente = useCallback((cliente) => {
    const documento = String(cliente?.numeroDocumento || '');
    setClienteEncontrado(cliente);
    setEstadoCliente('encontrado');
    setDocumentoCliente(documento);
    setDireccionCliente(cliente?.direccion || '');
    setClienteError('');
    ultimoDocumentoBuscado.current = documento;
    if (documento.length === 11) {
      setTipoComprobante('Factura');
      setRucCliente(documento);
      setDniCliente('');
    } else {
      setTipoComprobante('Boleta');
      setDniCliente(documento);
      setRucCliente('');
    }
    setRazonSocialCliente(cliente?.nombre || '');
  }, []);

  const consultarCliente = useCallback(async (documento) => {
    const limpio = String(documento || '').replace(/\D/g, '');
    if (limpio.length !== 8 && limpio.length !== 11) {
      setEstadoCliente('');
      setClienteError('Ingresa un DNI de 8 dígitos o un RUC de 11 dígitos.');
      return;
    }
    setBuscandoCliente(true);
    setClienteError('');
    setEstadoCliente('buscando');
    try {
      const cliente = await buscarClientePorDocumento(limpio);
      ultimoDocumentoBuscado.current = limpio;
      if (cliente?.encontrado) {
        aplicarCliente(cliente);
      } else {
        setClienteEncontrado(null);
        setDireccionCliente('');
        setRazonSocialCliente('');
        setEstadoCliente('no-encontrado');
        setNuevoCliente({ nombre: '', direccion: '', telefono: '' });
      }
    } catch (error) {
      setClienteEncontrado(null);
      setEstadoCliente('error');
      setClienteError(error?.message || 'No se pudo buscar el cliente.');
    } finally {
      setBuscandoCliente(false);
    }
  }, [aplicarCliente]);

  // Autocompletado: con 8 (DNI) u 11 (RUC) dígitos se consulta sola tras una
  // pausa; el botón Buscar permite forzarla de inmediato.
  useEffect(() => {
    const limpio = documentoCliente.replace(/\D/g, '');
    if ((limpio.length !== 8 && limpio.length !== 11)
        || limpio === ultimoDocumentoBuscado.current) {
      return undefined;
    }
    const temporizador = setTimeout(() => consultarCliente(limpio), 600);
    return () => clearTimeout(temporizador);
  }, [documentoCliente, consultarCliente]);

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

  function cambiarDocumentoCliente(valor) {
    const limpio = valor.replace(/\D/g, '').slice(0, 11);
    setDocumentoCliente(limpio);
    setClienteError('');
    // Al corregir el documento se descarta la ficha aplicada y sus datos en
    // el comprobante, para no emitir con el DNI/RUC de otro cliente.
    if (clienteEncontrado && limpio !== clienteEncontrado.numeroDocumento) {
      setClienteEncontrado(null);
      setDireccionCliente('');
      setDniCliente('');
      setRucCliente('');
      setRazonSocialCliente('');
    }
    if (limpio.length !== 8 && limpio.length !== 11) {
      setEstadoCliente('');
      setRegistroRapido(false);
    } else if (!clienteEncontrado && limpio === ultimoDocumentoBuscado.current) {
      // Volver al mismo documento ya consultado (p. ej. tras corregir un
      // dígito) no dispara el debounce: se repite la consulta aquí.
      consultarCliente(limpio);
    }
  }

  function buscarClienteManual(evento) {
    evento.preventDefault();
    if (buscandoCliente) return;
    consultarCliente(documentoCliente);
  }

  function quitarClienteEncontrado() {
    setClienteEncontrado(null);
    setDireccionCliente('');
    setDniCliente('');
    setRucCliente('');
    setRazonSocialCliente('');
    setEstadoCliente('');
    setRegistroRapido(false);
    setNuevoCliente({ nombre: '', direccion: '', telefono: '' });
    // Se conserva el documento escrito: si se dejara vacío el marcador, el
    // autocompletado volvería a buscar y a mostrar la ficha recién quitada.
    ultimoDocumentoBuscado.current = documentoCliente;
  }

  async function guardarClienteRapido(evento) {
    evento.preventDefault();
    const limpio = documentoCliente.replace(/\D/g, '');
    if (limpio.length !== 8 && limpio.length !== 11) {
      setClienteError('El documento debe tener 8 (DNI) u 11 (RUC) dígitos.');
      return;
    }
    const nombre = nuevoCliente.nombre.trim();
    if (!nombre) {
      setClienteError('Indica el nombre o la razón social.');
      return;
    }
    if (registrandoCliente) return;
    setRegistrandoCliente(true);
    setClienteError('');
    try {
      const cliente = await registrarClienteRapido({
        documento: limpio,
        nombre,
        direccion: nuevoCliente.direccion.trim(),
        telefono: nuevoCliente.telefono.trim(),
      });
      aplicarCliente(cliente);
      setRegistroRapido(false);
      showToast('Cliente registrado. Sus datos quedaron asociados a la venta.');
    } catch (error) {
      setClienteError(error?.message || 'No se pudo registrar el cliente.');
    } finally {
      setRegistrandoCliente(false);
    }
  }

  function cambiarDniCliente(valor) {
    setDniCliente(valor.replace(/\D/g, ''));
    if (clienteEncontrado) {
      setClienteEncontrado(null);
      setDireccionCliente('');
      setRazonSocialCliente('');
    }
  }

  function cambiarRucCliente(valor) {
    setRucCliente(valor.replace(/\D/g, ''));
    if (clienteEncontrado) {
      setClienteEncontrado(null);
      setDireccionCliente('');
      setRazonSocialCliente('');
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
      // Ficha elegida en la búsqueda: la venta queda asociada a ese cliente de
      // la tienda y guarda la dirección fiscal autocompletada.
      if (clienteEncontrado?.idCliente) {
        payload.idCliente = clienteEncontrado.idCliente;
      }
      if (direccionCliente.trim()) {
        payload.direccionCliente = direccionCliente.trim();
      }
      if (pagoConCredito && creditoAplicado) {
        payload.creditoCodigo = creditoAplicado.codigo;
      }
      if (tipoComprobante === 'Boleta' && dniCliente.trim()) {
        payload.documentoCliente = dniCliente.trim();
        if (razonSocialCliente.trim()) {
          payload.razonSocial = razonSocialCliente.trim();
        }
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
      cargarCaja();
    } catch (error) {
      showToast(error?.message || 'No se pudo registrar la venta.');
    } finally {
      setProcesando(false);
    }
  }

  async function abrirComprobante(idVenta, nombre, formato, abrir = true) {
    if (!idVenta) {
      showToast(formato === 'ticket'
        ? 'No se encontró el ID de la venta para imprimir el ticket.'
        : 'No se encontró el ID de la venta para descargar el comprobante.');
      return;
    }
    if (accionArchivo) return;
    setAccionArchivo(formato);
    try {
      await descargarArchivoSeguro(
        `/comprobantes/${idVenta}/${formato}`,
        'PDF',
        nombre || `venta-${idVenta}`,
        { abrir }
      );
    } catch (error) {
      showToast(error?.message || 'No se pudo obtener el comprobante PDF.');
    } finally {
      setAccionArchivo(null);
    }
  }

  async function imprimirTicket() {
    const idVenta = ventaExitosa?.idVenta;
    await abrirComprobante(idVenta, `ticket-${ventaExitosa?.nroComprobante || idVenta}`, 'ticket');
  }

  async function descargarPdf() {
    const idVenta = ventaExitosa?.idVenta;
    await abrirComprobante(
      idVenta,
      ventaExitosa?.nroComprobante || `venta-${idVenta}`,
      'pdf',
      false
    );
  }

  function siguienteCliente() {
    setVentaExitosa(null);
    setBusqueda('');
    setMetodoPago('Efectivo');
    setTipoComprobante('Boleta');
    setDniCliente('');
    setRucCliente('');
    setRazonSocialCliente('');
    setDocumentoCliente('');
    setClienteEncontrado(null);
    setEstadoCliente('');
    setClienteError('');
    setDireccionCliente('');
    setRegistroRapido(false);
    setNuevoCliente({ nombre: '', direccion: '', telefono: '' });
    ultimoDocumentoBuscado.current = '';
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

  // ------------------------------------------------------------------
  // Apertura y cierre de caja
  // ------------------------------------------------------------------

  async function registrarApertura(evento) {
    evento?.preventDefault();
    const monto = Number(montoApertura);
    if (!String(montoApertura).trim() || Number.isNaN(monto) || monto < 0) {
      showToast('Ingresa el monto de apertura en efectivo (puede ser 0.00).');
      return;
    }
    if (abriendoCaja) return;
    setAbriendoCaja(true);
    try {
      const estado = await abrirCaja(redondear(monto));
      setCaja(estado);
      setMontoApertura('');
      showToast('Caja abierta. Ya puedes registrar ventas.');
    } catch (error) {
      showToast(error?.message || 'No se pudo abrir la caja.');
      await cargarCaja();
    } finally {
      setAbriendoCaja(false);
    }
  }

  function abrirModalCierre() {
    if (!turnoAbierto) {
      showToast('Abre la caja antes de realizar el cierre.');
      return;
    }
    setModalCierre(true);
    setEfectivoContado('');
    setObservacionCierre('');
    setCierreResultado(null);
    cargarCaja();
  }

  function cerrarModalCierre() {
    if (cerrandoCaja) return;
    setModalCierre(false);
    setCierreResultado(null);
  }

  function prepararNuevaApertura() {
    setModalCierre(false);
    setCierreResultado(null);
    setMontoApertura('');
  }

  async function confirmarCierre(evento) {
    evento?.preventDefault();
    const contado = Number(efectivoContado);
    if (!String(efectivoContado).trim() || Number.isNaN(contado) || contado < 0) {
      showToast('Ingresa el efectivo contado en caja.');
      return;
    }
    if (cerrandoCaja) return;
    setCerrandoCaja(true);
    try {
      const estado = await cerrarCaja({
        efectivoContado: redondear(contado),
        observacion: observacionCierre.trim(),
      });
      setCaja(estado);
      setCierreResultado(estado);
    } catch (error) {
      showToast(error?.message || 'No se pudo cerrar la caja.');
      await cargarCaja();
    } finally {
      setCerrandoCaja(false);
    }
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
  const gruposCaja = Array.isArray(caja?.metodosPago) ? caja.metodosPago : [];
  const efectivoEsperado = Number(caja?.efectivoEsperado) || 0;
  const contadoNumero = Number(efectivoContado);
  const diferenciaArqueo = String(efectivoContado).trim() && !Number.isNaN(contadoNumero)
      ? redondear(contadoNumero - efectivoEsperado)
      : null;
  const claseDiferencia = diferenciaArqueo === null
      ? ''
      : diferenciaArqueo === 0 ? 'cuadra' : diferenciaArqueo < 0 ? 'faltante' : 'sobrante';
  const etiquetaDiferencia = diferenciaArqueo === null
      ? ''
      : diferenciaArqueo === 0 ? 'Cuadra' : diferenciaArqueo < 0 ? 'Faltante' : 'Sobrante';
  const periodoCaja = caja?.desde && caja?.hasta
      ? `${caja.desde} — ${caja.hasta}`
      : '';

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
            <span
                className={`pos-caja-chip${turnoAbierto ? ' abierta' : ' cerrada'}`}
                title={turnoAbierto
                    ? `Turno abierto por ${caja.nombreVendedor || 'el vendedor'} a las ${caja.fechaApertura}`
                    : 'Sin turno de caja abierto'}
            >
              {turnoAbierto
                  ? `Caja abierta · ${dinero.format(Number(caja.montoApertura) || 0)}`
                  : 'Caja cerrada'}
            </span>
            <button
                className="pos-enlace"
                type="button"
                onClick={abrirModalCierre}
                disabled={!turnoAbierto}
            >
              <Calculator size={18} aria-hidden="true" />
              Cierre de caja
            </button>
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

            <div className="pos-bloque pos-bloque-cliente">
              <span className="pos-etiqueta">Cliente</span>
              <form className="pos-cliente-busqueda" onSubmit={buscarClienteManual}>
                <div className="pos-cliente-busqueda-campo">
                  <Search size={18} aria-hidden="true" />
                  <input
                      type="text"
                      inputMode="numeric"
                      autoComplete="off"
                      maxLength={11}
                      value={documentoCliente}
                      onChange={(evento) => cambiarDocumentoCliente(evento.target.value)}
                      placeholder="DNI (8) o RUC (11)"
                      aria-label="Buscar cliente por DNI o RUC"
                  />
                </div>
                <button className="pos-cliente-buscar" type="submit" disabled={buscandoCliente}>
                  {buscandoCliente
                      ? <LoaderCircle size={16} className="pos-girando" aria-hidden="true" />
                      : <Search size={16} aria-hidden="true" />}
                  {buscandoCliente ? 'Buscando...' : 'Buscar'}
                </button>
              </form>
              <small className="pos-cliente-ayuda">
                Se autocompleta al escribir 8 (DNI) u 11 (RUC) dígitos.
              </small>

              {clienteEncontrado && (
                  <div className="pos-cliente-ficha">
                    <div className="pos-cliente-ficha-fila">
                      <div className="pos-cliente-ficha-datos">
                        <strong>{clienteEncontrado.nombre || 'Cliente'}</strong>
                        <span>
                          {clienteEncontrado.tipoDocumento} {clienteEncontrado.numeroDocumento}
                          {clienteEncontrado.origen === 'HISTORIAL'
                              && ' · según comprobantes de la tienda'}
                        </span>
                      </div>
                      <button className="pos-cliente-quitar" type="button" onClick={quitarClienteEncontrado}>
                        Quitar
                      </button>
                    </div>
                    <label className="pos-cliente-label" htmlFor="direccion-cliente">
                      Dirección fiscal (opcional)
                    </label>
                    <input
                        id="direccion-cliente"
                        className="pos-cliente-input"
                        type="text"
                        maxLength={200}
                        value={direccionCliente}
                        onChange={(evento) => setDireccionCliente(evento.target.value)}
                        placeholder="Se imprime en el comprobante"
                    />
                  </div>
              )}

              {!clienteEncontrado && estadoCliente === 'no-encontrado' && (
                  <div className="pos-cliente-nuevo">
                    <p>No está registrado en esta tienda.</p>
                    {!registroRapido ? (
                        <button
                            className="pos-cliente-registrar"
                            type="button"
                            onClick={() => {
                              setRegistroRapido(true);
                              setClienteError('');
                            }}
                        >
                          <UserPlus size={16} aria-hidden="true" />
                          Registrar cliente rápido
                        </button>
                    ) : (
                        <form className="pos-cliente-form" onSubmit={guardarClienteRapido}>
                          <label className="pos-cliente-label" htmlFor="nuevo-cliente-nombre">
                            Nombre o razón social *
                          </label>
                          <input
                              id="nuevo-cliente-nombre"
                              className="pos-cliente-input"
                              type="text"
                              maxLength={50}
                              value={nuevoCliente.nombre}
                              onChange={(evento) => setNuevoCliente((previo) => ({
                                ...previo,
                                nombre: evento.target.value,
                              }))}
                              placeholder="Ej. Comercial Andina SAC"
                          />
                          <label className="pos-cliente-label" htmlFor="nuevo-cliente-direccion">
                            Dirección (opcional)
                          </label>
                          <input
                              id="nuevo-cliente-direccion"
                              className="pos-cliente-input"
                              type="text"
                              maxLength={200}
                              value={nuevoCliente.direccion}
                              onChange={(evento) => setNuevoCliente((previo) => ({
                                ...previo,
                                direccion: evento.target.value,
                              }))}
                              placeholder="Av. Los Álamos 123"
                          />
                          <label className="pos-cliente-label" htmlFor="nuevo-cliente-telefono">
                            Teléfono (opcional)
                          </label>
                          <input
                              id="nuevo-cliente-telefono"
                              className="pos-cliente-input"
                              type="tel"
                              maxLength={15}
                              value={nuevoCliente.telefono}
                              onChange={(evento) => setNuevoCliente((previo) => ({
                                ...previo,
                                telefono: evento.target.value,
                              }))}
                              placeholder="987654321"
                          />
                          <div className="pos-cliente-acciones">
                            <button
                                className="pos-cliente-registrar"
                                type="submit"
                                disabled={registrandoCliente}
                            >
                              {registrandoCliente
                                  ? <LoaderCircle size={16} className="pos-girando" aria-hidden="true" />
                                  : <UserPlus size={16} aria-hidden="true" />}
                              {registrandoCliente ? 'Registrando...' : 'Guardar cliente'}
                            </button>
                            <button
                                className="pos-cliente-cancelar"
                                type="button"
                                onClick={() => setRegistroRapido(false)}
                                disabled={registrandoCliente}
                            >
                              Cancelar
                            </button>
                          </div>
                        </form>
                    )}
                  </div>
              )}
              {clienteError && <span className="pos-error" role="alert">{clienteError}</span>}
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
                        onChange={(e) => cambiarDniCliente(e.target.value)}
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
                        onChange={(e) => cambiarRucCliente(e.target.value)}
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
                disabled={lineas.length === 0 || procesando || pagoInsuficiente || !turnoAbierto
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

        {/* Apertura obligatoria: sin turno abierto el POS queda bloqueado. */}
        {!turnoAbierto && !modalCierre && (
            <div
                className="pos-modal-overlay pos-overlay-bloqueo"
                role="dialog"
                aria-modal="true"
                aria-labelledby="apertura-titulo"
            >
              <div className="pos-modal pos-modal-caja">
                {cajaCargando ? (
                    <>
                      <div className="pos-modal-icono neutro">
                        <LoaderCircle className="pos-girando" size={44} aria-hidden="true" />
                      </div>
                      <h2 id="apertura-titulo" className="pos-modal-titulo">Consultando caja…</h2>
                      <p className="pos-modal-mensaje">Verificando el turno de caja de la tienda.</p>
                    </>
                ) : cajaError ? (
                    <>
                      <div className="pos-modal-icono alerta">
                        <Lock size={44} aria-hidden="true" />
                      </div>
                      <h2 id="apertura-titulo" className="pos-modal-titulo">Turno de caja no disponible</h2>
                      <p className="pos-modal-mensaje">{cajaError}</p>
                      <div className="pos-modal-acciones">
                        <button className="pos-modal-btn primario" type="button" onClick={cargarCaja}>
                          <RotateCcw size={18} aria-hidden="true" />
                          Reintentar
                        </button>
                        <button className="pos-modal-btn secundario" type="button" onClick={salir}>
                          <LogOut size={18} aria-hidden="true" />
                          Salir
                        </button>
                      </div>
                    </>
                ) : (
                    <>
                      <div className="pos-modal-icono neutro">
                        <Lock size={44} aria-hidden="true" />
                      </div>
                      <h2 id="apertura-titulo" className="pos-modal-titulo">Apertura de caja</h2>
                      <p className="pos-modal-mensaje">
                        {caja?.nombreVendedor ? `${caja.nombreVendedor}, registra` : 'Registra'}
                        {' '}el fondo inicial en efectivo para empezar a vender.
                      </p>
                      <form className="pos-caja-form" onSubmit={registrarApertura}>
                        <label className="pos-caja-label" htmlFor="monto-apertura">
                          Monto de apertura (S/)
                        </label>
                        <input
                            id="monto-apertura"
                            type="number"
                            min="0"
                            step="0.01"
                            inputMode="decimal"
                            value={montoApertura}
                            onChange={(evento) => setMontoApertura(evento.target.value)}
                            placeholder="0.00"
                            autoFocus
                        />
                        <div className="pos-modal-acciones">
                          <button className="pos-modal-btn primario" type="submit" disabled={abriendoCaja}>
                            {abriendoCaja
                                ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                                : <Calculator size={18} aria-hidden="true" />}
                            {abriendoCaja ? 'Abriendo caja…' : 'Registrar apertura'}
                          </button>
                          <button className="pos-modal-btn secundario" type="button" onClick={salir}>
                            <LogOut size={18} aria-hidden="true" />
                            Salir
                          </button>
                        </div>
                      </form>
                      <p className="pos-nota">
                        Sin apertura no se pueden emitir comprobantes. El fondo puede ser S/ 0.00 si el cajón inicia vacío.
                      </p>
                    </>
                )}
              </div>
            </div>
        )}

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
                      <div>
                        <span>Monto recibido</span>
                        <strong>{dinero.format(Number(ventaExitosa.montoRecibido) || 0)}</strong>
                      </div>
                  )}
                  <div className="pos-modal-vuelto">
                    <span>Vuelto</span>
                    <strong>{dinero.format(Number(ventaExitosa.vuelto) || 0)}</strong>
                  </div>
                </div>
                <div className="pos-modal-acciones">
                  <button
                      className="pos-modal-btn primario"
                      type="button"
                      onClick={imprimirTicket}
                      disabled={Boolean(accionArchivo) || !ventaExitosa.idVenta}
                  >
                    {accionArchivo === 'ticket'
                        ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                        : <Printer size={18} aria-hidden="true" />}
                    {accionArchivo === 'ticket' ? 'Abriendo ticket...' : 'Imprimir Ticket'}
                  </button>
                  <button
                      className="pos-modal-btn secundario"
                      type="button"
                      onClick={descargarPdf}
                      disabled={Boolean(accionArchivo) || !ventaExitosa.idVenta}
                  >
                    {accionArchivo === 'pdf'
                        ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                        : <FileText size={18} aria-hidden="true" />}
                    {accionArchivo === 'pdf' ? 'Descargando PDF...' : 'Descargar PDF'}
                  </button>
                  <button
                      className="pos-modal-btn secundario"
                      type="button"
                      onClick={siguienteCliente}
                      disabled={Boolean(accionArchivo)}
                  >
                    <ShoppingCart size={18} aria-hidden="true" />
                    Nueva Venta
                  </button>
                </div>
              </div>
            </div>
        )}
        {modalCierre && (
            <div className="pos-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="cierre-titulo">
              <div className="pos-modal pos-modal-caja pos-modal-cierre">
                {cierreResultado ? (
                    <>
                      <div className="pos-modal-icono">
                        <CheckCircle2 size={48} aria-hidden="true" />
                      </div>
                      <h2 id="cierre-titulo" className="pos-modal-titulo">Caja cerrada</h2>
                      <p className="pos-modal-mensaje">El turno quedó registrado con su arqueo.</p>
                      <div className="pos-modal-detalle">
                        <div>
                          <span>Fondo de apertura</span>
                          <strong>{dinero.format(Number(cierreResultado.montoApertura) || 0)}</strong>
                        </div>
                        <div>
                          <span>Ventas del turno ({Number(cierreResultado.comprobantes) || 0})</span>
                          <strong>{dinero.format(Number(cierreResultado.totalVentas) || 0)}</strong>
                        </div>
                        <div>
                          <span>Efectivo esperado</span>
                          <strong>{dinero.format(Number(cierreResultado.montoSistema) || 0)}</strong>
                        </div>
                        <div>
                          <span>Efectivo contado</span>
                          <strong>{dinero.format(Number(cierreResultado.efectivoContado) || 0)}</strong>
                        </div>
                      </div>
                      <div
                          className={`pos-arqueo-diferencia ${
                            Number(cierreResultado.diferencia) === 0 ? 'cuadra'
                                : Number(cierreResultado.diferencia) < 0 ? 'faltante' : 'sobrante'
                          }`}
                      >
                        <span>
                          {Number(cierreResultado.diferencia) === 0 ? 'Cuadra'
                              : Number(cierreResultado.diferencia) < 0 ? 'Faltante' : 'Sobrante'}
                        </span>
                        <strong>{dinero.format(Number(cierreResultado.diferencia) || 0)}</strong>
                      </div>
                      {cierreResultado.observacion && (
                          <p className="pos-cierre-observacion">{cierreResultado.observacion}</p>
                      )}
                      <div className="pos-modal-acciones">
                        <button className="pos-modal-btn primario" type="button" onClick={prepararNuevaApertura}>
                          <Unlock size={18} aria-hidden="true" />
                          Abrir nueva caja
                        </button>
                        <button className="pos-modal-btn secundario" type="button" onClick={cerrarModalCierre}>
                          Cerrar
                        </button>
                      </div>
                    </>
                ) : (
                    <>
                      <div className="pos-modal-icono neutro">
                        <Calculator size={44} aria-hidden="true" />
                      </div>
                      <h2 id="cierre-titulo" className="pos-modal-titulo">Cierre de caja / Arqueo</h2>
                      <p className="pos-modal-mensaje">
                        {caja?.nombreVendedor || caja?.usuario || 'Vendedor'}
                        {periodoCaja ? ` · ${periodoCaja}` : ''}
                      </p>

                      <div className="pos-arqueo-grupos">
                        {gruposCaja.map((grupo) => (
                            <div className="pos-arqueo-grupo" key={grupo.grupo}>
                              <span>
                                {grupo.grupo}
                                <small>
                                  {Number(grupo.comprobantes) === 1
                                      ? '1 comprobante'
                                      : `${Number(grupo.comprobantes) || 0} comprobantes`}
                                </small>
                              </span>
                              <strong>{dinero.format(Number(grupo.total) || 0)}</strong>
                            </div>
                        ))}
                      </div>

                      <dl className="pos-arqueo-totales">
                        <div>
                          <dt>Fondo de apertura</dt>
                          <dd>{dinero.format(Number(caja?.montoApertura) || 0)}</dd>
                        </div>
                        <div>
                          <dt>Ventas del turno ({Number(caja?.comprobantes) || 0})</dt>
                          <dd>{dinero.format(Number(caja?.totalVentas) || 0)}</dd>
                        </div>
                        <div className="pos-arqueo-esperado">
                          <dt>Efectivo esperado en caja</dt>
                          <dd>{dinero.format(efectivoEsperado)}</dd>
                        </div>
                      </dl>

                      <form className="pos-caja-form" onSubmit={confirmarCierre}>
                        <label className="pos-caja-label" htmlFor="efectivo-contado">
                          Efectivo contado en caja *
                        </label>
                        <input
                            id="efectivo-contado"
                            type="number"
                            min="0"
                            step="0.01"
                            inputMode="decimal"
                            value={efectivoContado}
                            onChange={(evento) => setEfectivoContado(evento.target.value)}
                            placeholder={efectivoEsperado.toFixed(2)}
                            autoFocus
                        />
                        {diferenciaArqueo !== null && (
                            <div className={`pos-arqueo-diferencia ${claseDiferencia}`}>
                              <span>{etiquetaDiferencia}</span>
                              <strong>{dinero.format(diferenciaArqueo)}</strong>
                            </div>
                        )}
                        <label className="pos-caja-label" htmlFor="observacion-cierre">
                          Observación (opcional)
                        </label>
                        <textarea
                            id="observacion-cierre"
                            rows={3}
                            maxLength={200}
                            value={observacionCierre}
                            onChange={(evento) => setObservacionCierre(evento.target.value)}
                            placeholder="Ej. Falta cambio de un billete de S/ 10"
                        />
                        <div className="pos-modal-acciones">
                          <button className="pos-modal-btn primario" type="submit" disabled={cerrandoCaja}>
                            {cerrandoCaja
                                ? <LoaderCircle className="pos-girando" size={18} aria-hidden="true" />
                                : <Lock size={18} aria-hidden="true" />}
                            {cerrandoCaja ? 'Cerrando caja…' : 'Confirmar cierre'}
                          </button>
                          <button
                              className="pos-modal-btn secundario"
                              type="button"
                              onClick={cerrarModalCierre}
                              disabled={cerrandoCaja}
                          >
                            Cancelar
                          </button>
                        </div>
                      </form>
                    </>
                )}
              </div>
            </div>
        )}
      </div>
  );
}