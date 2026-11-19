import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRight, RefreshCw } from 'lucide-react';
import { useSesion } from '../auth/sesionStore';
import { consultarSolicitud, enviarRespuesta, listarSolicitudes, resolverSolicitud } from './solicitudesApi';
import './Solicitudes.css';

const ESTADOS = {
  PENDIENTE: 'Pendiente',
  REQUIERE_DATOS: 'Se requiere información',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
};

function Estado({ valor }) {
  return <span className={`flujo-estado estado-${valor.toLowerCase()}`}>{ESTADOS[valor]}</span>;
}

export default function PanelSolicitud({ maestro = false }) {
  const [registros, setRegistros] = useState(null);
  const [filtro, setFiltro] = useState('TODAS');
  const [seleccion, setSeleccion] = useState('');
  const [decision, setDecision] = useState('APROBADA');
  const [mensaje, setMensaje] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const logout = useSesion((estado) => estado.logout);

  const actualizar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const lista = maestro ? await listarSolicitudes() : [await consultarSolicitud()];
      setRegistros(lista);
    } catch (problema) {
      setError(problema.message || 'No se pudieron cargar las solicitudes.');
    } finally {
      setCargando(false);
    }
  }, [maestro]);

  useEffect(() => {
    let vigente = true;
    const consulta = maestro ? listarSolicitudes() : consultarSolicitud().then((registro) => [registro]);
    consulta.then(
      (lista) => { if (vigente) setRegistros(lista); },
      (problema) => { if (vigente) setError(problema.message || 'No se pudieron cargar las solicitudes.'); }
    ).finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [maestro]);

  const visibles = (registros || []).filter((item) => filtro === 'TODAS' || item.estado === filtro);
  const actual = visibles.find((item) => item.id === seleccion) ?? visibles[0];
  const puedeDecidir = actual && ['PENDIENTE', 'REQUIERE_DATOS'].includes(actual.estado);
  const pendientes = (registros || []).filter((item) => item.estado === 'PENDIENTE').length;

  async function enviar(evento) {
    evento.preventDefault();
    if (!actual) return;
    setAviso('');
    setError('');
    setGuardando(true);
    try {
      const resultado = maestro
        ? await resolverSolicitud(actual.id, decision, mensaje)
        : await enviarRespuesta(mensaje);
      setRegistros((lista) => lista.map((item) => item.id === resultado.id ? resultado : item));
      setMensaje('');
      setAviso(maestro ? 'Decisión guardada.' : 'Respuesta enviada al panel maestro.');
    } catch (problema) {
      setError(problema.message || 'No se pudo guardar el cambio.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <main className="page flujo-page">
      <header className="flujo-cabecera">
        <div><h1>{maestro ? 'Solicitudes de tienda' : 'Mi solicitud'}</h1>
          <p className="muted">{maestro ? `${pendientes} pendientes de revisión` : 'Consulta si tu negocio ya está listo para configurarse.'}</p>
        </div>
        <div className="flujo-acciones">
          <button type="button" className="btn ghost" onClick={actualizar} disabled={cargando || guardando}>
            <RefreshCw size={16} aria-hidden="true" /> {cargando ? 'Actualizando…' : 'Actualizar'}
          </button>
          <Link className="btn ghost" to="/admin/login" onClick={logout}>Cerrar sesión</Link>
        </div>
      </header>
      {error && <p className="flujo-error" role="alert">{error}</p>}
      {aviso && <p className="flujo-aviso" role="status">{aviso}</p>}
      {cargando && !registros && <p role="status">Cargando solicitudes…</p>}
      <div className={maestro ? 'flujo-bandeja' : ''}>
        {maestro && (
          <aside className="flujo-card">
            <label className="field">Filtrar por estado
              <select value={filtro} onChange={(e) => { setFiltro(e.target.value); setMensaje(''); setDecision('APROBADA'); }}>
                <option value="TODAS">Todas</option>
                {Object.entries(ESTADOS).map(([clave, texto]) => <option key={clave} value={clave}>{texto}</option>)}
              </select>
            </label>
            <div className="flujo-lista">
              {registros && !visibles.length && <p className="muted">No hay solicitudes en este estado.</p>}
              {visibles.map((item) => (
                <button key={item.id} className="flujo-fila" type="button" aria-pressed={actual?.id === item.id}
                  onClick={() => { setSeleccion(item.id); setDecision('APROBADA'); setMensaje(''); }}>
                  <strong>{item.negocio}</strong><Estado valor={item.estado} />
                </button>
              ))}
            </div>
          </aside>
        )}
        {actual && (
          <article className="flujo-card flujo-detalle">
            <div className="flujo-cabecera"><h2>{actual.negocio}</h2><Estado valor={actual.estado} /></div>
            <dl className="flujo-datos">
              <div><dt>Solicitante</dt><dd>{actual.nombres} {actual.apellidos}</dd></div>
              <div><dt>Correo</dt><dd>{actual.correo}</dd></div>
              <div><dt>Celular principal</dt><dd>{actual.telefono}</dd></div>
              <div><dt>Celular alternativo</dt><dd>{actual.telefonoAlternativo}</dd></div>
              <div><dt>DNI</dt><dd>••••{actual.dniUltimos4}</dd></div>
              <div><dt>Catálogo</dt><dd>/tienda/{actual.slug}</dd></div>
              <div><dt>Pago simulado</dt><dd className="small">{actual.referenciaDemo}</dd></div>
            </dl>
            {actual.mensaje && <p className="flujo-aviso"><strong>Mensaje de NovaCommerce</strong><br />{actual.mensaje}</p>}
            {actual.respuesta && <p className="flujo-aviso"><strong>Respuesta del negocio</strong><br />{actual.respuesta}</p>}
            {((maestro && puedeDecidir) || (!maestro && actual.estado === 'REQUIERE_DATOS')) && (
              <form className="flujo-form" onSubmit={enviar}>
                {maestro && (
                  <label className="field">Decisión
                    <select value={decision} onChange={(e) => setDecision(e.target.value)}>
                      <option value="APROBADA">Aprobar y crear la tienda</option>
                      <option value="REQUIERE_DATOS">Pedir información</option>
                      <option value="RECHAZADA">Rechazar solicitud</option>
                    </select>
                  </label>
                )}
                <label className="field">{maestro ? 'Mensaje al solicitante' : 'Tu respuesta'}
                  <textarea value={mensaje} onChange={(e) => setMensaje(e.target.value)}
                    rows="3" maxLength={500} required={!maestro || decision !== 'APROBADA'} />
                </label>
                <button className="btn" disabled={guardando} type="submit">
                  {guardando ? 'Guardando…' : maestro ? 'Confirmar decisión' : 'Enviar respuesta'}
                </button>
              </form>
            )}
            {!maestro && actual.estado === 'APROBADA' && (
              <div className="flujo-aviso">
                <p>Tu tienda está lista. Inicia sesión con el mismo correo y contraseña para configurarla.</p>
                <Link className="btn" to="/admin/login" onClick={logout}>Configurar mi tienda <ArrowUpRight size={16} /></Link>
              </div>
            )}
          </article>
        )}
      </div>
    </main>
  );
}
