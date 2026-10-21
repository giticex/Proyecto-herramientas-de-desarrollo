import { Navigate, Outlet } from 'react-router-dom';
import { useSesion } from './sesionStore';

// Redirige a /admin/login si no hay sesión, igual que la guarda de nav.js.
export default function RutaProtegida() {
  const token = useSesion((estado) => estado.token);
  if (!token) {
    return <Navigate to="/admin/login" replace />;
  }
  return <Outlet />;
}
