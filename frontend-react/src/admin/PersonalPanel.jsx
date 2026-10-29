import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Eye,
  EyeOff,
  LoaderCircle,
  Pencil,
  Power,
  PowerOff,
  RefreshCw,
  Save,
  Search,
  TriangleAlert,
  UserPlus,
  Users,
  X,
} from 'lucide-react';
import {
  actualizarPersonal,
  cambiarEstadoPersonal,
  crearPersonal,
  obtenerPersonal,
} from '../api/personalApi';
import { showToast } from '../components/toastBus';
import PanelLayout from './PanelLayout';
import './HistorialVentas.css';
import './PersonalPanel.css';

const FORM_VACIO = {
  idUsuario: null,
  usuario: '',
  nombre: '',
  apellido: '',
  correo: '',
  password: '',
  confirmacion: '',
};

function mensajeDe(problema, porDefecto) {
  return problema?.message || porDefecto;
}

function esCorreoValido(correo) {
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo);
}

// El alta de personal lo valida también el backend. Aquí solo se atajan los
// errores evidentes para que el administrador no espere una ida al servidor.
function validarFormulario(formulario) {
  const esAlta = !formulario.idUsuario;
  const usuario = formulario.usuario.trim();
  if (usuario.length < 3) return 'El nombre de usuario debe tener al menos 3 caracteres.';
  if (usuario.length > 50) return 'El nombre de usuario no puede superar los 50 caracteres.';
  if (/\s/.test(usuario)) return 'El nombre de usuario no puede contener espacios.';
  if (!formulario.nombre.trim()) return 'El nombre es obligatorio.';
  if (formulario.nombre.trim().length > 50) return 'El nombre no puede superar los 50 caracteres.';
  if (formulario.apellido.trim().length > 50) {
    return 'El apellido no puede superar los 50 caracteres.';
  }
  if (!esCorreoValido(formulario.correo.trim())) {
    return 'El correo electrónico no tiene un formato válido.';
  }
  if (esAlta && !formulario.password) return 'La contraseña es obligatoria para crear la cuenta.';
  if (formulario.password) {
    if (formulario.password.length < 8) return 'La contraseña debe tener al menos 8 caracteres.';
    if (formulario.password.length > 72) {
      return 'La contraseña no puede superar los 72 caracteres.';
    }
    if (formulario.password !== formulario.confirmacion) {
      return 'La confirmación no coincide con la contraseña.';
    }
  }
  return '';
}

export default function PersonalPanel() {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [texto, setTexto] = useState('');
  const [refresco, setRefresco] = useState(0);

  const [formulario, setFormulario] = useState(null);
  const [errorFormulario, setErrorFormulario] = useState('');
  const [mostrarClave, setMostrarClave] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const [confirmacion, setConfirmacion] = useState(null);
  const [cambiando, setCambiando] = useState(null);

  useEffect(() => {
    let vigente = true;
    (async () => {
      try {
        const respuesta = await obtenerPersonal();
        if (!vigente) return;
        setDatos(respuesta);
        setError('');
      } catch (problema) {
        if (!vigente) return;
        setError(mensajeDe(problema, 'No se pudo cargar el personal de la tienda.'));
        setDatos(null);
      } finally {
        if (vigente) setCargando(false);
      }
    })();
    return () => {
      vigente = false;
    };
  }, [refresco]);

  const recargar = useCallback(() => {
    setCargando(true);
    setError('');
    setRefresco((valor) => valor + 1);
  }, []);

  const personal = useMemo(
    () => (Array.isArray(datos?.personal) ? datos.personal : []),
    [datos],
  );

  const filtrados = useMemo(() => {
    const termino = texto.trim().toLowerCase();
    if (!termino) return personal;
    return personal.filter((cuenta) => [cuenta.usuario, cuenta.nombreCompleto,
      cuenta.correo, cuenta.estado]
      .some((valor) => String(valor || '').toLowerCase().includes(termino)));
  }, [personal, texto]);

  const limite = Number(datos?.limitePersonal) || 0;
  const enUso = Number(datos?.personalEnUso) || 0;
  const sinLimite = limite <= 0;
  const cupoCompleto = !sinLimite && enUso >= limite;
  const libres = sinLimite ? null : Math.max(0, limite - enUso);
  const porcentajeUso = sinLimite ? 0 : Math.min(100, Math.round((enUso / limite) * 100));

  function abrirAlta() {
    setFormulario({ ...FORM_VACIO });
    setErrorFormulario('');
    setMostrarClave(false);
  }

  function abrirEdicion(cuenta) {
    setFormulario({
      idUsuario: cuenta.idUsuario,
      usuario: cuenta.usuario || '',
      nombre: cuenta.nombre || '',
      apellido: cuenta.apellido || '',
      correo: cuenta.correo || '',
      password: '',
      confirmacion: '',
    });
    setErrorFormulario('');
    setMostrarClave(false);
  }

  function cerrarFormulario() {
    if (guardando) return;
    setFormulario(null);
    setErrorFormulario('');
  }

  async function guardar(evento) {
    evento.preventDefault();
    if (!formulario || guardando) return;

    const problema = validarFormulario(formulario);
    if (problema) {
      setErrorFormulario(problema);
      return;
    }

    const esAlta = !formulario.idUsuario;
    const cuerpo = {
      usuario: formulario.usuario.trim(),
      nombre: formulario.nombre.trim(),
      apellido: formulario.apellido.trim(),
      correo: formulario.correo.trim().toLowerCase(),
    };
    if (formulario.password) cuerpo.password = formulario.password;

    setGuardando(true);
    setErrorFormulario('');
    try {
      if (esAlta) {
        await crearPersonal(cuerpo);
        showToast(`Vendedor ${cuerpo.usuario} creado.`);
      } else {
        await actualizarPersonal(formulario.idUsuario, cuerpo);
        showToast(`Cuenta ${cuerpo.usuario} actualizada.`);
      }
      setFormulario(null);
      setMostrarClave(false);
      recargar();
    } catch (fallo) {
      setErrorFormulario(mensajeDe(fallo, 'No se pudo guardar la cuenta.'));
    } finally {
      setGuardando(false);
    }
  }

  function pedirCambioEstado(cuenta) {
    setConfirmacion({
      cuenta,
      destino: cuenta.estado === 'SUSPENDIDO' ? 'ACTIVO' : 'SUSPENDIDO',
    });
  }

  async function confirmarCambioEstado() {
    if (!confirmacion || cambiando) return;
    const { cuenta, destino } = confirmacion;
    setCambiando(cuenta.idUsuario);
    try {
      await cambiarEstadoPersonal(cuenta.idUsuario, destino);
      showToast(destino === 'SUSPENDIDO'
        ? `Acceso de ${cuenta.usuario} suspendido.`
        : `Acceso de ${cuenta.usuario} reactivado.`);
      setConfirmacion(null);
      recargar();
    } catch (fallo) {
      showToast(mensajeDe(fallo, 'No se pudo cambiar el estado de la cuenta.'));
      setConfirmacion(null);
    } finally {
      setCambiando(null);
    }
  }

  const titulo = formulario?.idUsuario ? 'Editar vendedor' : 'Nuevo vendedor';

  return (
    <PanelLayout>
      <main className="page historial-page">
        <header className="hist-cabecera">
          <div>
            <h1>Personal de la tienda.</h1>
            <p>
              Cuentas con rol VENDEDOR de {datos?.tiendaNombre || 'tu empresa'}. Solo ves y
              gestionas el personal de tu propia tienda.
            </p>
          </div>
          <div className="pp-cabecera-acciones">
            <button
              className="pp-btn primario"
              type="button"
              onClick={abrirAlta}
              disabled={cargando || cupoCompleto}
              title={cupoCompleto
                ? 'El plan contratado no tiene puestos de personal disponibles'
                : 'Crear una cuenta VENDEDOR'}
            >
              <UserPlus size={16} aria-hidden="true" />
              Nuevo vendedor
            </button>
            <button
              className="hist-btn actualizar"
              type="button"
              onClick={recargar}
              disabled={cargando}
            >
              <RefreshCw size={16} aria-hidden="true" className={cargando ? 'girando' : ''} />
              Actualizar
            </button>
          </div>
        </header>

        {datos && (
          <section
            className={`pp-cupo${cupoCompleto ? ' completo' : ''}`}
            aria-label="Uso de puestos del plan"
          >
            <div className="pp-cupo-icono">
              <Users size={22} aria-hidden="true" />
            </div>
            <div className="pp-cupo-texto">
              <strong>
                {sinLimite
                  ? `${enUso} puesto${enUso === 1 ? '' : 's'} en uso · plan sin límite de personal`
                  : `${enUso} de ${limite} puestos de personal`}
              </strong>
              <span>
                {sinLimite
                  ? 'El plan contratado no fija un máximo de cuentas de personal.'
                  : cupoCompleto
                    ? 'El plan está completo. Amplía el plan para crear otra cuenta de personal.'
                    : `El administrador ocupa un puesto: puedes crear ${libres} vendedor${libres === 1 ? '' : 'es'} más.`}
              </span>
            </div>
            {!sinLimite && (
              <div
                className="pp-cupo-barra"
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={limite}
                aria-valuenow={enUso}
                aria-label="Puestos de personal ocupados"
              >
                <span style={{ width: `${porcentajeUso}%` }} />
              </div>
            )}
          </section>
        )}

        <section className="hist-toolbar" aria-label="Buscar personal">
          <div className="hist-buscador">
            <Search size={18} aria-hidden="true" />
            <input
              type="search"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder="Buscar por usuario, nombre, correo o estado..."
              aria-label="Buscar personal por usuario, nombre, correo o estado"
            />
          </div>
          <span className="hist-contador">
            {personal.length === 1 ? '1 vendedor' : `${personal.length} vendedores`}
          </span>
        </section>

        {error && (
          <div className="hist-error" role="alert">
            <p>{error}</p>
            <button className="hist-btn" type="button" onClick={recargar}>Reintentar</button>
          </div>
        )}

        <section className="hist-panel" aria-label="Listado de personal">
          {!error && filtrados.length === 0 && (
            <p className="hist-vacio">
              {cargando
                ? 'Cargando personal…'
                : texto
                  ? `Ningún vendedor coincide con “${texto.trim()}”.`
                  : 'Todavía no hay vendedores en tu tienda. Crea la primera cuenta con “Nuevo vendedor”.'}
            </p>
          )}

          {filtrados.length > 0 && (
            <div className={`hist-tabla-envoltura${cargando ? ' cargando' : ''}`}>
              <table className="hist-tabla pp-tabla">
                <thead>
                  <tr>
                    <th scope="col">Usuario</th>
                    <th scope="col">Correo</th>
                    <th scope="col">Estado</th>
                    <th scope="col" className="acciones">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((cuenta) => {
                    const suspendida = cuenta.estado === 'SUSPENDIDO';
                    const ocupada = cambiando === cuenta.idUsuario;
                    return (
                      <tr key={cuenta.idUsuario}>
                        <td>
                          <div className="hist-cliente">
                            <span>{cuenta.usuario}</span>
                            <small>{cuenta.nombreCompleto || 'Sin nombre registrado'}</small>
                          </div>
                        </td>
                        <td>{cuenta.correo || '—'}</td>
                        <td>
                          <span className={`hist-estado ${suspendida ? 'error' : 'ok'}`}>
                            {suspendida ? 'Suspendido' : 'Activo'}
                          </span>
                        </td>
                        <td>
                          <div className="pp-acciones">
                            <button
                              className="pp-accion editar"
                              type="button"
                              onClick={() => abrirEdicion(cuenta)}
                              disabled={ocupada}
                            >
                              <Pencil size={15} aria-hidden="true" />
                              Editar
                            </button>
                            <button
                              className={`pp-accion ${suspendida ? 'reactivar' : 'suspender'}`}
                              type="button"
                              onClick={() => pedirCambioEstado(cuenta)}
                              disabled={ocupada}
                            >
                              {suspendida
                                ? <Power size={15} aria-hidden="true" />
                                : <PowerOff size={15} aria-hidden="true" />}
                              {suspendida ? 'Reactivar' : 'Suspender'}
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
        </section>

        {!cargando && !error && personal.length > 0 && (
          <p className="pp-nota">
            Las cuentas creadas aquí entran solo a esta tienda y no pueden administrar el
            catálogo, la marca ni el personal.
          </p>
        )}
      </main>

      {formulario && (
        <div
          className="pp-overlay"
          role="presentation"
          onClick={cerrarFormulario}
        >
          <div
            className="pp-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="pp-modal-titulo"
            onClick={(evento) => evento.stopPropagation()}
          >
            <h2 id="pp-modal-titulo" className="pp-titulo">{titulo}</h2>
            <p className="pp-mensaje">
              {formulario.idUsuario
                ? 'Actualiza los datos de la cuenta y, si hace falta, su contraseña.'
                : `La cuenta se creará como VENDEDOR dentro de ${datos?.tiendaNombre || 'tu empresa'}.`}
            </p>

            {errorFormulario && (
              <div className="pp-error" role="alert">
                <TriangleAlert size={16} aria-hidden="true" />
                <p>{errorFormulario}</p>
              </div>
            )}

            <form className="pp-form" onSubmit={guardar} noValidate>
              <div className="pp-grid">
                <label className="pp-campo">
                  <span>Nombre de usuario</span>
                  <input
                    type="text"
                    value={formulario.usuario}
                    onChange={(evento) => setFormulario((actual) => ({
                      ...actual,
                      usuario: evento.target.value,
                    }))}
                    autoComplete="off"
                    minLength={3}
                    maxLength={50}
                    required
                    disabled={guardando}
                    autoFocus
                  />
                </label>

                <label className="pp-campo">
                  <span>Correo electrónico</span>
                  <input
                    type="email"
                    value={formulario.correo}
                    onChange={(evento) => setFormulario((actual) => ({
                      ...actual,
                      correo: evento.target.value,
                    }))}
                    autoComplete="off"
                    maxLength={100}
                    required
                    disabled={guardando}
                  />
                </label>

                <label className="pp-campo">
                  <span>Nombre</span>
                  <input
                    type="text"
                    value={formulario.nombre}
                    onChange={(evento) => setFormulario((actual) => ({
                      ...actual,
                      nombre: evento.target.value,
                    }))}
                    maxLength={50}
                    required
                    disabled={guardando}
                  />
                </label>

                <label className="pp-campo">
                  <span>Apellido</span>
                  <input
                    type="text"
                    value={formulario.apellido}
                    onChange={(evento) => setFormulario((actual) => ({
                      ...actual,
                      apellido: evento.target.value,
                    }))}
                    maxLength={50}
                    disabled={guardando}
                  />
                </label>

                <label className="pp-campo">
                  <span>{formulario.idUsuario ? 'Nueva contraseña (opcional)' : 'Contraseña'}</span>
                  <div className="pp-clave">
                    <input
                      type={mostrarClave ? 'text' : 'password'}
                      value={formulario.password}
                      onChange={(evento) => setFormulario((actual) => ({
                        ...actual,
                        password: evento.target.value,
                      }))}
                      autoComplete="new-password"
                      minLength={8}
                      maxLength={72}
                      required={!formulario.idUsuario}
                      disabled={guardando}
                    />
                    <button
                      className="pp-clave-ojo"
                      type="button"
                      onClick={() => setMostrarClave((valor) => !valor)}
                      aria-label={mostrarClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                      title={mostrarClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                      {mostrarClave
                        ? <EyeOff size={16} aria-hidden="true" />
                        : <Eye size={16} aria-hidden="true" />}
                    </button>
                  </div>
                </label>

                <label className="pp-campo">
                  <span>Confirmar contraseña</span>
                  <input
                    type={mostrarClave ? 'text' : 'password'}
                    value={formulario.confirmacion}
                    onChange={(evento) => setFormulario((actual) => ({
                      ...actual,
                      confirmacion: evento.target.value,
                    }))}
                    autoComplete="new-password"
                    minLength={8}
                    maxLength={72}
                    required={!formulario.idUsuario || Boolean(formulario.password)}
                    disabled={guardando}
                  />
                </label>
              </div>

              <p className="pp-hint">
                Rol asignado: <strong>VENDEDOR</strong>. Desde este panel no se pueden crear
                administradores.
              </p>

              <div className="pp-acciones-modal">
                <button className="pp-modal-btn primario" type="submit" disabled={guardando}>
                  {guardando
                    ? <LoaderCircle className="pp-girando" size={18} aria-hidden="true" />
                    : <Save size={18} aria-hidden="true" />}
                  {guardando ? 'Guardando...' : formulario.idUsuario ? 'Guardar cambios' : 'Crear vendedor'}
                </button>
                <button
                  className="pp-modal-btn secundario"
                  type="button"
                  onClick={cerrarFormulario}
                  disabled={guardando}
                >
                  <X size={18} aria-hidden="true" />
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {confirmacion && (
        <div
          className="pp-overlay"
          role="presentation"
          onClick={() => !cambiando && setConfirmacion(null)}
        >
          <div
            className="pp-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="pp-confirmacion-titulo"
            onClick={(evento) => evento.stopPropagation()}
          >
            <h2 id="pp-confirmacion-titulo" className="pp-titulo">
              {confirmacion.destino === 'SUSPENDIDO' ? 'Suspender acceso' : 'Reactivar acceso'}
            </h2>
            <p className="pp-mensaje">
              {confirmacion.destino === 'SUSPENDIDO'
                ? `La cuenta ${confirmacion.cuenta.usuario} dejará de poder iniciar sesión. Sus ventas y registros se conservan.`
                : `La cuenta ${confirmacion.cuenta.usuario} podrá volver a iniciar sesión en el panel.`}
            </p>
            <div className="pp-acciones-modal">
              <button
                className="pp-modal-btn primario"
                type="button"
                onClick={confirmarCambioEstado}
                disabled={cambiando !== null}
              >
                {cambiando !== null
                  ? <LoaderCircle className="pp-girando" size={18} aria-hidden="true" />
                  : confirmacion.destino === 'SUSPENDIDO'
                    ? <PowerOff size={18} aria-hidden="true" />
                    : <Power size={18} aria-hidden="true" />}
                {cambiando !== null
                  ? 'Procesando...'
                  : confirmacion.destino === 'SUSPENDIDO' ? 'Suspender' : 'Reactivar'}
              </button>
              <button
                className="pp-modal-btn secundario"
                type="button"
                onClick={() => setConfirmacion(null)}
                disabled={cambiando !== null}
              >
                <X size={18} aria-hidden="true" />
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </PanelLayout>
  );
}
