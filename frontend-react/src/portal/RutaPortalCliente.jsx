import { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useClienteStore } from '../tienda/clienteStore';
import { useSesion } from '../auth/sesionStore';

// El portal es exclusivo de CLIENTE: sin sesión de comprador se pide login y
// una cuenta de panel se devuelve a su propio panel.
export default function RutaPortalCliente() {
  const cliente = useClienteStore((estado) => estado.cliente);
  const tokenPanel = useSesion((estado) => estado.token);

  // Si quedó una sesión de panel vieja en el navegador, se limpia para que las
  // llamadas del portal usen el token del cliente y no el del personal.
  useEffect(() => {
    if (cliente?.rol === 'CLIENTE' && tokenPanel) {
      useSesion.getState().logout();
    }
  }, [cliente, tokenPanel]);

  if (cliente?.rol === 'CLIENTE') {
    return <Outlet />;
  }

  if (tokenPanel) {
    return <Navigate to="/admin" replace />;
  }

  return <Navigate to="/login" replace />;
}
