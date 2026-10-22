import { Navigate, Outlet, useParams } from 'react-router-dom';
import { useClienteStore } from './clienteStore';

// Si un comprador intenta entrar por URL al checkout o mis-compras sin sesión,
// lo enviamos al login de esa tienda específica. Con sesión, el rol debe ser
// CLIENTE: una cuenta ADMIN no compra desde la tienda.
export default function RutaProtegidaCliente() {
  const { slug } = useParams();
  const cliente = useClienteStore((estado) => estado.cliente);

  if (!cliente) {
    return <Navigate to={`/tienda/${slug || 'mi-tienda'}/login`} replace />;
  }

  if (cliente.rol !== 'CLIENTE') {
    return <Navigate to={`/tienda/${slug || 'mi-tienda'}`} replace />;
  }

  return <Outlet />;
}
