import { Navigate, Outlet, useParams } from 'react-router-dom';
import { useClienteStore } from './clienteStore';

export default function RutaProtegidaCliente() {
  const { slug } = useParams();
  const cliente = useClienteStore((estado) => estado.cliente);

  // Si un comprador intenta entrar por URL al checkout o mis-compras sin sesión,
  // lo enviamos al login de esa tienda específica.
  if (!cliente) {
    return <Navigate to={`/tienda/${slug || 'mi-tienda'}/login`} replace />;
  }

  return <Outlet />;
}