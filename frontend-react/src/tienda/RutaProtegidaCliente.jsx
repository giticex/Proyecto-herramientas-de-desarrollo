import { Navigate, Outlet, useParams, useLocation } from 'react-router-dom';
import { useClienteStore } from './clienteStore';

// Si un comprador intenta entrar por URL al checkout o mis-compras sin sesión,
// lo enviamos al login de esa tienda específica. Con sesión, el rol debe ser
// CLIENTE: una cuenta ADMIN no compra desde la tienda. Las sesiones guardadas
// antes del login con roles no traen `rol`; se tratan como CLIENTE para no
// expulsarlas del checkout.
export default function RutaProtegidaCliente() {
  const { slug } = useParams();
  const location = useLocation();
  const cliente = useClienteStore((estado) => estado.cliente);

  if (!cliente) {
    // Guarda la ruta pedida para volver a ella después de iniciar sesión.
    return (
      <Navigate
        to={`/tienda/${slug || 'mi-tienda'}/login`}
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  if (cliente.rol && cliente.rol !== 'CLIENTE') {
    return <Navigate to={`/tienda/${slug || 'mi-tienda'}`} replace />;
  }

  return <Outlet />;
}
