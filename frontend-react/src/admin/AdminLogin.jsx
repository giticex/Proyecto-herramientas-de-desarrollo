import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSesion } from '../auth/sesionStore';
import { loginApi } from '../auth/authApi';
import './AdminLogin.css';

// Puerto de frontend/login.html + frontend/js/login.js: mismos campos,
// validación y mensajes en español. El login es real (POST /auth/login) y
// deja entrar a ADMIN y VENDEDOR; cada rol aterriza en su vista inicial.
const ROLES_PANEL = ['ADMIN', 'VENDEDOR'];
export default function AdminLogin() {
  const [usuario, setUsuario] = useState('');
  const [clave, setClave] = useState('');
  const [errores, setErrores] = useState({ usuario: '', clave: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const login = useSesion((estado) => estado.login);
  const navigate = useNavigate();

  function limpiarError(campo) {
    setErrores((prev) => ({ ...prev, [campo]: '' }));
    setError('');
  }

  async function manejarSubmit(evento) {
    evento.preventDefault();
    const u = usuario.trim();
    const errorUsuario = u ? '' : 'Ingresa tu usuario o correo.';
    const errorClave = clave.length >= 1 ? '' : 'Ingresa tu contraseña.';
    setErrores({ usuario: errorUsuario, clave: errorClave });
    if (errorUsuario || errorClave) return;

    setEnviando(true);
    setError('');
    try {
      const respuesta = await loginApi(u, clave);
      if (!ROLES_PANEL.includes(respuesta.rol)) {
        setError('Tu cuenta no tiene permisos para entrar al panel.');
        return;
      }
      login(respuesta.usuario ?? u, respuesta.token, respuesta.rol);
      navigate(respuesta.rol === 'VENDEDOR' ? '/admin/pos' : '/admin');
    } catch (problema) {
      setError(problema.message || 'No se pudo iniciar sesión.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <nav className="nav">
        <Link className="nav-brand" to="/">NovaCommerce</Link>
        <div className="nav-links"><Link to="/registro-tienda">Crear tienda</Link></div>
      </nav>

      <main className="page narrow">
        <header className="head">
          <h1>Bienvenido.</h1>
          <p>Inicia sesión para gestionar tu tienda.</p>
        </header>

        <form className="form" onSubmit={manejarSubmit} noValidate>
          <div className={`field ${errores.usuario ? 'invalid' : ''}`}>
            <label htmlFor="usuario">Usuario o correo</label>
            <input
              id="usuario"
              type="text"
              autoComplete="username"
              placeholder="tu@correo.com"
              value={usuario}
              onChange={(e) => { setUsuario(e.target.value); limpiarError('usuario'); }}
            />
            <span className="error" role="alert">{errores.usuario}</span>
          </div>
          <div className={`field ${errores.clave ? 'invalid' : ''}`}>
            <label htmlFor="clave">Contraseña</label>
            <input
              id="clave"
              type="password"
              autoComplete="current-password"
              value={clave}
              onChange={(e) => { setClave(e.target.value); limpiarError('clave'); }}
            />
            <span className="error" role="alert">{errores.clave}</span>
          </div>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="btn block" type="submit" disabled={enviando}>Entrar</button>
          <p className="small muted">¿Sin tienda todavía? <Link to="/registro-tienda">Crea una</Link></p>
        </form>
      </main>
    </>
  );
}
