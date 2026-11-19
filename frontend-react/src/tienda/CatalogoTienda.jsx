import { useMemo, useState } from 'react';
import { useOutletContext, useParams } from 'react-router-dom';
import { coincidenciaMarcaYPrecio, errorRangoPrecio, esAtributoMarca, opcionesDeMarca } from './filtrosCatalogo';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import { coincidenciaBusquedaCatalogo } from './busquedaCatalogo';
import './CatalogoTienda.css';

export default function CatalogoTienda() {
  const { slug } = useParams();
  const { tienda, productos, carrito, catalogoListo, errorCatalogo } = useOutletContext();
  const umbral = useStore((estado) => estado.umbral);
  const agregarAlCarrito = useClienteStore((estado) => estado.agregarAlCarrito);

  const [busqueda, setBusqueda] = useState('');
  const [categoriaActiva, setCategoriaActiva] = useState('');
  const [atributosActivos, setAtributosActivos] = useState({});
  const [marcaActiva, setMarcaActiva] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [filtrosAbiertos, setFiltrosAbiertos] = useState(false);
  const errorPrecio = errorRangoPrecio(desde, hasta);

  // 1. Extraer categorías únicas desde los productos del store
  const categorias = useMemo(() => {
    const unicas = new Set(productos.map((p) => p.categoria).filter(Boolean));
    return ['', ...unicas];
  }, [productos]);

  const { marcas, mapaAtributos } = useMemo(() => {
    const base = categoriaActiva
      ? productos.filter((p) => p.categoria === categoriaActiva)
      : productos;
    const atributos = new Map();
    for (const producto of base) {
      for (const [clave, valor] of Object.entries(producto.atributos ?? {})) {
        if (esAtributoMarca(clave) || !String(valor ?? '').trim()) continue;
        if (!atributos.has(clave)) atributos.set(clave, new Set());
        atributos.get(clave).add(String(valor));
      }
    }
    return { marcas: opcionesDeMarca(base), mapaAtributos: Array.from(atributos) };
  }, [productos, categoriaActiva]);

  // 3. Filtrar productos por búsqueda + categoría + atributos dinámicos
  const productosFiltrados = useMemo(() => {
    const coincideBusqueda = coincidenciaBusquedaCatalogo(productos, busqueda);
    return productos.filter((p) => {
      const coincideTexto = coincideBusqueda(p);

      const coincideCategoria =
        !categoriaActiva || p.categoria === categoriaActiva;

      const coincideAtributos = Object.entries(atributosActivos).every(
        ([clave, valorSeleccionado]) => {
          if (!valorSeleccionado) return true;
          return p.atributos && String(p.atributos[clave]) === String(valorSeleccionado);
        }
      );

      return coincideTexto && coincideCategoria && coincideAtributos;
    });
  }, [productos, busqueda, categoriaActiva, atributosActivos]);

  const resultados = useMemo(() => {
    const coincide = coincidenciaMarcaYPrecio({ marca: marcaActiva, desde, hasta });
    return productosFiltrados.filter(coincide);
  }, [productosFiltrados, marcaActiva, desde, hasta]);

  const toggleAtributo = (clave, valor) => {
    setAtributosActivos((prev) => ({
      ...prev,
      [clave]: prev[clave] === valor ? '' : valor,
    }));
  };

  const limpiarFiltros = () => {
    setBusqueda('');
    setCategoriaActiva('');
    setAtributosActivos({});
    setMarcaActiva('');
    setDesde('');
    setHasta('');
  };

  return (
    <>
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
              placeholder="Buscar por producto, categoría o SKU..."
              aria-label="Buscar por producto, categoría o SKU"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </div>
        </div>
      </header>

      {/* Contenedor Principal */}
      <main className="catalogo-container">
        {/* Panel lateral de filtros */}
        <aside className="filtros-panel" aria-label="Filtros de productos">
          <button
            type="button"
            className="filtros-toggle"
            aria-expanded={filtrosAbiertos}
            aria-controls="catalogo-filtros"
            onClick={() => setFiltrosAbiertos((abiertos) => !abiertos)}
          >
            {filtrosAbiertos ? 'Ocultar filtros' : 'Mostrar filtros'}
          </button>
          <div id="catalogo-filtros" className={`filtros-contenido${filtrosAbiertos ? ' abierto' : ''}`}>
            <div className="filtros-header">
              <h2>Filtros</h2>
              <button type="button" className="btn-limpiar" onClick={limpiarFiltros}>
                Limpiar todo
              </button>
            </div>

            <fieldset className="filtro-grupo">
              <legend className="filtro-titulo">Categorías</legend>
              <div className="chips-lista">
                {categorias.map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    className={`chip-filtro ${categoriaActiva === cat ? 'activo' : ''}`}
                    aria-pressed={categoriaActiva === cat}
                    onClick={() => {
                      if (categoriaActiva === cat) return;
                      setCategoriaActiva(cat);
                      setAtributosActivos({});
                      setMarcaActiva('');
                    }}
                  >
                    {cat || 'Todas'}
                  </button>
                ))}
              </div>
            </fieldset>

            <fieldset className="filtro-grupo filtro-precio">
              <legend className="filtro-titulo">Precio (S/)</legend>
              <div className="filtro-precio-campos">
                <label>
                  Desde
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={desde}
                    aria-invalid={Boolean(errorPrecio)}
                    aria-describedby={errorPrecio ? 'catalogo-precio-error' : undefined}
                    onChange={(e) => setDesde(e.target.value)}
                  />
                </label>
                <label>
                  Hasta
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={hasta}
                    aria-invalid={Boolean(errorPrecio)}
                    aria-describedby={errorPrecio ? 'catalogo-precio-error' : undefined}
                    onChange={(e) => setHasta(e.target.value)}
                  />
                </label>
              </div>
              {errorPrecio && <p id="catalogo-precio-error" role="alert">{errorPrecio}</p>}
            </fieldset>

            <label className="filtro-grupo filtro-marca">
              <span className="filtro-titulo">Marca</span>
              <select value={marcaActiva} onChange={(e) => setMarcaActiva(e.target.value)} disabled={!marcas.length}>
                <option value="">{marcas.length ? 'Todas las marcas' : 'No hay marcas disponibles'}</option>
                {marcas.map(({ valor, etiqueta }) => <option key={valor} value={valor}>{etiqueta}</option>)}
              </select>
            </label>

            {mapaAtributos.map(([clave, valores]) => (
              <fieldset key={clave} className="filtro-grupo">
                <legend className="filtro-titulo">{clave}</legend>
                <div className="chips-lista">
                  {Array.from(valores, (valor) => (
                    <button
                      key={valor}
                      type="button"
                      className={`chip-filtro ${atributosActivos[clave] === valor ? 'activo' : ''}`}
                      aria-pressed={atributosActivos[clave] === valor}
                      onClick={() => toggleAtributo(clave, valor)}
                    >
                      {valor}
                    </button>
                  ))}
                </div>
              </fieldset>
            ))}
          </div>
        </aside>

        {/* Listado de Productos */}
        <section>
          <div className="productos-toolbar">
            <h2 aria-live="polite">
              Mostrando {resultados.length} de {productos.length} productos
            </h2>
          </div>

          {errorCatalogo ? (
            <p className="portal-status" role="alert">No se pudo cargar el catálogo. Recarga la página.</p>
          ) : !catalogoListo ? (
            <p className="portal-status" role="status">Cargando catálogo…</p>
          ) : resultados.length === 0 ? (
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
              {resultados.map((producto) => {
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
                      {producto.sku && <p className="small muted">SKU: {producto.sku}</p>}

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
                        onClick={() => agregarAlCarrito(slug, producto, 1)}
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
    </>
  );
}
