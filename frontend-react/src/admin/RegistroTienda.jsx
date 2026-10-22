import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useStore } from '../store/store';
import { slugify } from '../store/slugify';
import { useSesion } from '../auth/sesionStore';
import { registrarTienda } from '../auth/authApi';
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

// Porta frontend/registro.html + frontend/js/registro.js. No requiere sesión.
export default function RegistroTienda() {
  const [nombre, setNombre] = useState('');
  const [rubro, setRubro] = useState('');
  const [slug, setSlug] = useState('');
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [slugEditado, setSlugEditado] = useState(false);
  const [errores, setErrores] = useState({ nombre: '', rubro: '', slug: '', correo: '', contrasena: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const setTienda = useStore((estado) => estado.setTienda);
  const login = useSesion((estado) => estado.login);
  const navigate = useNavigate();

  function limpiarError(campo) {
    setErrores((prev) => ({ ...prev, [campo]: '' }));
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
    const errorCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim()) ? '' : 'Escribe un correo válido.';
    const errorContrasena = contrasena.length >= 8 ? '' : 'Usa al menos 8 caracteres.';
    setErrores({ nombre: errorNombre, rubro: errorRubro, slug: errorSlug, correo: errorCorreo, contrasena: errorContrasena });
    if (errorNombre || errorRubro || errorSlug || errorCorreo || errorContrasena) return;

    setEnviando(true);
    setError('');
    try {
      const respuesta = await registrarTienda({
        nombre: nombre.trim(),
        rubro,
        slug: s,
        correo: correo.trim(),
        contrasena,
      });
      setTienda(respuesta.tienda);
      localStorage.removeItem('productos'); // tienda nueva = catálogo nuevo (mock)
      login(respuesta.usuario ?? correo.trim(), respuesta.token, respuesta.rol);
      navigate('/admin/marca');
    } catch (problema) {
      setError(problema.message || 'No se pudo crear la tienda.');
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
          <p>Tres datos y estás dentro. Podrás cambiarlos después.</p>
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
            <label htmlFor="correo">Correo de administrador</label>
            <input
              id="correo"
              type="email"
              autoComplete="email"
              placeholder="tu@correo.com"
              value={correo}
              onChange={(e) => { setCorreo(e.target.value); limpiarError('correo'); setError(''); }}
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
              onChange={(e) => { setContrasena(e.target.value); limpiarError('contrasena'); setError(''); }}
            />
            <span className="error" role="alert">{errores.contrasena}</span>
          </div>

          {error && <p className="error" role="alert">{error}</p>}

          <button className="btn block" type="submit" disabled={enviando}>Crear tienda</button>
        </form>
      </main>
    </>
  );
}
