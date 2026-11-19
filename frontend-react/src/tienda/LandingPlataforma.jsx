import { Link } from 'react-router-dom';
import { TIENDA_DEFAULT } from '../api/tiendaApi';
import { useTiendaPublica } from './useTiendaPublica';
import './LandingPlataforma.css';

export default function LandingPlataforma() {
  const slugTienda = TIENDA_DEFAULT.slug;
  const { tienda: marca, productos, catalogoListo, errorCatalogo } = useTiendaPublica(slugTienda);
  const tienda = marca || TIENDA_DEFAULT;
  const muestraProductos = productos.slice(0, 4);

  return (
    <div className="landing-wrapper">
      {/* Barra de Navegación */}
      <header className="landing-nav">
        <Link to="/" className="landing-logo">
          <span className="landing-logo-badge">N</span>
          <span>NovaCommerce Cloud</span>
        </Link>

        <div className="landing-nav-actions">
          <Link to="/demo" className="btn-tech glass">
            Probar la demo
          </Link>
          <Link to="/registro-tienda" className="btn-tech glass">
            Crear mi tienda
          </Link>
          <Link to="/admin/login" className="btn-tech primary">
            Iniciar sesión
          </Link>
        </div>
      </header>

      {/* Hero Principal */}
      <section className="landing-hero">
        <div className="hero-glow" />

        <div className="hero-content">
          <div className="hero-pill">
            <span className="hero-pill-dot" />
            <span>Arquitectura Multi-Tienda + Facturación Electrónica</span>
          </div>

          <h1 className="hero-title">
            Tu comercio digital con <span>identidad propia</span> y stock en tiempo real.
          </h1>

          <p className="hero-subtitle">
            Personaliza el ADN visual de tu marca, filtra catálogos con atributos dinámicos
            según tu rubro y emite boletas o facturas al instante con validación automática de inventario.
          </p>

          <div className="hero-cta-group">
            <Link to="/demo" className="btn-tech primary">
              Probar la demo →
            </Link>
            <Link to="/registro-tienda" className="btn-tech glass">
              Solicitar mi tienda
            </Link>
          </div>

          <div className="hero-stats">
            <div className="stat-item">
              <strong>Activación manual</strong>
              <span>Solicita tu tienda en línea</span>
            </div>
            <div className="stat-item">
              <strong>DNI / RUC</strong>
              <span>Boleta y Factura integrada</span>
            </div>
            <div className="stat-item">
              <strong>Tiempo Real</strong>
              <span>Sincronización de Stock</span>
            </div>
          </div>
        </div>

        {/* Vista previa interactiva conectada al Store de Kiara */}
        <div className="hero-showcase">
          <div className="showcase-header">
            <div className="showcase-dots">
              <i style={{ background: '#ff5f56' }} />
              <i style={{ background: '#ffbd2e' }} />
              <i style={{ background: '#27c93f' }} />
            </div>
            <div className="showcase-store-pill">
              <span
                className="showcase-store-dot"
                style={{ background: tienda.colorPrimario || '#0071e3' }}
              />
              <span>Vista en vivo: {tienda.nombre}</span>
            </div>
          </div>

          <div className="showcase-body">
            <div
              className="showcase-banner"
              style={{
                background: `linear-gradient(135deg, ${tienda.colorPrimario || '#0071e3'}, ${
                  tienda.colorSecundario || '#1d1d1f'
                })`,
              }}
            >
              <div>
                <div style={{ fontSize: 12, opacity: 0.85, textTransform: 'uppercase' }}>
                  Rubro: {tienda.rubro || 'General'}
                </div>
                <div style={{ fontSize: 20, fontWeight: 800, marginTop: 4 }}>
                  {tienda.nombre} Oficial
                </div>
              </div>
              <Link
                to={`/tienda/${slugTienda}`}
                style={{
                  background: 'rgba(255,255,255,0.2)',
                  color: '#fff',
                  padding: '8px 14px',
                  borderRadius: 999,
                  fontSize: 12,
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                Abrir catálogo
              </Link>
            </div>

            <div className="showcase-mini-grid">
              {muestraProductos.length > 0 ? (
                muestraProductos.map((prod) => (
                  <div key={prod.idProducto} className="showcase-card">
                    <div className="showcase-card-cat">{prod.categoria || 'Destacado'}</div>
                    <div className="showcase-card-title">{prod.nombre}</div>
                    <div className="showcase-card-foot">
                      <strong>S/ {Number(prod.precio).toFixed(2)}</strong>
                      <span style={{ color: prod.stock > 0 ? '#4ade80' : '#f87171', fontSize: 12 }}>
                        {prod.stock > 0 ? `Stock: ${prod.stock}` : 'Agotado'}
                      </span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="showcase-card" style={{ gridColumn: 'span 2', textAlign: 'center' }}>
                  {errorCatalogo ? 'Catálogo de demostración no disponible.' : catalogoListo ? 'Aún no hay productos publicados.' : 'Cargando productos…'}
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Bento Grid de Arquitectura */}
      <section className="bento-section">
        <div className="bento-head">
          <h2>Diseñado para escalar cualquier rubro</h2>
          <p>
            Desde moda y calzado hasta electrónica de alto rendimiento. Todo conectado al mismo
            núcleo de datos reactivo.
          </p>
        </div>

        <div className="bento-grid">
          <div className="bento-card wide">
            <div className="bento-icon">🎨</div>
            <h3>Diseña tu tienda</h3>
            <p>Cambia el nombre y el color de la tienda de ejemplo. Mira cómo se ve el resultado en el catálogo.</p>
            <Link className="btn-tech glass" to="/demo?modulo=marca">Probar el diseño</Link>
          </div>

          <div className="bento-card">
            <div className="bento-icon">⚡</div>
            <h3>Explora el catálogo</h3>
            <p>Abre la vista del comprador y elige un producto para simular una compra.</p>
            <Link className="btn-tech glass" to="/demo?modulo=catalogo">Ver catálogo demo</Link>
          </div>

          <div className="bento-card">
            <div className="bento-icon">🛡️</div>
            <h3>Gestiona el inventario</h3>
            <p>Agrega un producto de prueba, fija su precio y comprueba cómo se descuenta su stock cuando se vende.</p>
            <Link className="btn-tech glass" to="/demo?modulo=productos">Probar inventario</Link>
          </div>

          <div className="bento-card wide">
            <div className="bento-icon">🧾</div>
            <h3>Registra una venta</h3>
            <p>Elige una boleta o factura de demostración y revisa el comprobante de la compra simulada.</p>
            <Link className="btn-tech glass" to="/demo?modulo=venta">Ver flujo de venta</Link>
          </div>
        </div>
      </section>

      {/* Call to Action Final */}
      <footer className="landing-footer">
        <div className="footer-cta-box">
          <h2>Experimenta la tienda en vivo ahora mismo</h2>
          <p>
            Prueba el diseño, el catálogo y una compra simulada antes de solicitar tu tienda.
          </p>
          <div style={{ display: 'flex', gap: 14, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/demo" className="btn-tech primary">
              Abrir la demo
            </Link>
            <Link to="/registro-tienda" className="btn-tech glass">
              Solicitar mi tienda
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}