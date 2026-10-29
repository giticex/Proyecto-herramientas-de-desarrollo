import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Banknote,
  Building2,
  Check,
  ChevronLeft,
  ChevronRight,
  CreditCard,
  Eye,
  EyeOff,
  FileCode2,
  FileText,
  KeyRound,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Package,
  Pencil,
  Power,
  PowerOff,
  RefreshCw,
  Save,
  ScrollText,
  Search,
  ShieldCheck,
  ShoppingBag,
  Trash2,
  TrendingUp,
  TriangleAlert,
  UserCog,
  UserPlus,
  Users,
} from 'lucide-react';
import {
  actualizarEstadoEmpresa,
  actualizarPlanEmpresa,
  actualizarUsuarioEmpresa,
  cambiarEstadoUsuarioEmpresa,
  crearUsuarioEmpresa,
  eliminarUsuarioEmpresa,
  listarAuditoria,
  listarComprobantesEmpresa,
  listarEmpresas,
  listarProductosEmpresa,
  listarUsuariosEmpresa,
  obtenerAdminEmpresa,
  obtenerDashboardEmpresa,
  obtenerPlanEmpresa,
  obtenerResumenGlobal,
  restablecerPasswordAdminEmpresa,
} from '../api/superAdminApi';
import { rutaComprobantePdf, rutaComprobanteXml } from '../api/ventasApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import { useSesion } from '../auth/sesionStore';
import './SuperAdminPanel.css';

const TAMANIO_COMPROBANTES = 8;
const TAMANIO_AUDITORIA = 10;

const ETIQUETAS_AUDITORIA = {
  TIENDA_CREADA: 'Alta de tienda',
  TIENDA_SUSPENDIDA: 'Suspensión de tienda',
  TIENDA_ACTIVADA: 'Reactivación de tienda',
  PLAN_ACTUALIZADO: 'Cambio de plan',
  ADMIN_PASSWORD_RESTABLECIDO: 'Restablecimiento de contraseña',
  USUARIO_CREADO: 'Alta de usuario',
  USUARIO_ACTUALIZADO: 'Edición de usuario',
  USUARIO_SUSPENDIDO: 'Suspensión de usuario',
  USUARIO_ACTIVADO: 'Reactivación de usuario',
  USUARIO_ELIMINADO: 'Baja de usuario',
  USUARIO_PASSWORD_RESTABLECIDO: 'Cambio de clave de personal',
};

const FILTROS_AUDITORIA = [
  { valor: '', etiqueta: 'Todos los eventos' },
  { valor: 'TIENDA_CREADA', etiqueta: 'Altas de tienda' },
  { valor: 'TIENDA_SUSPENDIDA', etiqueta: 'Suspensiones' },
  { valor: 'TIENDA_ACTIVADA', etiqueta: 'Reactivaciones' },
  { valor: 'PLAN_ACTUALIZADO', etiqueta: 'Cambios de plan' },
  { valor: 'ADMIN_PASSWORD_RESTABLECIDO', etiqueta: 'Restablecimientos de contraseña' },
  { valor: 'USUARIO_CREADO', etiqueta: 'Altas de usuario' },
  { valor: 'USUARIO_ACTUALIZADO', etiqueta: 'Ediciones de usuario' },
  { valor: 'USUARIO_SUSPENDIDO', etiqueta: 'Suspensiones de usuario' },
  { valor: 'USUARIO_ACTIVADO', etiqueta: 'Reactivaciones de usuario' },
  { valor: 'USUARIO_ELIMINADO', etiqueta: 'Bajas de usuario' },
  { valor: 'USUARIO_PASSWORD_RESTABLECIDO', etiqueta: 'Claves de personal' },
];

const ROLES_STAFF = [
  { valor: 'ADMIN', etiqueta: 'Administrador' },
  { valor: 'VENDEDOR', etiqueta: 'Vendedor' },
];

const FORM_STAFF_VACIO = {
  idUsuario: null,
  usuario: '',
  nombre: '',
  apellido: '',
  correo: '',
  rol: 'VENDEDOR',
  password: '',
  confirmacion: '',
};

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

function etiquetaEvento(tipo) {
  return ETIQUETAS_AUDITORIA[tipo] || tipo || 'Evento';
}

function porcentajeUso(usado, limite) {
  const total = Number(limite) || 0;
  if (total <= 0) return 0;
  return Math.min(100, Math.round(((Number(usado) || 0) / total) * 100));
}

function iniciales(...valores) {
  const base = valores.find((valor) => typeof valor === 'string' && valor.trim()) || '?';
  return base
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join('');
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
  { id: 'plan', etiqueta: 'Plan', icono: CreditCard },
  { id: 'admin', etiqueta: 'Administrador', icono: UserCog },
  { id: 'comprobantes', etiqueta: 'Comprobantes', icono: FileText },
];

const VISTAS = [
  { id: 'empresas', etiqueta: 'Empresas', icono: Building2 },
  { id: 'auditoria', etiqueta: 'Auditoría del Sistema', icono: ScrollText },
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

  const [vista, setVista] = useState('empresas');
  const [confirmacion, setConfirmacion] = useState(null);
  const [cambiandoEstado, setCambiandoEstado] = useState(null);

  const [plan, setPlan] = useState(null);
  const [planForm, setPlanForm] = useState({
    plan: 'BASICO',
    limiteProductos: 100,
    limiteUsuarios: 5,
  });
  const [cargandoPlan, setCargandoPlan] = useState(false);
  const [errorPlan, setErrorPlan] = useState('');
  const [guardandoPlan, setGuardandoPlan] = useState(false);

  const [admin, setAdmin] = useState(null);
  const [cargandoAdmin, setCargandoAdmin] = useState(false);
  const [errorAdmin, setErrorAdmin] = useState('');
  const [claveForm, setClaveForm] = useState({ password: '', confirmacion: '' });
  const [mostrarClave, setMostrarClave] = useState(false);
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false);
  const [errorClave, setErrorClave] = useState('');
  const [guardandoClave, setGuardandoClave] = useState(false);
  const [confirmacionClave, setConfirmacionClave] = useState(null);

  const [usuarios, setUsuarios] = useState([]);
  const [cargandoUsuarios, setCargandoUsuarios] = useState(false);
  const [errorUsuarios, setErrorUsuarios] = useState('');
  const [formStaff, setFormStaff] = useState(null);
  const [mostrarClaveStaff, setMostrarClaveStaff] = useState(false);
  const [errorStaff, setErrorStaff] = useState('');
  const [guardandoStaff, setGuardandoStaff] = useState(false);
  const [confirmacionStaff, setConfirmacionStaff] = useState(null);
  const [cambiandoStaff, setCambiandoStaff] = useState(null);

  const [auditoria, setAuditoria] = useState({
    contenido: [],
    totalPaginas: 0,
    totalElementos: 0,
  });
  const [cargandoAuditoria, setCargandoAuditoria] = useState(false);
  const [errorAuditoria, setErrorAuditoria] = useState('');
  const [tipoAuditoria, setTipoAuditoria] = useState('');
  const [textoAuditoria, setTextoAuditoria] = useState('');
  const [busquedaAuditoria, setBusquedaAuditoria] = useState('');
  const [paginaAuditoria, setPaginaAuditoria] = useState(0);

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

  useEffect(() => {
    if (pestania !== 'plan' || !seleccionada) return undefined;
    let vigente = true;
    setCargandoPlan(true);
    setErrorPlan('');
    obtenerPlanEmpresa(seleccionada)
      .then((datos) => {
        if (!vigente) return;
        setPlan(datos || null);
        if (datos) {
          setPlanForm({
            plan: datos.plan,
            limiteProductos: datos.limiteProductos,
            limiteUsuarios: datos.limiteUsuarios,
          });
        }
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorPlan(mensajeDe(problema, 'No se pudo cargar el plan de la empresa.'));
        setPlan(null);
      })
      .finally(() => {
        if (vigente) setCargandoPlan(false);
      });
    return () => { vigente = false; };
  }, [pestania, seleccionada, refresco]);

  useEffect(() => {
    if (pestania !== 'admin' || !seleccionada) return undefined;
    let vigente = true;
    setCargandoAdmin(true);
    setErrorAdmin('');
    obtenerAdminEmpresa(seleccionada)
      .then((datos) => {
        if (!vigente) return;
        setAdmin(datos || null);
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorAdmin(mensajeDe(problema, 'No se pudo cargar el administrador de la empresa.'));
        setAdmin(null);
      })
      .finally(() => {
        if (vigente) setCargandoAdmin(false);
      });
    return () => { vigente = false; };
  }, [pestania, seleccionada, refresco]);

  useEffect(() => {
    if (pestania !== 'admin' || !seleccionada) return undefined;
    let vigente = true;
    setCargandoUsuarios(true);
    setErrorUsuarios('');
    listarUsuariosEmpresa(seleccionada)
      .then((datos) => {
        if (!vigente) return;
        setUsuarios(Array.isArray(datos) ? datos : []);
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorUsuarios(mensajeDe(problema, 'No se pudo cargar el personal de la empresa.'));
        setUsuarios([]);
      })
      .finally(() => {
        if (vigente) setCargandoUsuarios(false);
      });
    return () => { vigente = false; };
  }, [pestania, seleccionada, refresco]);

  useEffect(() => {
    const temporizador = setTimeout(() => {
      setBusquedaAuditoria(textoAuditoria.trim());
      setPaginaAuditoria(0);
    }, 350);
    return () => clearTimeout(temporizador);
  }, [textoAuditoria]);

  useEffect(() => {
    if (vista !== 'auditoria') return undefined;
    let vigente = true;
    setCargandoAuditoria(true);
    setErrorAuditoria('');
    listarAuditoria({
      pagina: paginaAuditoria,
      tamanio: TAMANIO_AUDITORIA,
      tipo: tipoAuditoria,
      busqueda: busquedaAuditoria,
    })
      .then((datos) => {
        if (!vigente) return;
        setAuditoria({
          contenido: Array.isArray(datos?.contenido) ? datos.contenido : [],
          totalPaginas: Number(datos?.totalPaginas) || 0,
          totalElementos: Number(datos?.totalElementos) || 0,
        });
      })
      .catch((problema) => {
        if (!vigente) return;
        setErrorAuditoria(mensajeDe(problema, 'No se pudo cargar la auditoría del sistema.'));
        setAuditoria({ contenido: [], totalPaginas: 0, totalElementos: 0 });
      })
      .finally(() => {
        if (vigente) setCargandoAuditoria(false);
      });
    return () => { vigente = false; };
  }, [vista, paginaAuditoria, tipoAuditoria, busquedaAuditoria, refresco]);

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
    setPlan(null);
    setErrorPlan('');
    setAdmin(null);
    setErrorAdmin('');
    setClaveForm({ password: '', confirmacion: '' });
    setErrorClave('');
    setMostrarClave(false);
    setMostrarConfirmacion(false);
    setUsuarios([]);
    setErrorUsuarios('');
    setFormStaff(null);
    setErrorStaff('');
    setMostrarClaveStaff(false);
    setConfirmacionStaff(null);
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

  function pedirCambioEstado(empresaObjetivo) {
    if (!empresaObjetivo) return;
    setConfirmacion({
      idTienda: empresaObjetivo.idTienda,
      nombre: empresaObjetivo.nombre,
      estadoDestino: empresaObjetivo.estadoAcceso === 'SUSPENDIDA' ? 'ACTIVA' : 'SUSPENDIDA',
    });
  }

  async function confirmarCambioEstado() {
    if (!confirmacion) return;
    const { idTienda, estadoDestino } = confirmacion;
    setCambiandoEstado(idTienda);
    try {
      const actualizada = await actualizarEstadoEmpresa(idTienda, estadoDestino);
      setEmpresas((lista) => lista.map((item) => (
        item.idTienda === idTienda ? { ...item, ...actualizada } : item
      )));
      setDetalle((actual) => (
        actual?.empresa?.idTienda === idTienda
          ? { ...actual, empresa: { ...actual.empresa, ...actualizada } }
          : actual
      ));
      setPlan((actual) => (
        actual?.idTienda === idTienda
          ? { ...actual, estadoAcceso: actualizada.estadoAcceso }
          : actual
      ));
      setAdmin((actual) => (
        actual?.idTienda === idTienda
          ? {
            ...actual,
            estadoAcceso: actualizada.estadoAcceso,
            estadoRol: actualizada.estadoAcceso === 'SUSPENDIDA' ? 'SUSPENDIDO' : 'ACTIVO',
          }
          : actual
      ));
      showToast(
        estadoDestino === 'SUSPENDIDA'
          ? `${actualizada.nombre} quedó suspendida.`
          : `${actualizada.nombre} volvió a estar activa.`,
      );
      setConfirmacion(null);
    } catch (problema) {
      showToast(problema?.message || 'No se pudo cambiar el estado de la empresa.');
    } finally {
      setCambiandoEstado(null);
    }
  }

  function elegirPlan(codigo) {
    const opcion = (plan?.planes || []).find((item) => item.codigo === codigo);
    setPlanForm((actual) => ({
      plan: codigo,
      limiteProductos: opcion?.limiteProductos ?? actual.limiteProductos,
      limiteUsuarios: opcion?.limiteUsuarios ?? actual.limiteUsuarios,
    }));
  }

  async function guardarPlan(evento) {
    evento.preventDefault();
    if (!seleccionada) return;
    const productos = Number(planForm.limiteProductos);
    const usuarios = Number(planForm.limiteUsuarios);
    if (!Number.isInteger(productos) || productos < 1 || !Number.isInteger(usuarios) || usuarios < 1) {
      setErrorPlan('Los límites deben ser números enteros mayores que cero.');
      return;
    }
    setGuardandoPlan(true);
    setErrorPlan('');
    try {
      const datos = await actualizarPlanEmpresa(seleccionada, {
        plan: planForm.plan,
        limiteProductos: productos,
        limiteUsuarios: usuarios,
      });
      setPlan(datos);
      setPlanForm({
        plan: datos.plan,
        limiteProductos: datos.limiteProductos,
        limiteUsuarios: datos.limiteUsuarios,
      });
      setEmpresas((lista) => lista.map((item) => (
        item.idTienda === seleccionada
          ? {
            ...item,
            plan: datos.plan,
            limiteProductos: datos.limiteProductos,
            limiteUsuarios: datos.limiteUsuarios,
          }
          : item
      )));
      showToast('Plan y límites actualizados.');
    } catch (problema) {
      setErrorPlan(mensajeDe(problema, 'No se pudo actualizar el plan de la empresa.'));
    } finally {
      setGuardandoPlan(false);
    }
  }

  function pedirRestablecerClave(evento) {
    evento.preventDefault();
    if (!admin) return;
    const clave = claveForm.password;
    if (!clave.trim()) {
      setErrorClave('La nueva contraseña es obligatoria.');
      return;
    }
    if (clave.length < 8) {
      setErrorClave('La nueva contraseña debe tener al menos 8 caracteres.');
      return;
    }
    if (clave.length > 72) {
      setErrorClave('La nueva contraseña no puede superar los 72 caracteres.');
      return;
    }
    if (clave !== claveForm.confirmacion) {
      setErrorClave('La confirmación no coincide con la nueva contraseña.');
      return;
    }
    setErrorClave('');
    setConfirmacionClave({
      usuario: admin.usuario,
      nombre: admin.nombreCompleto || admin.usuario,
    });
  }

  async function confirmarRestablecerClave() {
    if (!confirmacionClave || !seleccionada) return;
    setGuardandoClave(true);
    setErrorClave('');
    try {
      const datos = await restablecerPasswordAdminEmpresa(seleccionada, claveForm.password);
      setAdmin(datos);
      setClaveForm({ password: '', confirmacion: '' });
      setMostrarClave(false);
      setMostrarConfirmacion(false);
      setConfirmacionClave(null);
      showToast(`Contraseña de ${datos.usuario} restablecida.`);
    } catch (problema) {
      setErrorClave(mensajeDe(problema, 'No se pudo restablecer la contraseña.'));
      setConfirmacionClave(null);
    } finally {
      setGuardandoClave(false);
    }
  }

  function abrirNuevoUsuario() {
    setFormStaff({ ...FORM_STAFF_VACIO });
    setErrorStaff('');
    setMostrarClaveStaff(false);
  }

  function abrirEditarUsuario(cuenta) {
    setFormStaff({
      idUsuario: cuenta.idUsuario,
      usuario: cuenta.usuario || '',
      nombre: cuenta.nombre || '',
      apellido: cuenta.apellido || '',
      correo: cuenta.correo || '',
      rol: cuenta.rol || 'VENDEDOR',
      password: '',
      confirmacion: '',
    });
    setErrorStaff('');
    setMostrarClaveStaff(false);
  }

  function cerrarFormStaff() {
    if (guardandoStaff) return;
    setFormStaff(null);
    setErrorStaff('');
  }

  async function guardarStaff(evento) {
    evento.preventDefault();
    if (!formStaff || !seleccionada) return;
    const esAlta = !formStaff.idUsuario;
    const usuario = formStaff.usuario.trim();
    const nombre = formStaff.nombre.trim();
    const correo = formStaff.correo.trim();
    const clave = formStaff.password;

    if (usuario.length < 3) {
      setErrorStaff('El nombre de usuario debe tener al menos 3 caracteres.');
      return;
    }
    if (!nombre) {
      setErrorStaff('El nombre es obligatorio.');
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo)) {
      setErrorStaff('El correo electrónico no tiene un formato válido.');
      return;
    }
    if (esAlta && !clave) {
      setErrorStaff('La contraseña es obligatoria para crear la cuenta.');
      return;
    }
    if (clave) {
      if (clave.length < 8) {
        setErrorStaff('La contraseña debe tener al menos 8 caracteres.');
        return;
      }
      if (clave.length > 72) {
        setErrorStaff('La contraseña no puede superar los 72 caracteres.');
        return;
      }
      if (clave !== formStaff.confirmacion) {
        setErrorStaff('La confirmación no coincide con la contraseña.');
        return;
      }
    }

    const datos = {
      usuario,
      nombre,
      apellido: formStaff.apellido.trim(),
      correo,
      rol: formStaff.rol,
    };
    if (clave) datos.password = clave;

    setGuardandoStaff(true);
    setErrorStaff('');
    try {
      if (esAlta) {
        await crearUsuarioEmpresa(seleccionada, datos);
        showToast(`Cuenta ${usuario} creada.`);
      } else {
        await actualizarUsuarioEmpresa(seleccionada, formStaff.idUsuario, datos);
        showToast(`Cuenta ${usuario} actualizada.`);
      }
      setFormStaff(null);
      setMostrarClaveStaff(false);
      setRefresco((valor) => valor + 1);
    } catch (problema) {
      setErrorStaff(mensajeDe(problema, 'No se pudo guardar la cuenta.'));
    } finally {
      setGuardandoStaff(false);
    }
  }

  function pedirCambioEstadoUsuario(cuenta) {
    if (!cuenta) return;
    setConfirmacionStaff({
      tipo: 'estado',
      cuenta,
      destino: cuenta.estado === 'SUSPENDIDO' ? 'ACTIVO' : 'SUSPENDIDO',
    });
  }

  function pedirEliminarUsuario(cuenta) {
    if (!cuenta) return;
    setConfirmacionStaff({ tipo: 'eliminar', cuenta });
  }

  async function confirmarAccionStaff() {
    if (!confirmacionStaff || !seleccionada) return;
    const { tipo, cuenta, destino } = confirmacionStaff;
    setCambiandoStaff(cuenta.idUsuario);
    try {
      if (tipo === 'eliminar') {
        await eliminarUsuarioEmpresa(seleccionada, cuenta.idUsuario);
        showToast(`Cuenta ${cuenta.usuario} eliminada.`);
      } else {
        await cambiarEstadoUsuarioEmpresa(seleccionada, cuenta.idUsuario, destino);
        showToast(destino === 'SUSPENDIDO'
          ? `Acceso de ${cuenta.usuario} suspendido.`
          : `Acceso de ${cuenta.usuario} reactivado.`);
      }
      setConfirmacionStaff(null);
      setRefresco((valor) => valor + 1);
    } catch (problema) {
      showToast(problema?.message || 'No se pudo completar la acción.');
      setConfirmacionStaff(null);
    } finally {
      setCambiandoStaff(null);
    }
  }

  const primeraFila = comprobantes.totalElementos === 0
    ? 0
    : pagina * TAMANIO_COMPROBANTES + 1;  const ultimaFila = Math.min(
    (pagina + 1) * TAMANIO_COMPROBANTES,
    comprobantes.totalElementos,
  );

  const empresaSuspendida = empresa?.estadoAcceso === 'SUSPENDIDA';
  const usoProductos = porcentajeUso(plan?.totalProductos, planForm.limiteProductos);
  const usoUsuarios = porcentajeUso(plan?.totalUsuarios, planForm.limiteUsuarios);
  const primeraAuditoria = auditoria.totalElementos === 0
    ? 0
    : paginaAuditoria * TAMANIO_AUDITORIA + 1;
  const ultimaAuditoria = Math.min(
    (paginaAuditoria + 1) * TAMANIO_AUDITORIA,
    auditoria.totalElementos,
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
            disabled={vista === 'empresas' ? cargandoGlobal : cargandoAuditoria}
          >
            <RefreshCw
              size={16}
              aria-hidden="true"
              className={(vista === 'empresas' ? cargandoGlobal : cargandoAuditoria) ? 'girando' : ''}
            />
            Actualizar
          </button>
        </header>

        <nav className="sa-vistas" aria-label="Vistas de la consola">
          {VISTAS.map(({ id, etiqueta, icono: Icono }) => (
            <button
              key={id}
              className={`sa-vista${vista === id ? ' activa' : ''}`}
              type="button"
              aria-current={vista === id ? 'page' : undefined}
              onClick={() => setVista(id)}
            >
              <Icono size={16} aria-hidden="true" />
              {etiqueta}
            </button>
          ))}
        </nav>

        {vista === 'empresas' && (
          <>
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
                const suspendida = item.estadoAcceso === 'SUSPENDIDA';
                return (
                  <li key={item.idTienda} className="sa-empresa-fila">
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
                      <span className="sa-empresa-badges">
                        <span className={`sa-estado ${suspendida ? 'acceso-suspendida' : 'acceso-activa'}`}>
                          {suspendida ? 'Suspendida' : 'Activa'}
                        </span>
                        <span className={`sa-estado ${claseEstado(item.estado)}`}>{item.estado}</span>
                      </span>
                    </button>
                    <button
                      className={`sa-accion-estado ${suspendida ? 'reactivar' : 'suspender'}`}
                      type="button"
                      title={`${suspendida ? 'Reactivar' : 'Suspender'} acceso de ${item.nombre}`}
                      aria-label={`${suspendida ? 'Reactivar' : 'Suspender'} acceso de ${item.nombre}`}
                      disabled={cambiandoEstado === item.idTienda}
                      onClick={() => pedirCambioEstado(item)}
                    >
                      {cambiandoEstado === item.idTienda
                        ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                        : suspendida
                          ? <Power size={16} aria-hidden="true" />
                          : <PowerOff size={16} aria-hidden="true" />}
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
                      <span className={`sa-estado ${empresaSuspendida ? 'acceso-suspendida' : 'acceso-activa'}`}>
                        {empresaSuspendida ? 'Suspendida' : 'Activa'}
                      </span>
                      <span className={`sa-estado ${claseEstado(empresa.estado)}`}>{empresa.estado}</span>
                      <span className="sa-detalle-dato">
                        Última venta: <strong>{formatearFecha(empresa.ultimaVenta)}</strong>
                      </span>
                      <button
                        className={`sa-btn ${empresaSuspendida ? 'reactivar' : 'peligro'}`}
                        type="button"
                        onClick={() => pedirCambioEstado(empresa)}
                        disabled={cambiandoEstado === empresa.idTienda}
                      >
                        {empresaSuspendida
                          ? <Power size={15} aria-hidden="true" />
                          : <PowerOff size={15} aria-hidden="true" />}
                        {empresaSuspendida ? 'Reactivar acceso' : 'Suspender acceso'}
                      </button>
                    </div>
                  )}
                </header>

                {empresaSuspendida && (
                  <div className="sa-aviso" role="status">
                    <TriangleAlert size={16} aria-hidden="true" />
                    <p>
                      El acceso de esta empresa está suspendido: sus usuarios no pueden iniciar
                      sesión ni operar la tienda mientras no se reactive.
                    </p>
                  </div>
                )}

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

                {pestania === 'plan' && (
                  <div
                    className="sa-tab-panel"
                    id="sa-panel-plan"
                    role="tabpanel"
                    aria-labelledby="sa-tab-plan"
                  >
                    {errorPlan && (
                      <div className="sa-error compacto" role="alert">
                        <TriangleAlert size={16} aria-hidden="true" />
                        <p>{errorPlan}</p>
                      </div>
                    )}

                    {cargandoPlan && !plan && (
                      <p className="sa-cargando">
                        <LoaderCircle size={16} className="girando" aria-hidden="true" />
                        Cargando plan…
                      </p>
                    )}

                    {plan && (
                      <form className="sa-plan" onSubmit={guardarPlan}>
                        <div className="sa-plan-encabezado">
                          <div>
                            <h3 className="sa-subtitulo">Plan contratado</h3>
                            <p className="sa-panel-sub">
                              Define el nivel de servicio y los límites operativos de la empresa.
                            </p>
                          </div>
                          <span className="sa-contador">{plan.plan}</span>
                        </div>

                        <div className="sa-plan-planes" role="radiogroup" aria-label="Plan contratado">
                          {(plan.planes || []).map((opcion) => {
                            const elegido = planForm.plan === opcion.codigo;
                            return (
                              <button
                                key={opcion.codigo}
                                className={`sa-plan-opcion${elegido ? ' activa' : ''}`}
                                type="button"
                                role="radio"
                                aria-checked={elegido}
                                onClick={() => elegirPlan(opcion.codigo)}
                              >
                                <span className="sa-plan-opcion-cabecera">
                                  <strong>{opcion.nombre}</strong>
                                  {elegido && <Check size={16} aria-hidden="true" />}
                                </span>
                                <span className="sa-plan-opcion-detalle">
                                  {entero.format(opcion.limiteProductos)} productos
                                </span>
                                <span className="sa-plan-opcion-detalle">
                                  {entero.format(opcion.limiteUsuarios)} de personal
                                </span>
                              </button>
                            );
                          })}
                        </div>

                        <div className="sa-consumo">
                          <div className="sa-consumo-fila">
                            <div className="sa-consumo-cabecera">
                              <span><Package size={14} aria-hidden="true" /> Productos en uso</span>
                              <strong>
                                {entero.format(plan.totalProductos)} de {entero.format(planForm.limiteProductos)}
                              </strong>
                            </div>
                            <span className="sa-consumo-barra" aria-hidden="true">
                              <span
                                className={usoProductos >= 100 ? 'tope' : ''}
                                style={{ width: `${usoProductos}%` }}
                              />
                            </span>
                          </div>
                          <div className="sa-consumo-fila">
                            <div className="sa-consumo-cabecera">
                              <span><Users size={14} aria-hidden="true" /> Personal de la tienda</span>
                              <strong>
                                {entero.format(plan.totalUsuarios)} de {entero.format(planForm.limiteUsuarios)}
                              </strong>
                            </div>
                            <span className="sa-consumo-barra" aria-hidden="true">
                              <span
                                className={usoUsuarios >= 100 ? 'tope' : ''}
                                style={{ width: `${usoUsuarios}%` }}
                              />
                            </span>
                          </div>
                        </div>

                        <div className="sa-plan-limites">
                          <label className="sa-campo">
                            <span>Límite de productos</span>
                            <input
                              type="number"
                              min="1"
                              step="1"
                              value={planForm.limiteProductos}
                              onChange={(evento) => setPlanForm((actual) => ({
                                ...actual,
                                limiteProductos: evento.target.value,
                              }))}
                              disabled={guardandoPlan}
                            />
                          </label>
                          <label className="sa-campo">
                            <span>Límite de personal</span>
                            <input
                              type="number"
                              min="1"
                              step="1"
                              value={planForm.limiteUsuarios}
                              onChange={(evento) => setPlanForm((actual) => ({
                                ...actual,
                                limiteUsuarios: evento.target.value,
                              }))}
                              disabled={guardandoPlan}
                            />
                          </label>
                        </div>

                        <div className="sa-plan-pie">
                          <p>Los límites bloquean nuevas altas; no eliminan productos ni cuentas existentes.</p>
                          <button className="sa-btn actualizar" type="submit" disabled={guardandoPlan}>
                            {guardandoPlan
                              ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                              : <Save size={16} aria-hidden="true" />}
                            Guardar plan
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}

                {pestania === 'admin' && (
                  <div
                    className="sa-tab-panel"
                    id="sa-panel-admin"
                    role="tabpanel"
                    aria-labelledby="sa-tab-admin"
                  >
                    {errorAdmin && (
                      <div className="sa-error compacto" role="alert">
                        <TriangleAlert size={16} aria-hidden="true" />
                        <p>{errorAdmin}</p>
                        <button className="sa-btn" type="button" onClick={() => setRefresco((v) => v + 1)}>
                          Reintentar
                        </button>
                      </div>
                    )}

                    {cargandoAdmin && !admin && (
                      <p className="sa-cargando">
                        <LoaderCircle size={16} className="girando" aria-hidden="true" />
                        Cargando administrador…
                      </p>
                    )}

                    {admin && (
                      <div className="sa-admin">
                        <section className="sa-admin-ficha" aria-label="Administrador actual de la empresa">
                          <span className="sa-admin-avatar" aria-hidden="true">
                            {iniciales(admin.nombreCompleto, admin.usuario)}
                          </span>
                          <div className="sa-admin-datos">
                            <div className="sa-admin-cabecera">
                              <div>
                                <h3 className="sa-subtitulo">{admin.nombreCompleto || admin.usuario}</h3>
                                <p className="sa-panel-sub">
                                  Cuenta que administra {admin.tiendaNombre}.
                                </p>
                              </div>
                              <div className="sa-admin-badges">
                                <span className="sa-estado neutro">{admin.rol}</span>
                                <span
                                  className={`sa-estado ${admin.estadoRol === 'ACTIVO' ? 'acceso-activa' : 'acceso-suspendida'}`}
                                >
                                  {admin.estadoRol === 'ACTIVO' ? 'Rol activo' : 'Acceso suspendido'}
                                </span>
                              </div>
                            </div>
                            <dl className="sa-admin-lista">
                              <div>
                                <dt>Usuario</dt>
                                <dd>{admin.usuario}</dd>
                              </div>
                              <div>
                                <dt>Correo electrónico</dt>
                                <dd>{admin.correo || 'Sin correo registrado'}</dd>
                              </div>
                              <div>
                                <dt>Rol</dt>
                                <dd>{admin.rol}</dd>
                              </div>
                              <div>
                                <dt>Estado del rol</dt>
                                <dd>{admin.estadoRol === 'ACTIVO' ? 'Activo' : 'Suspendido por la tienda'}</dd>
                              </div>
                            </dl>
                            <p className="sa-admin-proteccion">
                              <ShieldCheck size={14} aria-hidden="true" />
                              Contraseña protegida con {admin.formatoContrasena === 'SHA-256'
                                ? 'el hash heredado SHA-256; se migrará a BCrypt en el próximo inicio de sesión'
                                : 'BCrypt'}.
                            </p>
                          </div>
                        </section>

                        <form className="sa-clave" onSubmit={pedirRestablecerClave}>
                          <div className="sa-plan-encabezado">
                            <div>
                              <h3 className="sa-subtitulo">Credenciales de acceso</h3>
                              <p className="sa-panel-sub">
                                Restablece la contraseña del administrador de forma segura.
                              </p>
                            </div>
                            <KeyRound size={18} aria-hidden="true" />
                          </div>

                          {errorClave && (
                            <div className="sa-error compacto" role="alert">
                              <TriangleAlert size={16} aria-hidden="true" />
                              <p>{errorClave}</p>
                            </div>
                          )}

                          <div className="sa-clave-grid">
                            <label className="sa-campo">
                              <span>Nueva contraseña</span>
                              <div className="sa-clave-entrada">
                                <input
                                  type={mostrarClave ? 'text' : 'password'}
                                  value={claveForm.password}
                                  onChange={(evento) => setClaveForm((actual) => ({
                                    ...actual,
                                    password: evento.target.value,
                                  }))}
                                  autoComplete="new-password"
                                  minLength={8}
                                  maxLength={72}
                                  required
                                  disabled={guardandoClave}
                                />
                                <button
                                  className="sa-clave-ojo"
                                  type="button"
                                  onClick={() => setMostrarClave((valor) => !valor)}
                                  aria-label={mostrarClave ? 'Ocultar nueva contraseña' : 'Mostrar nueva contraseña'}
                                  title={mostrarClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                >
                                  {mostrarClave
                                    ? <EyeOff size={16} aria-hidden="true" />
                                    : <Eye size={16} aria-hidden="true" />}
                                </button>
                              </div>
                            </label>

                            <label className="sa-campo">
                              <span>Confirmar contraseña</span>
                              <div className="sa-clave-entrada">
                                <input
                                  type={mostrarConfirmacion ? 'text' : 'password'}
                                  value={claveForm.confirmacion}
                                  onChange={(evento) => setClaveForm((actual) => ({
                                    ...actual,
                                    confirmacion: evento.target.value,
                                  }))}
                                  autoComplete="new-password"
                                  minLength={8}
                                  maxLength={72}
                                  required
                                  disabled={guardandoClave}
                                />
                                <button
                                  className="sa-clave-ojo"
                                  type="button"
                                  onClick={() => setMostrarConfirmacion((valor) => !valor)}
                                  aria-label={mostrarConfirmacion ? 'Ocultar confirmación' : 'Mostrar confirmación'}
                                  title={mostrarConfirmacion ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                >
                                  {mostrarConfirmacion
                                    ? <EyeOff size={16} aria-hidden="true" />
                                    : <Eye size={16} aria-hidden="true" />}
                                </button>
                              </div>
                            </label>
                          </div>

                          <div className="sa-clave-pie">
                            <p>
                              Mínimo 8 caracteres. La contraseña actual dejará de funcionar y la
                              acción quedará registrada en la auditoría del sistema.
                            </p>
                            <button className="sa-btn actualizar" type="submit" disabled={guardandoClave}>
                              {guardandoClave
                                ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                                : <KeyRound size={16} aria-hidden="true" />}
                              Restablecer contraseña
                            </button>
                          </div>
                        </form>
                      </div>
                    )}

                    <section className="sa-personal" aria-label="Personal de la tienda">
                      <header className="sa-personal-cabecera">
                        <div>
                          <h3 className="sa-subtitulo">Personal de la tienda</h3>
                          <p className="sa-panel-sub">
                            Cuentas ADMIN y VENDEDOR vinculadas a {empresa?.nombre || 'esta empresa'}.
                            {empresa?.limiteUsuarios
                              ? ` El plan permite hasta ${entero.format(empresa.limiteUsuarios)} cuentas de personal.`
                              : ''}
                          </p>
                        </div>
                        <button
                          className="sa-btn actualizar"
                          type="button"
                          onClick={abrirNuevoUsuario}
                          disabled={!seleccionada || cargandoUsuarios}
                        >
                          <UserPlus size={16} aria-hidden="true" />
                          Nuevo usuario
                        </button>
                      </header>

                      {errorUsuarios && (
                        <div className="sa-error compacto" role="alert">
                          <TriangleAlert size={16} aria-hidden="true" />
                          <p>{errorUsuarios}</p>
                          <button className="sa-btn" type="button" onClick={() => setRefresco((v) => v + 1)}>
                            Reintentar
                          </button>
                        </div>
                      )}

                      {cargandoUsuarios && usuarios.length === 0 && (
                        <p className="sa-cargando">
                          <LoaderCircle size={16} className="girando" aria-hidden="true" />
                          Cargando personal…
                        </p>
                      )}

                      {!errorUsuarios && !cargandoUsuarios && usuarios.length === 0 && (
                        <p className="sa-vacio">Esta empresa todavía no tiene cuentas de personal.</p>
                      )}

                      {usuarios.length > 0 && (
                        <div className="sa-tabla-envoltura">
                          <table className="sa-tabla sa-tabla-personal">
                            <thead>
                              <tr>
                                <th scope="col">Nombre</th>
                                <th scope="col">Correo</th>
                                <th scope="col">Rol</th>
                                <th scope="col">Estado</th>
                                <th scope="col" className="acciones">Acciones</th>
                              </tr>
                            </thead>
                            <tbody>
                              {usuarios.map((cuenta) => {
                                const suspendida = cuenta.estado === 'SUSPENDIDO';
                                const ocupada = cambiandoStaff === cuenta.idUsuario;
                                return (
                                  <tr key={cuenta.idUsuario}>
                                    <td>
                                      <div className="sa-usuario">
                                        <span className="sa-usuario-avatar" aria-hidden="true">
                                          {iniciales(cuenta.nombreCompleto, cuenta.usuario)}
                                        </span>
                                        <div>
                                          <strong>{cuenta.nombreCompleto || cuenta.usuario}</strong>
                                          <small>@{cuenta.usuario}</small>
                                        </div>
                                      </div>
                                    </td>
                                    <td>{cuenta.correo || '—'}</td>
                                    <td>
                                      <span className={`sa-estado ${cuenta.rol === 'ADMIN' ? 'neutro' : 'info'}`}>
                                        {cuenta.rol}
                                      </span>
                                    </td>
                                    <td>
                                      <span className={`sa-estado ${suspendida ? 'acceso-suspendida' : 'acceso-activa'}`}>
                                        {suspendida ? 'Suspendido' : 'Activo'}
                                      </span>
                                    </td>
                                    <td className="acciones">
                                      <div className="sa-usuario-acciones">
                                        <button
                                          className="sa-accion-staff editar"
                                          type="button"
                                          title={`Editar ${cuenta.usuario}`}
                                          aria-label={`Editar ${cuenta.usuario}`}
                                          onClick={() => abrirEditarUsuario(cuenta)}
                                          disabled={ocupada}
                                        >
                                          <Pencil size={15} aria-hidden="true" />
                                        </button>
                                        <button
                                          className={`sa-accion-staff ${suspendida ? 'reactivar' : 'suspender'}`}
                                          type="button"
                                          title={`${suspendida ? 'Reactivar' : 'Suspender'} el acceso de ${cuenta.usuario}`}
                                          aria-label={`${suspendida ? 'Reactivar' : 'Suspender'} el acceso de ${cuenta.usuario}`}
                                          onClick={() => pedirCambioEstadoUsuario(cuenta)}
                                          disabled={ocupada}
                                        >
                                          {ocupada
                                            ? <LoaderCircle size={15} className="girando" aria-hidden="true" />
                                            : suspendida
                                              ? <Power size={15} aria-hidden="true" />
                                              : <PowerOff size={15} aria-hidden="true" />}
                                        </button>
                                        <button
                                          className="sa-accion-staff eliminar"
                                          type="button"
                                          title={`Eliminar ${cuenta.usuario}`}
                                          aria-label={`Eliminar ${cuenta.usuario}`}
                                          onClick={() => pedirEliminarUsuario(cuenta)}
                                          disabled={ocupada}
                                        >
                                          <Trash2 size={15} aria-hidden="true" />
                                        </button>
                                      </div>
                                    </td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>
                      )}

                      <p className="sa-personal-nota">
                        <ShieldCheck size={14} aria-hidden="true" />
                        Suspender o eliminar una cuenta revoca su sesión de inmediato. La empresa
                        conserva siempre al menos un administrador activo.
                      </p>
                    </section>
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
          </>
        )}

        {vista === 'auditoria' && (
          <section className="sa-auditoria" aria-label="Auditoría del sistema">
            <header className="sa-panel-cabecera">
              <div>
                <h2>Auditoría del Sistema</h2>
                <p className="sa-panel-sub">
                  Altas de tiendas, suspensiones, cambios de plan y restablecimientos de
                  contraseña de toda la plataforma.
                </p>
              </div>
              <span className="sa-contador">
                {entero.format(auditoria.totalElementos)} eventos
              </span>
            </header>

            <div className="sa-auditoria-filtros">
              <div className="sa-buscador">
                <Search size={16} aria-hidden="true" />
                <input
                  type="search"
                  value={textoAuditoria}
                  onChange={(evento) => setTextoAuditoria(evento.target.value)}
                  placeholder="Buscar por empresa, actor o descripción…"
                  aria-label="Buscar en la auditoría del sistema"
                />
              </div>
              <label className="sa-selector">
                <span>Tipo de evento</span>
                <select
                  value={tipoAuditoria}
                  onChange={(evento) => {
                    setTipoAuditoria(evento.target.value);
                    setPaginaAuditoria(0);
                  }}
                >
                  {FILTROS_AUDITORIA.map((filtro) => (
                    <option key={filtro.valor || 'todos'} value={filtro.valor}>
                      {filtro.etiqueta}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {errorAuditoria && (
              <div className="sa-error compacto" role="alert">
                <TriangleAlert size={16} aria-hidden="true" />
                <p>{errorAuditoria}</p>
                <button className="sa-btn" type="button" onClick={() => setRefresco((v) => v + 1)}>
                  Reintentar
                </button>
              </div>
            )}

            {!errorAuditoria && auditoria.contenido.length === 0 && (
              <p className="sa-vacio">
                {cargandoAuditoria
                  ? 'Cargando eventos…'
                  : busquedaAuditoria || tipoAuditoria
                    ? 'Ningún evento coincide con el filtro.'
                    : 'Todavía no hay actividad registrada.'}
              </p>
            )}

            {auditoria.contenido.length > 0 && (
              <div className={`sa-tabla-envoltura${cargandoAuditoria ? ' cargando' : ''}`}>
                <table className="sa-tabla sa-tabla-auditoria">
                  <thead>
                    <tr>
                      <th scope="col">Fecha</th>
                      <th scope="col">Evento</th>
                      <th scope="col">Empresa</th>
                      <th scope="col">Actor</th>
                      <th scope="col">Severidad</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditoria.contenido.map((evento) => (
                      <tr key={evento.idEvento}>
                        <td className="sa-fecha">{formatearFecha(evento.fecha)}</td>
                        <td>
                          <div className="sa-producto">
                            <strong>{etiquetaEvento(evento.tipo)}</strong>
                            <small className="sa-descripcion-evento">{evento.descripcion}</small>
                          </div>
                        </td>
                        <td>
                          <div className="sa-producto">
                            <span>{evento.tiendaNombre || 'Plataforma'}</span>
                            {evento.idTienda != null && <small>Tienda #{evento.idTienda}</small>}
                          </div>
                        </td>
                        <td className="sa-fecha">{evento.actor || 'sistema'}</td>
                        <td>
                          <span className={`sa-estado ${evento.severidad === 'ALERTA' ? 'alerta' : 'neutro'}`}>
                            {evento.severidad === 'ALERTA' ? 'Alerta' : 'Info'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {auditoria.totalPaginas > 1 && (
              <nav className="sa-paginacion" aria-label="Paginación de la auditoría">
                <span className="sa-rango">
                  {primeraAuditoria}–{ultimaAuditoria} de {entero.format(auditoria.totalElementos)}
                </span>
                <div className="sa-pagina-botones">
                  <button
                    className="sa-icono-btn"
                    type="button"
                    aria-label="Página anterior"
                    title="Página anterior"
                    disabled={paginaAuditoria <= 0 || cargandoAuditoria}
                    onClick={() => setPaginaAuditoria((valor) => Math.max(0, valor - 1))}
                  >
                    <ChevronLeft size={18} aria-hidden="true" />
                  </button>
                  <span className="sa-pagina-actual">
                    Página {paginaAuditoria + 1} de {auditoria.totalPaginas}
                  </span>
                  <button
                    className="sa-icono-btn"
                    type="button"
                    aria-label="Página siguiente"
                    title="Página siguiente"
                    disabled={paginaAuditoria + 1 >= auditoria.totalPaginas || cargandoAuditoria}
                    onClick={() => setPaginaAuditoria((valor) => valor + 1)}
                  >
                    <ChevronRight size={18} aria-hidden="true" />
                  </button>
                </div>
              </nav>
            )}
          </section>
        )}

        {confirmacion && (
          <div
            className="sa-modal-fondo"
            role="presentation"
            onClick={() => setConfirmacion(null)}
          >
            <div
              className="sa-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="sa-modal-titulo"
              onClick={(evento) => evento.stopPropagation()}
            >
              <h2 id="sa-modal-titulo">
                {confirmacion.estadoDestino === 'SUSPENDIDA'
                  ? `¿Suspender a ${confirmacion.nombre}?`
                  : `¿Reactivar a ${confirmacion.nombre}?`}
              </h2>
              <p>
                {confirmacion.estadoDestino === 'SUSPENDIDA'
                  ? 'Sus usuarios no podrán iniciar sesión ni operar la tienda hasta que reactives el acceso. Los datos se conservan.'
                  : 'La tienda volverá a permitir el ingreso de sus usuarios de inmediato.'}
              </p>
              <div className="sa-modal-acciones">
                <button
                  className="sa-btn"
                  type="button"
                  onClick={() => setConfirmacion(null)}
                  disabled={cambiandoEstado != null}
                >
                  Cancelar
                </button>
                <button
                  className={`sa-btn ${confirmacion.estadoDestino === 'SUSPENDIDA' ? 'peligro' : 'reactivar'}`}
                  type="button"
                  onClick={confirmarCambioEstado}
                  disabled={cambiandoEstado != null}
                >
                  {cambiandoEstado != null
                    ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                    : confirmacion.estadoDestino === 'SUSPENDIDA'
                      ? <PowerOff size={16} aria-hidden="true" />
                      : <Power size={16} aria-hidden="true" />}
                  {confirmacion.estadoDestino === 'SUSPENDIDA' ? 'Suspender' : 'Reactivar'}
                </button>
              </div>
            </div>
          </div>
        )}
        {confirmacionClave && (
          <div
            className="sa-modal-fondo"
            role="presentation"
            onClick={() => {
              if (!guardandoClave) setConfirmacionClave(null);
            }}
          >
            <div
              className="sa-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="sa-modal-clave-titulo"
              onClick={(evento) => evento.stopPropagation()}
            >
              <h2 id="sa-modal-clave-titulo">
                ¿Restablecer la contraseña de {confirmacionClave.usuario}?
              </h2>
              <p>
                La contraseña actual de {confirmacionClave.nombre} dejará de funcionar y deberá
                iniciar sesión con la nueva clave. La acción quedará registrada en la auditoría
                del sistema.
              </p>
              <div className="sa-modal-acciones">
                <button
                  className="sa-btn"
                  type="button"
                  onClick={() => setConfirmacionClave(null)}
                  disabled={guardandoClave}
                >
                  Cancelar
                </button>
                <button
                  className="sa-btn actualizar"
                  type="button"
                  onClick={confirmarRestablecerClave}
                  disabled={guardandoClave}
                >
                  {guardandoClave
                    ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                    : <KeyRound size={16} aria-hidden="true" />}
                  Restablecer contraseña
                </button>
              </div>
            </div>
          </div>
        )}
        {formStaff && (
          <div
            className="sa-modal-fondo"
            role="presentation"
            onClick={cerrarFormStaff}
          >
            <div
              className="sa-modal sa-modal-form"
              role="dialog"
              aria-modal="true"
              aria-labelledby="sa-modal-staff-titulo"
              onClick={(evento) => evento.stopPropagation()}
            >
              <h2 id="sa-modal-staff-titulo">
                {formStaff.idUsuario ? 'Editar usuario' : 'Nuevo usuario'}
              </h2>
              <p>
                {formStaff.idUsuario
                  ? `Actualiza los datos, el rol y, si hace falta, la contraseña de la cuenta en ${empresa?.nombre || 'la empresa'}.`
                  : `La cuenta se creará dentro de ${empresa?.nombre || 'la empresa'} con acceso al panel.`}
              </p>
              <form className="sa-staff-form" onSubmit={guardarStaff}>
                {errorStaff && (
                  <div className="sa-error compacto" role="alert">
                    <TriangleAlert size={16} aria-hidden="true" />
                    <p>{errorStaff}</p>
                  </div>
                )}

                <div className="sa-staff-grid">
                  <label className="sa-campo">
                    <span>Nombre de usuario</span>
                    <input
                      type="text"
                      value={formStaff.usuario}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        usuario: evento.target.value,
                      }))}
                      autoComplete="off"
                      minLength={3}
                      maxLength={50}
                      required
                      disabled={guardandoStaff}
                    />
                  </label>

                  <label className="sa-campo">
                    <span>Correo electrónico</span>
                    <input
                      type="email"
                      value={formStaff.correo}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        correo: evento.target.value,
                      }))}
                      autoComplete="off"
                      maxLength={100}
                      required
                      disabled={guardandoStaff}
                    />
                  </label>

                  <label className="sa-campo">
                    <span>Nombre</span>
                    <input
                      type="text"
                      value={formStaff.nombre}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        nombre: evento.target.value,
                      }))}
                      maxLength={50}
                      required
                      disabled={guardandoStaff}
                    />
                  </label>

                  <label className="sa-campo">
                    <span>Apellido</span>
                    <input
                      type="text"
                      value={formStaff.apellido}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        apellido: evento.target.value,
                      }))}
                      maxLength={50}
                      disabled={guardandoStaff}
                    />
                  </label>

                  <label className="sa-campo">
                    <span>Rol</span>
                    <select
                      value={formStaff.rol}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        rol: evento.target.value,
                      }))}
                      disabled={guardandoStaff}
                    >
                      {ROLES_STAFF.map((opcion) => (
                        <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>
                      ))}
                    </select>
                  </label>

                  <label className="sa-campo">
                    <span>{formStaff.idUsuario ? 'Nueva contraseña (opcional)' : 'Contraseña'}</span>
                    <div className="sa-clave-entrada">
                      <input
                        type={mostrarClaveStaff ? 'text' : 'password'}
                        value={formStaff.password}
                        onChange={(evento) => setFormStaff((actual) => ({
                          ...actual,
                          password: evento.target.value,
                        }))}
                        autoComplete="new-password"
                        minLength={8}
                        maxLength={72}
                        required={!formStaff.idUsuario}
                        disabled={guardandoStaff}
                      />
                      <button
                        className="sa-clave-ojo"
                        type="button"
                        onClick={() => setMostrarClaveStaff((valor) => !valor)}
                        aria-label={mostrarClaveStaff ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                        title={mostrarClaveStaff ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                      >
                        {mostrarClaveStaff
                          ? <EyeOff size={16} aria-hidden="true" />
                          : <Eye size={16} aria-hidden="true" />}
                      </button>
                    </div>
                  </label>

                  <label className="sa-campo">
                    <span>Confirmar contraseña</span>
                    <input
                      type={mostrarClaveStaff ? 'text' : 'password'}
                      value={formStaff.confirmacion}
                      onChange={(evento) => setFormStaff((actual) => ({
                        ...actual,
                        confirmacion: evento.target.value,
                      }))}
                      autoComplete="new-password"
                      minLength={8}
                      maxLength={72}
                      required={!formStaff.idUsuario || Boolean(formStaff.password)}
                      disabled={guardandoStaff}
                    />
                  </label>
                </div>

                <p className="sa-staff-ayuda">
                  Mínimo 8 caracteres.
                  {formStaff.idUsuario ? ' Déjala vacía para conservar la contraseña actual.' : ''}
                </p>

                <div className="sa-modal-acciones">
                  <button
                    className="sa-btn"
                    type="button"
                    onClick={cerrarFormStaff}
                    disabled={guardandoStaff}
                  >
                    Cancelar
                  </button>
                  <button className="sa-btn actualizar" type="submit" disabled={guardandoStaff}>
                    {guardandoStaff
                      ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                      : <Save size={16} aria-hidden="true" />}
                    {formStaff.idUsuario ? 'Guardar cambios' : 'Crear usuario'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
        {confirmacionStaff && (
          <div
            className="sa-modal-fondo"
            role="presentation"
            onClick={() => {
              if (!cambiandoStaff) setConfirmacionStaff(null);
            }}
          >
            <div
              className="sa-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="sa-modal-staff-conf-titulo"
              onClick={(evento) => evento.stopPropagation()}
            >
              <h2 id="sa-modal-staff-conf-titulo">
                {confirmacionStaff.tipo === 'eliminar'
                  ? `¿Eliminar la cuenta de ${confirmacionStaff.cuenta.usuario}?`
                  : confirmacionStaff.destino === 'SUSPENDIDO'
                    ? `¿Suspender el acceso de ${confirmacionStaff.cuenta.usuario}?`
                    : `¿Reactivar el acceso de ${confirmacionStaff.cuenta.usuario}?`}
              </h2>
              <p>
                {confirmacionStaff.tipo === 'eliminar'
                  ? 'Se borrará la cuenta y su ficha de contacto. Si la cuenta tiene comprobantes emitidos la operación se rechaza; en ese caso suspéndela.'
                  : confirmacionStaff.destino === 'SUSPENDIDO'
                    ? 'La cuenta no podrá iniciar sesión y sus sesiones abiertas se cerrarán de inmediato. Los datos se conservan.'
                    : 'La cuenta podrá volver a iniciar sesión de inmediato.'}
              </p>
              <div className="sa-modal-acciones">
                <button
                  className="sa-btn"
                  type="button"
                  onClick={() => setConfirmacionStaff(null)}
                  disabled={cambiandoStaff != null}
                >
                  Cancelar
                </button>
                <button
                  className={`sa-btn ${confirmacionStaff.tipo === 'eliminar'
                    || confirmacionStaff.destino === 'SUSPENDIDO' ? 'peligro' : 'reactivar'}`}
                  type="button"
                  onClick={confirmarAccionStaff}
                  disabled={cambiandoStaff != null}
                >
                  {cambiandoStaff != null
                    ? <LoaderCircle size={16} className="girando" aria-hidden="true" />
                    : confirmacionStaff.tipo === 'eliminar'
                      ? <Trash2 size={16} aria-hidden="true" />
                      : confirmacionStaff.destino === 'SUSPENDIDO'
                        ? <PowerOff size={16} aria-hidden="true" />
                        : <Power size={16} aria-hidden="true" />}
                  {confirmacionStaff.tipo === 'eliminar'
                    ? 'Eliminar'
                    : confirmacionStaff.destino === 'SUSPENDIDO' ? 'Suspender' : 'Reactivar'}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
