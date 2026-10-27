import { Link, Navigate, Outlet } from 'react-router-dom';
import { useSesion } from './sesionStore';

// Redirige a /admin/login si no hay sesión, igual que la guarda de nav.js.
// `roles` define quién puede entrar a cada bloque de rutas: por defecto el
// personal del panel (ADMIN y VENDEDOR). Las vistas de catálogo pasan
// roles={['ADMIN']}; POS e historial de ventas quedan abiertos a ambos.
// El backend valida lo mismo, así que evitamos llamadas que terminen en 403.
export default function RutaProtegida({ roles = ['ADMIN', 'VENDEDOR'] }) {
  const token = useSesion((estado) => estado.token);
  const rol = useSesion((estado) => estado.rol);
  const usuario = useSesion((estado) => estado.usuario);

  if (!token) {
    return <Navigate to="/admin/login" replace />;
  }

  if (roles.includes(rol)) {
    return <Outlet />;
  }

  // Un vendedor que intenta abrir una vista solo de ADMIN (dashboard, marca,
  // productos) vuelve a su pantalla de trabajo en lugar de ver un error.
  if (rol === 'VENDEDOR') {
    return <Navigate to="/admin/pos" replace />;
  }

  return (
    <main className="page narrow">
      <header className="head">
        <h1>Sin permisos.</h1>
        <p>
          La cuenta <strong>{usuario}</strong> no tiene un rol habilitado para entrar
          a esta sección del panel.
        </p>
      </header>
      <p className="small muted">
        <Link to="/admin/login">Inicia sesión con otra cuenta</Link>
      </p>
    </main>
  );
}
