import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import './CatalogoTienda.css';

export default function CatalogoTienda() {
  const { slug } = useParams();
  const { tienda, productos, umbral, getTienda, listProductos } = useStore();
  const { carrito, cliente, agregarAlCarrito } = useClienteStore();

  const [busqueda, setBusqueda] = useState('');
  const [categoriaActiva, setCategoriaActiva] = useState('Todas');
  const [atributosActivos, setAtributosActivos] = useState({});
  const { logoutCliente } = useClienteStore();
  

  useEffect(() => {
    getTienda(slug);
    listProductos(slug);
  }, [slug, getTienda, listProductos]);

  // 1. Extraer categorías únicas desde los productos del store
  const categorias = useMemo(() => {
    const unicas = new Set(productos.map((p) => p.categoria).filter(Boolean));
    return ['Todas', ...Array.from(unicas)];
  }, [productos]);

  // 2. Extraer atributos dinámicos según el rubro y la categoría seleccionada
  const mapaAtributos = useMemo(() => {
    const base =
      categoriaActiva === 'Todas'
        ? productos
        : productos.filter((p) => p.categoria === categoriaActiva);

    const acumulador = {};
    base.forEach((p) => {
      if (p.atributos && typeof p.atributos === 'object') {
        Object.entries(p.atributos).forEach(([clave, valor]) => {
          if (valor !== undefined && valor !== null && String(valor).trim() !== '') {
            if (!acumulador[clave]) acumulador[clave] = new Set();
            acumulador[clave].add(String(valor));
          }
        });
      }
    });

    const resultado = {};
    Object.keys(acumulador).forEach((clave) => {
      resultado[clave] = Array.from(acumulador[clave]);
    });
    return resultado;
  }, [productos, categoriaActiva]);

  // 3. Filtrar productos por búsqueda + categoría + atributos dinámicos
  const productosFiltrados = useMemo(() => {
    return productos.filter((p) => {
      const coincideTexto =
        p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        (p.categoria && p.categoria.toLowerCase().includes(busqueda.toLowerCase()));

      const coincideCategoria =
        categoriaActiva === 'Todas' || p.categoria === categoriaActiva;

      const coincideAtributos = Object.entries(atributosActivos).every(
        ([clave, valorSeleccionado]) => {
          if (!valorSeleccionado) return true;
          return p.atributos && String(p.atributos[clave]) === String(valorSeleccionado);
        }
      );

      return coincideTexto && coincideCategoria && coincideAtributos;
    });
  }, [productos, busqueda, categoriaActiva, atributosActivos]);

  const toggleAtributo = (clave, valor) => {
    setAtributosActivos((prev) => ({
      ...prev,
      [clave]: prev[clave] === valor ? '' : valor,
    }));
  };

  const limpiarFiltros = () => {
    setBusqueda('');
    setCategoriaActiva('Todas');
    setAtributosActivos({});
  };

  const totalItemsCarrito = carrito.reduce((acc, item) => acc + item.cantidad, 0);
  const slugActual = slug || tienda.slug || 'mi-tienda';

  const navLinks = (
    <>
      <Link to="/" style={{ color: '#4a4a4a' }}>Inicio Plataforma</Link>
      <Link to={`/tienda/${slugActual}`} aria-current="page" style={{ color: '#4a4a4a', fontWeight: 700 }}>
        Catálogo
      </Link>
      {cliente && (
        <>
          <Link to={`/tienda/${slugActual}/mis-compras`} style={{ color: '#4a4a4a' }}>
            Mis Compras
          </Link>
          <button
            type="button"
            onClick={() => {
              logoutCliente();
              showToast('Sesión cerrada correctamente');
            }}
            style={{
              background: 'transparent',
              border: '1px solid rgba(255,255,255,0.4)',
              color: '#4a4a4a',
              padding: '6px 12px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 500,
              fontSize: '14px'
            }}
          >
            Cerrar Sesión
          </button>
        </>
      )}
      {!cliente && (
        <Link to={`/tienda/${slugActual}/login`} style={{ color: '#4a4a4a' }}>
          Ingresar
        </Link>
      )}
      <Link
        to={`/tienda/${slugActual}/carrito`}
        className="btn small"
        style={{ background: '#4a4a4a', color: 'var(--color-primario, #0071e3)', fontWeight: 700 }}
      >
        🛒 Carrito
        <span className="nav-badge-carrito">{totalItemsCarrito}</span>
      </Link>
    </>
  );

  return (
    <PlantillaTienda navLinks={navLinks}>
      {/* Hero de la Tienda con los colores dinámicos del Store */}
      <header className="catalogo-hero">
        <div className="catalogo-hero-inner">
          <div>
            <span className="catalogo-rubro-badge">
              Rubro: {tienda.rubro || 'Tienda Oficial'}
            </span>
            <h1>{tienda.nombre}</h1>
            <p>
              Explora nuestro catálogo oficial con stock sincronizado en tiempo real y filtros
              inteligentes.
            </p>
          </div>

          <div className="catalogo-search-box">
            <span>🔍</span>
            <input
              type="search"
              placeholder="Buscar producto o categoría..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </div>
        </div>
      </header>

      {/* Contenedor Principal */}
      <main className="catalogo-container">
        {/* Panel lateral de filtros */}
        <aside className="filtros-panel">
          <div className="filtros-header">
            <h3>Filtros</h3>
            <button type="button" className="btn-limpiar" onClick={limpiarFiltros}>
              Limpiar todo
            </button>
          </div>

          {/* Filtro por Categoría */}
          <div className="filtro-grupo">
            <div className="filtro-titulo">Categorías</div>
            <div className="chips-lista">
              {categorias.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`chip-filtro ${categoriaActiva === cat ? 'activo' : ''}`}
                  onClick={() => {
                    setCategoriaActiva(cat);
                    setAtributosActivos({});
                  }}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* Filtros Dinámicos por Atributos según el Rubro */}
          {Object.entries(mapaAtributos).map(([clave, valores]) => (
            <div key={clave} className="filtro-grupo">
              <div className="filtro-titulo">{clave}</div>
              <div className="chips-lista">
                {valores.map((valor) => (
                  <button
                    key={valor}
                    type="button"
                    className={`chip-filtro ${atributosActivos[clave] === valor ? 'activo' : ''
                      }`}
                    onClick={() => toggleAtributo(clave, valor)}
                  >
                    {valor}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </aside>

        {/* Listado de Productos */}
        <section>
          <div className="productos-toolbar">
            <h2>
              Mostrando {productosFiltrados.length} de {productos.length} productos
            </h2>
          </div>

          {productosFiltrados.length === 0 ? (
            <div
              style={{
                padding: 48,
                textAlign: 'center',
                background: 'var(--bg-soft)',
                borderRadius: 18,
              }}
            >
              <h3>No encontramos productos con esos filtros</h3>
              <p className="muted" style={{ marginTop: 8, marginBottom: 20 }}>
                Prueba limpiando los filtros o buscando otro término.
              </p>
              <button type="button" className="btn" onClick={limpiarFiltros}>
                Ver todo el catálogo
              </button>
            </div>
          ) : (
            <div className="productos-grid">
              {productosFiltrados.map((producto) => {
                const enCarrito =
                  carrito.find((item) => item.idProducto === producto.idProducto)?.cantidad || 0;
                const stockRestante = Math.max(0, Number(producto.stock) - enCarrito);
                const agotado = stockRestante <= 0;
                const stockBajo = !agotado && stockRestante <= umbral;

                return (
                  <article key={producto.idProducto} className="producto-card">
                    <div>
                      <div className="producto-card-top">
                        <span className="producto-cat-badge">
                          {producto.categoria || 'General'}
                        </span>
                        <span
                          className={`stock-pill ${agotado ? 'agotado' : stockBajo ? 'bajo' : 'ok'
                            }`}
                        >
                          {agotado
                            ? 'Agotado'
                            : stockBajo
                              ? `¡Solo quedan ${stockRestante}!`
                              : `Stock: ${stockRestante}`}
                        </span>
                      </div>

                      <div className="producto-visual">
                        {producto.nombre.slice(0, 2).toUpperCase()}
                      </div>

                      <h3 className="producto-nombre">{producto.nombre}</h3>

                      {producto.atributos && Object.keys(producto.atributos).length > 0 && (
                        <div className="atributos-tags">
                          {Object.entries(producto.atributos).map(([k, v]) => (
                            <span key={k} className="atributo-tag">
                              <strong>{k}:</strong> {String(v)}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="producto-footer">
                      <span className="producto-precio">
                        S/ {Number(producto.precio).toFixed(2)}
                      </span>
                      <button
                        type="button"
                        className="btn small"
                        disabled={agotado}
                        style={
                          agotado
                            ? { opacity: 0.5, cursor: 'not-allowed' }
                            : undefined
                        }
                        onClick={() => agregarAlCarrito(producto, 1)}
                      >
                        {agotado ? 'Sin stock' : '+ Agregar'}
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </main>
    </PlantillaTienda>
  );
}