import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useSesion } from '../auth/sesionStore';
import { saveTienda, TIENDA_DEFAULT } from '../api/tiendaApi';
import { textoSobre } from '../store/theme';
import { showToast } from '../components/toastBus';
import './MarcaPanel.css';

const URL_HTTP = /^https?:\/\//i;

// Porta frontend/marca.html + frontend/js/marca.js.
export default function MarcaPanel() {
  const tienda = useSesion((estado) => estado.tienda);
  const actualizarTienda = useSesion((estado) => estado.actualizarTienda);
  const token = useSesion((estado) => estado.token);

  const [logoUrl, setLogoUrl] = useState('');
  const [colorPrimario, setColorPrimario] = useState(TIENDA_DEFAULT.colorPrimario);
  const [colorSecundario, setColorSecundario] = useState(TIENDA_DEFAULT.colorSecundario);

  // Cuando la tienda llega del store (cargar()), carga el formulario.
  useEffect(() => {
    if (!tienda) return;
    setLogoUrl(tienda.logoUrl || '');
    setColorPrimario(tienda.colorPrimario || TIENDA_DEFAULT.colorPrimario);
    setColorSecundario(tienda.colorSecundario || TIENDA_DEFAULT.colorSecundario);
  }, [tienda]);

  function restablecer() {
    setLogoUrl('');
    setColorPrimario(TIENDA_DEFAULT.colorPrimario);
    setColorSecundario(TIENDA_DEFAULT.colorSecundario);
  }

  async function manejarSubmit(evento) {
    evento.preventDefault();
    const logo = logoUrl.trim();
    if (logo && !URL_HTTP.test(logo)) {
      showToast('El logo debe empezar con http:// o https://');
      return;
    }
    try {
      const guardada = await saveTienda({ ...tienda, logoUrl: logo, colorPrimario, colorSecundario });
      if (useSesion.getState().token === token) {
        actualizarTienda(guardada);
        showToast('Marca guardada');
      }
    } catch (error) {
      showToast(error.message || 'No se pudo guardar la marca.');
    }
  }

  if (!tienda?.slug) {
    return (
      <main className="page">
        <header className="head"><h1>Marca</h1><p>Inicia sesión nuevamente para identificar tu tienda.</p></header>
        <Link className="btn" to="/admin/login" state={{ from: '/admin/marca' }}>Iniciar sesión</Link>
      </main>
    );
  }

  const logoValido = URL_HTTP.test(logoUrl.trim());

  return (
      <main className="page">
        <header className="head">
          <h1>Tu marca.</h1>
          <p>Logo y colores. La vista previa cambia mientras eliges.</p>
        </header>

        <div className="split section">
          <form className="form" onSubmit={manejarSubmit} noValidate>
            <div className="field">
              <label htmlFor="logoUrl">URL del logo</label>
              <input
                id="logoUrl"
                type="url"
                placeholder="https://..."
                value={logoUrl}
                onChange={(e) => setLogoUrl(e.target.value)}
              />
              <span className="hint">Pega el enlace de una imagen. Déjalo vacío para usar la inicial.</span>
            </div>
            <div className="colors">
              <div className="field">
                <label htmlFor="colorPrimario">Color primario</label>
                <div className="color-row">
                  <input id="colorPrimario" type="color" value={colorPrimario} onChange={(e) => setColorPrimario(e.target.value)} />
                  <code>{colorPrimario}</code>
                </div>
              </div>
              <div className="field">
                <label htmlFor="colorSecundario">Color secundario</label>
                <div className="color-row">
                  <input id="colorSecundario" type="color" value={colorSecundario} onChange={(e) => setColorSecundario(e.target.value)} />
                  <code>{colorSecundario}</code>
                </div>
              </div>
            </div>
            <div className="actions">
              <button className="btn" type="submit">Guardar marca</button>
              <button className="btn ghost" type="button" onClick={restablecer}>Restablecer</button>
            </div>
          </form>

          {/* Mini mockup: los colores se aplican como custom properties SOLO en este contenedor */}
          <div
            className="preview"
            aria-label="Vista previa de la tienda"
            style={{
              '--color-primario': colorPrimario,
              '--color-secundario': colorSecundario,
              '--on-primario': textoSobre(colorPrimario),
            }}
          >
            <div className="pv-bar">
              <span
                className="pv-logo"
                style={logoValido ? { backgroundImage: `url("${encodeURI(logoUrl.trim())}")` } : undefined}
              >
                {!logoValido && (tienda.nombre || '?').charAt(0).toUpperCase()}
              </span>
              <b>{tienda.nombre}</b>
              <i></i><i></i>
            </div>
            <div className="pv-hero">
              <span className="pv-tag">Nueva colección</span>
              <div className="pv-title">Todo lo que buscas, en un solo lugar.</div>
              <span className="pv-btn">Comprar ahora</span>
            </div>
            <div className="pv-grid"><div></div><div></div><div></div></div>
          </div>
        </div>
      </main>
  );
}
