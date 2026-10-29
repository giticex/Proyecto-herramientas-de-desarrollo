import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSesion } from './sesionStore';
import { useClienteStore } from '../tienda/clienteStore';
import { loginApi } from './authApi';
import { showToast } from '../components/toastBus';
import './Login.css';

function destinoSegunRol(rol) {
  if (rol === 'SUPER_ADMIN' || rol === 'SUPERADMIN') return '/super-admin/dashboard';
  if (rol === 'VENDEDOR') return '/admin/pos';
  if (rol === 'ADMIN') return '/admin';
  if (rol === 'CLIENTE') return null;
  return '/admin';
}

export default function Login({ modo = 'unificado', volverA = '/' } = {}) {
  const [usuario, setUsuario] = useState('');
  const [clave, setClave] = useState('');
  const [errores, setErrores] = useState({ usuario: '', clave: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const loginAdmin = useSesion((estado) => estado.login);
  const loginCliente = useClienteStore((estado) => estado.loginCliente);
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
      const rol = respuesta.rol;

      if (rol === 'CLIENTE') {
        const sesion = {
          idCliente: respuesta.idCliente,
          nombre: respuesta.nombre || u.split('@')[0],
          correo: respuesta.correo || u,
          email: respuesta.correo || u,
          token: respuesta.token,
          rol: respuesta.rol,
        };
        loginCliente(sesion);
        showToast(`Bienvenido/a, ${sesion.nombre}`);
        navigate('/', { replace: true });
        return;
      }

      if (rol === 'VENDEDOR' || rol === 'ADMIN' || rol === 'SUPERADMIN' || rol === 'SUPER_ADMIN') {
        loginAdmin(respuesta.usuario ?? u, respuesta.token, rol);
        navigate(destinoSegunRol(rol), { replace: true });
        return;
      }

      setError('Tu cuenta no tiene permisos para acceder.');
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
        <div className="nav-links">
          <Link to={volverA}>Volver</Link>
          <Link to="/registro-tienda">Crear tienda</Link>
        </div>
      </nav>

      <main className="page narrow">
        <header className="head">
          <h1>Iniciar sesión</h1>
          <p>Accede con tu cuenta para entrar al panel, punto de venta o tienda.</p>
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
