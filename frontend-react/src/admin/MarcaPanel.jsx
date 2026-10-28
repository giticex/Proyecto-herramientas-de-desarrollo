import { useEffect, useState } from 'react';
import { useStore } from '../store/store';
import { TIENDA_DEFAULT } from '../api/tiendaApi';
import { textoSobre } from '../store/theme';
import { showToast } from '../components/toastBus';
import PanelLayout from './PanelLayout';
import './MarcaPanel.css';

const URL_HTTP = /^https?:\/\//i;

// Porta frontend/marca.html + frontend/js/marca.js.
export default function MarcaPanel() {
  const tienda = useStore((estado) => estado.tienda);
  const getTienda = useStore((estado) => estado.getTienda);
  const saveTienda = useStore((estado) => estado.saveTienda);

  const [logoUrl, setLogoUrl] = useState('');
  const [colorPrimario, setColorPrimario] = useState(TIENDA_DEFAULT.colorPrimario);
  const [colorSecundario, setColorSecundario] = useState(TIENDA_DEFAULT.colorSecundario);
  const [guardando, setGuardando] = useState(false);

  // La marca real vive en el backend: al entrar directo al panel hay que
  // traerla, porque el store arranca con los valores por defecto.
  useEffect(() => {
    getTienda().catch(() => {});
  }, [getTienda]);

  // Cuando la tienda llega del store (cargar()), carga el formulario.
  useEffect(() => {
    setLogoUrl(tienda.logoUrl || '');
    setColorPrimario(tienda.colorPrimario);
    setColorSecundario(tienda.colorSecundario);
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
    setGuardando(true);
    try {
      await saveTienda({ ...tienda, logoUrl: logo, colorPrimario, colorSecundario });
      showToast('Marca guardada');
    } catch (error) {
      showToast(error?.message || 'No se pudo guardar la marca.');
    } finally {
      setGuardando(false);
    }
  }

  const logoValido = URL_HTTP.test(logoUrl.trim());

  return (
    <PanelLayout>
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
              <button className="btn" type="submit" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar marca'}
              </button>
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
    </PanelLayout>
  );
}
