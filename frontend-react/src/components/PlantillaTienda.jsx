import { Link } from 'react-router-dom';
import { textoSobre } from '../store/theme';
import { TIENDA_DEFAULT } from '../api/tiendaApi';

export default function PlantillaTienda({ children, tienda = TIENDA_DEFAULT, inicio, navLinks }) {
  const primario = tienda.colorPrimario || TIENDA_DEFAULT.colorPrimario;
  const secundario = tienda.colorSecundario || TIENDA_DEFAULT.colorSecundario;

  return (
    <div className="portal" style={{
      '--color-primario': primario,
      '--color-secundario': secundario,
      '--on-primario': textoSobre(primario),
    }}>
      {inicio && (
        <header className="nav portal-nav">
          <div className="portal-nav-inner">
            <Link className="nav-brand" to={inicio}>
              {tienda.logoUrl && <img src={tienda.logoUrl} alt="" />}
              <span>{tienda.nombre}</span>
            </Link>
            <nav className="nav-links" aria-label="Navegación principal">{navLinks}</nav>
          </div>
        </header>
      )}
      {children}
    </div>
  );
}
