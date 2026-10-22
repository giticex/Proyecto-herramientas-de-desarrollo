import { NavLink, useNavigate } from 'react-router-dom';
import { useSesion } from '../auth/sesionStore';
import { useStore } from '../store/store';
import { TIENDA_DEFAULT } from '../api/tiendaApi';

// Porta los nav-links de marca.html/productos.html: Marca, Productos, Salir.
export default function PanelNavLinks() {
  const logout = useSesion((estado) => estado.logout);
  const setTienda = useStore((estado) => estado.setTienda);
  const navigate = useNavigate();

  function salir(evento) {
    evento.preventDefault();
    logout();
    setTienda({ ...TIENDA_DEFAULT });
    navigate('/admin/login');
  }

  return (
    <>
      <NavLink to="/admin/marca">Marca</NavLink>
      <NavLink to="/admin/productos">Productos</NavLink>
      <a href="#" onClick={salir}>Salir</a>
    </>
  );
}
