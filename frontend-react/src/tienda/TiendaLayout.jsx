import { useEffect } from 'react';
import { Link, NavLink, Outlet, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, ShoppingBag } from 'lucide-react';
import PlantillaTienda from '../components/PlantillaTienda';
import { TIENDA_DEFAULT } from '../api/tiendaApi';
import { carritoDeTienda, useClienteStore } from './clienteStore';
import { rutaTienda } from './rutasTienda';
import { useTiendaPublica } from './useTiendaPublica';

export default function TiendaLayout() {
  const { slug } = useParams();
  return <PortalTienda key={slug} slug={slug} />;
}

function PortalTienda({ slug }) {
  const { pathname, state } = useLocation();
  const datos = useTiendaPublica(slug);
  const items = useClienteStore((estado) => estado.carrito);
  const cliente = useClienteStore((estado) => estado.cliente);
  const adoptarCarrito = useClienteStore((estado) => estado.adoptarCarrito);
  const logoutCliente = useClienteStore((estado) => estado.logoutCliente);
  const carrito = carritoDeTienda(items, slug, datos.tienda ? datos.productos : []);
  const total = carrito.reduce((cantidad, item) => cantidad + item.cantidad, 0);
  const base = rutaTienda(slug);
  const pantalla = pathname.replace(/\/$/, '');
  const checkout = base + '/checkout';
  const volverAlCarrito = pantalla === checkout || (pantalla === base + '/login' && state?.from === checkout);
  const tienda = datos.tienda ?? { ...TIENDA_DEFAULT, nombre: 'Tienda', slug };
  const identificado = cliente?.token && (!cliente.rol || cliente.rol === 'CLIENTE');

  useEffect(() => {
    if (!datos.tienda) return;
    adoptarCarrito(slug, datos.productos);
  }, [adoptarCarrito, datos.productos, datos.tienda, slug]);

  const enlaces = (
    <>
      <NavLink to={base} end>Catálogo</NavLink>
      <NavLink to={base + '/carrito'}><ShoppingBag size={17} aria-hidden="true" /> Carrito <span className="portal-count">{total}</span></NavLink>
      {identificado ? (
        <>
          <NavLink to={base + '/mis-compras'}>Mis compras</NavLink>
          <button type="button" onClick={logoutCliente}>Cerrar sesión</button>
        </>
      ) : <NavLink to={base + '/login'}>Iniciar sesión</NavLink>}
    </>
  );

  return (
    <PlantillaTienda tienda={tienda} inicio={base} navLinks={enlaces}>
      {pantalla !== base && (
        <div className="portal-return">
          <Link to={volverAlCarrito ? base + '/carrito' : base}>
            <ArrowLeft size={17} aria-hidden="true" />
            {volverAlCarrito ? 'Volver al carrito' : 'Volver al catálogo'}
          </Link>
        </div>
      )}
      {datos.errorTienda ? (
        <main className="page portal-notice">
          <h1>{datos.errorTienda.status === 404 ? 'Tienda no encontrada' : 'No se pudo cargar la tienda'}</h1>
          <Link className="btn ghost" to="/">Volver a NovaCommerce</Link>
        </main>
      ) : <Outlet context={{ ...datos, tienda, carrito }} />}
      <footer className="portal-footer"><Link to="/">NovaCommerce</Link></footer>
    </PlantillaTienda>
  );
}
