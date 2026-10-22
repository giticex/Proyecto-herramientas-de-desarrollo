import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { slugify } from '../store/slugify';
import { useSesion } from '../auth/sesionStore';
import { useStore } from '../store/store';
import { registroTienda as registroTiendaApi } from '../auth/authApi';
import { ApiError } from '../api/ApiError';
import './RegistroTienda.css';

const RUBROS = [
  { value: '', label: 'Selecciona un rubro' },
  { value: 'moda', label: 'Moda' },
  { value: 'comida', label: 'Comida' },
  { value: 'tecnologia', label: 'Tecnología' },
  { value: 'salud', label: 'Salud y belleza' },
  { value: 'hogar', label: 'Hogar y decoración' },
  { value: 'deportes', label: 'Deportes' },
  { value: 'juguetes', label: 'Juguetes' },
  { value: 'libros', label: 'Libros y papelería' },
  { value: 'otro', label: 'Otro' },
];

const ERRORES_VACIOS = { nombre: '', rubro: '', slug: '', correo: '', contrasena: '', confirmar: '' };

// Porta frontend/registro.html + frontend/js/registro.js. No requiere sesión.
export default function RegistroTienda() {
  const [nombre, setNombre] = useState('');
  const [rubro, setRubro] = useState('');
  const [slug, setSlug] = useState('');
  const [slugEditado, setSlugEditado] = useState(false);
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [errores, setErrores] = useState(ERRORES_VACIOS);
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);
  const login = useSesion((estado) => estado.login);
  const setTienda = useStore((estado) => estado.setTienda);
  const navigate = useNavigate();

  function limpiarError(campo) {
    setErrores((prev) => ({ ...prev, [campo]: '' }));
    setErrorGeneral('');
  }

  function manejarNombre(valor) {
    setNombre(valor);
    limpiarError('nombre');
    if (!slugEditado) setSlug(slugify(valor));
  }

  function manejarSlug(valor) {
    setSlug(valor);
    setSlugEditado(true);
    limpiarError('slug');
  }

  async function manejarSubmit(evento) {
    evento.preventDefault();
    const s = slugify(slug);
    const errorNombre = nombre.trim().length >= 2 ? '' : 'Escribe el nombre de tu tienda.';
    const errorRubro = rubro ? '' : 'Elige un rubro.';
    const errorSlug = s ? '' : 'La dirección no puede estar vacía.';
    const errorCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo) ? '' : 'Ingresa un correo válido.';
    const errorContrasena = contrasena.length >= 8 ? '' : 'Debe tener al menos 8 caracteres.';
    const errorConfirmar = confirmar === contrasena ? '' : 'Las contraseñas no coinciden.';
    setErrores({
      nombre: errorNombre,
      rubro: errorRubro,
      slug: errorSlug,
      correo: errorCorreo,
      contrasena: errorContrasena,
      confirmar: errorConfirmar,
    });
    setErrorGeneral('');
    if (errorNombre || errorRubro || errorSlug || errorCorreo || errorContrasena || errorConfirmar) return;

    setEnviando(true);
    try {
      const respuesta = await registroTiendaApi({
        nombre: nombre.trim(),
        rubro,
        slug: s,
        correo: correo.trim(),
        contrasena,
      });
      login(respuesta.usuario, respuesta.token);
      if (respuesta.tienda) setTienda(respuesta.tienda);
      navigate('/admin');
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setErrorGeneral('Ese correo o dirección ya está en uso.');
      } else {
        setErrorGeneral(error instanceof ApiError ? error.message : 'No se pudo crear la tienda.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <nav className="nav">
        <Link className="nav-brand" to="/">NovaCommerce</Link>
        <div className="nav-links"><Link to="/admin/login">Iniciar sesión</Link></div>
      </nav>

      <main className="page narrow">
        <header className="head">
          <h1>Abre tu tienda.</h1>
          <p>Unos datos y estás dentro. Podrás cambiarlos después.</p>
        </header>

        <form className="form" onSubmit={manejarSubmit} noValidate>
          <div className={`field ${errores.nombre ? 'invalid' : ''}`}>
            <label htmlFor="nombre">Nombre de la tienda</label>
            <input
              id="nombre"
              type="text"
              placeholder="Mi Tienda"
              autoComplete="organization"
              value={nombre}
              onChange={(e) => manejarNombre(e.target.value)}
            />
            <span className="error" role="alert">{errores.nombre}</span>
          </div>

          <div className={`field ${errores.rubro ? 'invalid' : ''}`}>
            <label htmlFor="rubro">Rubro</label>
            <select
              id="rubro"
              value={rubro}
              onChange={(e) => { setRubro(e.target.value); limpiarError('rubro'); }}
            >
              {RUBROS.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
            <span className="error" role="alert">{errores.rubro}</span>
          </div>

          <div className={`field ${errores.slug ? 'invalid' : ''}`}>
            <label htmlFor="slug">Dirección de tu tienda</label>
            <div className="slug-row">
              <span className="muted">tienda.com/</span>
              <input
                id="slug"
                type="text"
                placeholder="mi-tienda"
                autoCapitalize="off"
                value={slug}
                onChange={(e) => manejarSlug(e.target.value)}
                onBlur={(e) => setSlug(slugify(e.target.value))}
              />
            </div>
            <span className="hint">Se genera desde el nombre. Puedes editarla.</span>
            <span className="error" role="alert">{errores.slug}</span>
          </div>

          <div className={`field ${errores.correo ? 'invalid' : ''}`}>
            <label htmlFor="correo">Correo</label>
            <input
              id="correo"
              type="email"
              placeholder="tu@correo.com"
              autoComplete="email"
              value={correo}
              onChange={(e) => { setCorreo(e.target.value); limpiarError('correo'); }}
            />
            <span className="error" role="alert">{errores.correo}</span>
          </div>

          <div className={`field ${errores.contrasena ? 'invalid' : ''}`}>
            <label htmlFor="contrasena">Contraseña</label>
            <input
              id="contrasena"
              type="password"
              autoComplete="new-password"
              value={contrasena}
              onChange={(e) => { setContrasena(e.target.value); limpiarError('contrasena'); }}
            />
            <span className="hint">Mínimo 8 caracteres.</span>
            <span className="error" role="alert">{errores.contrasena}</span>
          </div>

          <div className={`field ${errores.confirmar ? 'invalid' : ''}`}>
            <label htmlFor="confirmar">Confirmar contraseña</label>
            <input
              id="confirmar"
              type="password"
              autoComplete="new-password"
              value={confirmar}
              onChange={(e) => { setConfirmar(e.target.value); limpiarError('confirmar'); }}
            />
            <span className="error" role="alert">{errores.confirmar}</span>
          </div>

          {errorGeneral && <p className="error" role="alert">{errorGeneral}</p>}
          <button className="btn block" type="submit" disabled={enviando}>Crear tienda</button>
        </form>
      </main>
    </>
  );
}
