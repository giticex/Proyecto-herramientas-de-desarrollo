import { Link, Navigate, Outlet } from 'react-router-dom';
import { useSesion } from './sesionStore';

// Redirige a /admin/login si no hay sesión, igual que la guarda de nav.js.
// Con sesión pero sin rol ADMIN no entra al panel: el backend también lo
// rechaza con 403, así que conviene avisarlo antes de llamar a la API.
export default function RutaProtegida() {
  const token = useSesion((estado) => estado.token);
  const rol = useSesion((estado) => estado.rol);
  const usuario = useSesion((estado) => estado.usuario);

  if (!token) {
    return <Navigate to="/admin/login" replace />;
  }

  if (rol !== 'ADMIN') {
    return (
      <main className="page narrow">
        <header className="head">
          <h1>Sin permisos.</h1>
          <p>
            La cuenta <strong>{usuario}</strong> no tiene el rol ADMIN para entrar al
            panel de administración.
          </p>
        </header>
        <p className="small muted">
          <Link to="/admin/login">Inicia sesión con una cuenta administradora</Link>
        </p>
      </main>
    );
  }

  return <Outlet />;
}
