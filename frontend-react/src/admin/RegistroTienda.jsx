import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, CreditCard, Store } from 'lucide-react';
import { loginApi } from '../auth/authApi';
import { useSesion } from '../auth/sesionStore';
import { slugify } from '../store/slugify';
import { solicitarTienda } from '../solicitudes/solicitudesApi';
import '../solicitudes/Solicitudes.css';

const INICIAL = {
  nombres: '', apellidos: '', dni: '', telefono: '', telefonoAlternativo: '',
  correo: '', contrasena: '', negocio: '', rubro: '', slug: '', pagoSimulado: false,
};

const RUBROS = ['Moda', 'Alimentos', 'Tecnología', 'Salud y belleza', 'Hogar', 'Deportes', 'Otro'];
const ETAPAS = ['Tu negocio', 'Tu cuenta', 'Enviar solicitud'];

function Campo({ etiqueta, name, value, editar, ...props }) {
  return (
    <label className="field">
      <span>{etiqueta}</span>
      <input name={name} value={value} onChange={editar} required {...props} />
    </label>
  );
}

export default function RegistroTienda() {
  const [datos, setDatos] = useState(INICIAL);
  const [paso, setPaso] = useState(0);
  const [slugEditado, setSlugEditado] = useState(false);
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const login = useSesion((estado) => estado.login);
  const navigate = useNavigate();

  function editar(evento) {
    const { name, value } = evento.target;
    setError('');
    setDatos((previo) => ({
      ...previo, [name]: value,
      ...(name === 'negocio' && !slugEditado ? { slug: slugify(value).slice(0, 80).replace(/-+$/, '') } : {}),
    }));
    if (name === 'slug') setSlugEditado(true);
  }

  async function continuar(evento) {
    evento.preventDefault();
    if (paso === 1 && datos.telefono === datos.telefonoAlternativo) {
      setError('Ingresa dos números telefónicos distintos.');
      return;
    }
    if (paso < 2) {
      setPaso(paso + 1);
      return;
    }

    setEnviando(true);
    setError('');
    try {
      await solicitarTienda(datos);
      try {
        const cuenta = await loginApi(datos.correo, datos.contrasena);
        login(cuenta.usuario, cuenta.token, cuenta.rol, cuenta.tienda);
        navigate('/solicitud-tienda', { replace: true });
      } catch {
        navigate('/admin/login', { replace: true, state: { solicitudEnviada: true } });
      }
    } catch (problema) {
      setError(problema.message || 'No se pudo enviar la solicitud.');
    } finally {
      setEnviando(false);
    }
  }

  const campo = (name) => ({ name, value: datos[name], editar });

  return (
    <main className="page flujo-page">
      <div className="flujo-grid">
        <aside className="flujo-intro">
          <Store size={28} aria-hidden="true" />
          <h1>Abre tu tienda</h1>
          <p>Envía tu solicitud. Cuando NovaCommerce la apruebe, podrás configurar el negocio con esta misma cuenta.</p>
          <ol className="flujo-pasos">
            {ETAPAS.map((etapa, i) => <li key={etapa} aria-current={paso === i ? 'step' : undefined}>{etapa}</li>)}
          </ol>
          <Link to="/demo" className="flujo-enlace">Explorar la demo</Link>
        </aside>
        <form className="flujo-card flujo-form" onSubmit={continuar}>
          <h2>{ETAPAS[paso]}</h2>
          {paso === 0 && (
            <>
              <Campo {...campo('negocio')} etiqueta="Nombre del negocio" autoComplete="organization" maxLength={100} />
              <label className="field">
                <span>Rubro</span>
                <select name="rubro" value={datos.rubro} onChange={editar} required>
                  <option value="">Selecciona un rubro</option>
                  {RUBROS.map((rubro) => <option key={rubro}>{rubro}</option>)}
                </select>
              </label>
              <Campo {...campo('slug')} etiqueta="Dirección del catálogo" placeholder="mi-negocio" maxLength={80}
                pattern="[a-z0-9]+(-[a-z0-9]+)*" title="Usa minúsculas, números y guiones entre palabras." />
              <p className="small muted">Tu catálogo se publicará en /tienda/{datos.slug || 'mi-negocio'}.</p>
            </>
          )}
          {paso === 1 && (
            <>
              <div className="flujo-campos-dobles">
                <Campo {...campo('nombres')} etiqueta="Nombres" autoComplete="given-name" maxLength={50} />
                <Campo {...campo('apellidos')} etiqueta="Apellidos" autoComplete="family-name" maxLength={50} />
              </div>
              <Campo {...campo('dni')} etiqueta="DNI" inputMode="numeric" pattern="[0-9]{8}" maxLength={8} autoComplete="off" />
              <p className="small muted">Guardamos únicamente los últimos cuatro dígitos. Para esta demostración, usa datos ficticios.</p>
              <div className="flujo-campos-dobles">
                <Campo {...campo('telefono')} etiqueta="Celular principal" type="tel" autoComplete="tel-national"
                  pattern="9[0-9]{8}" title="9 dígitos, empieza con 9." maxLength={9} />
                <Campo {...campo('telefonoAlternativo')} etiqueta="Celular alternativo" type="tel" autoComplete="off"
                  pattern="9[0-9]{8}" title="9 dígitos, empieza con 9." maxLength={9} />
              </div>
              <Campo {...campo('correo')} etiqueta="Correo electrónico" type="email" autoComplete="email" maxLength={50} />
              <Campo {...campo('contrasena')} etiqueta="Contraseña" type="password" autoComplete="new-password"
                minLength={12} maxLength={72} />
              <p className="small muted">Usa al menos 12 caracteres. Tus teléfonos sirven para contactarte.</p>
            </>
          )}
          {paso === 2 && (
            <>
              <div className="flujo-tarjeta-demo">
                <CreditCard size={24} aria-hidden="true" />
                <div><strong>Tarjeta ficticia •••• 4242</strong><p>Simulación académica. No se realizará ningún cobro.</p></div>
              </div>
              <label className="flujo-check">
                <input type="checkbox" checked={datos.pagoSimulado} required
                  onChange={(e) => setDatos({ ...datos, pagoSimulado: e.target.checked })} />
                Confirmo la simulación de pago y envío mi solicitud para revisión.
              </label>
              <p className="small muted">{datos.negocio} · {datos.correo}</p>
            </>
          )}
          {error && <p className="flujo-error" role="alert">{error}</p>}
          <div className="flujo-acciones">
            {paso > 0 && <button type="button" className="btn ghost" disabled={enviando}
              onClick={() => { setError(''); setPaso(paso - 1); }}><ArrowLeft size={16} /> Atrás</button>}
            <button className="btn" type="submit" disabled={enviando}>
              {enviando ? 'Enviando…' : paso === 2 ? 'Simular pago y solicitar tienda' : 'Continuar'}
              <ArrowRight size={16} aria-hidden="true" />
            </button>
          </div>
          <Link to="/admin/login" className="small muted">Ya tengo cuenta</Link>
        </form>
      </div>
    </main>
  );
}
