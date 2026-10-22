import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { useClienteStore } from './clienteStore';

export default function RutaProtegidaCliente() {
  const { slug } = useParams();
  const location = useLocation();
  const cliente = useClienteStore((estado) => estado.cliente);

  // Si un comprador intenta entrar por URL al checkout o mis-compras sin
  // sesión, lo enviamos al login de esa tienda específica y guardamos a
  // dónde iba para volver ahí después de loguearse.
  if (!cliente?.token) {
    return <Navigate to={`/tienda/${slug || 'mi-tienda'}/login`} replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
