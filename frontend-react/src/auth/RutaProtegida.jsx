import { Navigate, Outlet } from 'react-router-dom';
import { useSesion } from './sesionStore';

// rol: el rol requerido para esta rama de rutas (hoy solo "ADMIN"; las
// rutas de CLIENTE usan RutaProtegidaCliente, que es consciente del slug
// de la tienda para redirigir al login correcto).
export default function RutaProtegida({ rol = 'ADMIN' }) {
  const token = useSesion((estado) => estado.token);
  const usuario = useSesion((estado) => estado.usuario);
  const autorizado = Boolean(token) && usuario?.rol === rol;

  if (!autorizado) {
    return <Navigate to="/admin/login" replace />;
  }
  return <Outlet />;
}
