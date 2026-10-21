import { useEffect } from 'react';
import { useStore } from '../store/store';

// Misma fórmula de textoSobre() de frontend/js/store.js: eligen texto claro u
// oscuro según la luminancia del color primario.
function textoSobre(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = n >> 16;
  const g = (n >> 8) & 255;
  const b = n & 255;
  return 0.299 * r + 0.587 * g + 0.114 * b > 160 ? '#1d1d1f' : '#ffffff';
}

// Único lugar que aplica el logo y los colores de la marca (antes cada
// página de frontend/ lo hacía por su cuenta, incluso con colores a mano en
// carrito.js). Lee el store y reacciona al instante si la marca cambia.
export default function PlantillaTienda({ children }) {
  const tienda = useStore((estado) => estado.tienda);

  useEffect(() => {
    const raiz = document.documentElement;
    raiz.style.setProperty('--color-primario', tienda.colorPrimario);
    raiz.style.setProperty('--color-secundario', tienda.colorSecundario);
    raiz.style.setProperty('--on-primario', textoSobre(tienda.colorPrimario));
  }, [tienda.colorPrimario, tienda.colorSecundario]);

  return (
    <>
      <nav className="nav scrolled">
        <span className="nav-brand">
          {tienda.logoUrl && <img src={tienda.logoUrl} alt="" />}
          <span>{tienda.nombre}</span>
        </span>
      </nav>
      {children}
    </>
  );
}
