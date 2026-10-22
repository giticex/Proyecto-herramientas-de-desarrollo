import { useEffect } from 'react';
import { useStore } from '../store/store';
import { textoSobre } from '../store/theme';

// Único lugar que aplica el logo y los colores de la marca (antes cada
// página de frontend/ lo hacía por su cuenta, incluso con colores a mano en
// carrito.js). Lee el store y reacciona al instante si la marca cambia.
// navLinks es opcional: los paneles de administrador lo usan para Marca /
// Productos / Salir; el catálogo público de Anelhi no necesita pasarlo.
// sinNav lo usan las vistas con barra lateral, que no llevan cabecera fija.
export default function PlantillaTienda({ children, navLinks, sinNav = false }) {
  const tienda = useStore((estado) => estado.tienda);

  useEffect(() => {
    const raiz = document.documentElement;
    raiz.style.setProperty('--color-primario', tienda.colorPrimario);
    raiz.style.setProperty('--color-secundario', tienda.colorSecundario);
    raiz.style.setProperty('--on-primario', textoSobre(tienda.colorPrimario));
  }, [tienda.colorPrimario, tienda.colorSecundario]);

  return (
    <>
      {!sinNav && (
        <nav className="nav scrolled">
          <span className="nav-brand">
            {tienda.logoUrl && <img src={tienda.logoUrl} alt="" />}
            <span>{tienda.nombre}</span>
          </span>
          {navLinks && <div className="nav-links">{navLinks}</div>}
        </nav>
      )}
      {children}
    </>
  );
}
