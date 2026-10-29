import { Link, Navigate, Outlet } from 'react-router-dom';
import { useSesion } from './sesionStore';
import { useClienteStore } from '../tienda/clienteStore';

// Redirige a /admin/login si no hay sesión, igual que la guarda de nav.js.
// `roles` define quién puede entrar a cada bloque de rutas: por defecto el
// personal del panel (ADMIN y VENDEDOR). Las vistas de catálogo pasan
// roles={['ADMIN']}; POS e historial de ventas quedan abiertos a ambos.
// El backend valida lo mismo, así que evitamos llamadas que terminen en 403.
export default function RutaProtegida({ roles = ['ADMIN', 'VENDEDOR'] }) {
  const token = useSesion((estado) => estado.token);
  const rol = useSesion((estado) => estado.rol);
  const usuario = useSesion((estado) => estado.usuario);
  const cliente = useClienteStore((estado) => estado.cliente);

  // Un CLIENTE autenticado nunca entra a una ruta interna, ni escribiendo la
  // URL a mano. Se comprueba antes que el token del panel para que una sesión
  // de comprador no pueda abrir vistas gerenciales aunque quede un token viejo.
  if (cliente?.rol === 'CLIENTE') {
    return <Navigate to="/portal-cliente" replace />;
  }

  if (!token) {
    return <Navigate to="/admin/login" replace />;
  }

  if (rol === 'CLIENTE') {
    return <Navigate to="/portal-cliente" replace />;
  }

  if (roles.includes(rol)) {
    return <Outlet />;
  }

  // El SUPER_ADMIN solo tiene su consola global: si intenta abrir una vista
  // de tienda vuelve a su panel en lugar de ver un error.
  if (rol === 'SUPER_ADMIN' || rol === 'SUPERADMIN') {
    return <Navigate to="/super-admin/dashboard" replace />;
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
