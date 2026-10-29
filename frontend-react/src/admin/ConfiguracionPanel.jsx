import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, RefreshCw, Save, ShieldCheck, Undo2 } from 'lucide-react';
import { guardarConfiguracion, obtenerConfiguracion } from '../api/configuracionApi';
import { showToast } from '../components/toastBus';
import PanelLayout from './PanelLayout';
import './HistorialVentas.css';
import './ConfiguracionPanel.css';

const CAMPOS = [
  'razonSocial',
  'ruc',
  'direccionFiscal',
  'telefono',
  'logoUrl',
  'serieBoleta',
  'serieFactura',
];

const FORM_VACIO = {
  razonSocial: '',
  ruc: '',
  direccionFiscal: '',
  telefono: '',
  logoUrl: '',
  serieBoleta: '',
  serieFactura: '',
};

const REGEX_RUC = /^(10|15|17|20)\d{9}$/;
const REGEX_TELEFONO = /^[0-9+()\-\s]{6,30}$/;
const REGEX_LOGO = /^https?:\/\/\S+$/i;
const REGEX_SERIE_BOLETA = /^B[A-Z0-9]{3}$/;
const REGEX_SERIE_FACTURA = /^F[A-Z0-9]{3}$/;

function aFormulario(configuracion) {
  const formulario = { ...FORM_VACIO };
  CAMPOS.forEach((campo) => {
    formulario[campo] = configuracion?.[campo] ?? '';
  });
  return formulario;
}

// El backend vuelve a validar todo: aquí solo se atajan los errores evidentes
// para no mandar una petición que se va a rechazar.
function validar(formulario) {
  const razonSocial = formulario.razonSocial.trim();
  if (!razonSocial) return 'La razón social es obligatoria.';
  if (razonSocial.length > 150) return 'La razón social no puede superar los 150 caracteres.';

  const ruc = formulario.ruc.trim();
  if (!ruc) return 'El RUC es obligatorio.';
  if (!REGEX_RUC.test(ruc)) {
    return 'El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20.';
  }

  const direccion = formulario.direccionFiscal.trim();
  if (!direccion) return 'La dirección fiscal es obligatoria.';
  if (direccion.length > 200) return 'La dirección fiscal no puede superar los 200 caracteres.';

  const telefono = formulario.telefono.trim();
  if (telefono && !REGEX_TELEFONO.test(telefono)) {
    return 'El teléfono solo admite números, espacios, +, guiones y paréntesis.';
  }

  const logo = formulario.logoUrl.trim();
  if (logo && !REGEX_LOGO.test(logo)) {
    return 'El logotipo debe ser una URL que empiece con http:// o https://.';
  }

  if (!REGEX_SERIE_BOLETA.test(formulario.serieBoleta.trim().toUpperCase())) {
    return 'La serie de boletas debe tener 4 caracteres y empezar con B (por ejemplo B001).';
  }
  if (!REGEX_SERIE_FACTURA.test(formulario.serieFactura.trim().toUpperCase())) {
    return 'La serie de facturas debe tener 4 caracteres y empezar con F (por ejemplo F001).';
  }
  return '';
}

export default function ConfiguracionPanel() {
  const [tienda, setTienda] = useState(null);
  const [original, setOriginal] = useState(FORM_VACIO);
  const [formulario, setFormulario] = useState(FORM_VACIO);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [refresco, setRefresco] = useState(0);
  const [logoRoto, setLogoRoto] = useState('');

  useEffect(() => {
    let vigente = true;
    (async () => {
      try {
        const configuracion = await obtenerConfiguracion();
        if (!vigente) return;
        setTienda({ nombre: configuracion.nombre || '', slug: configuracion.slug || '' });
        setOriginal(aFormulario(configuracion));
        setFormulario(aFormulario(configuracion));
        setError('');
      } catch (problema) {
        if (!vigente) return;
        setError(problema?.message || 'No se pudo cargar la configuración de la tienda.');
      } finally {
        if (vigente) setCargando(false);
      }
    })();
    return () => {
      vigente = false;
    };
  }, [refresco]);

  const recargar = useCallback(() => {
    setCargando(true);
    setError('');
    setRefresco((valor) => valor + 1);
  }, []);

  const cambio = CAMPOS.some((campo) => formulario[campo] !== original[campo]);
  const logo = formulario.logoUrl.trim();
  const logoValido = REGEX_LOGO.test(logo);
  const inicial = (tienda?.nombre || '?').charAt(0).toUpperCase();

  function cambiar(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }));
  }

  function deshacer() {
    setFormulario(original);
    setLogoRoto('');
  }

  async function manejarSubmit(evento) {
    evento.preventDefault();
    const problema = validar(formulario);
    if (problema) {
      showToast(problema);
      return;
    }
    const datos = {};
    CAMPOS.forEach((campo) => {
      datos[campo] = formulario[campo].trim();
    });
    setGuardando(true);
    try {
      const guardada = await guardarConfiguracion(datos);
      setOriginal(aFormulario(guardada));
      setFormulario(aFormulario(guardada));
      showToast('Configuración guardada. Los próximos comprobantes usarán estos datos.');
    } catch (fallo) {
      showToast(fallo?.message || 'No se pudo guardar la configuración.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <PanelLayout>
      <main className="page historial-page">
        <header className="hist-cabecera">
          <div>
            <h1>Configuración de la tienda.</h1>
            <p>
              Datos fiscales del emisor y numeración de los comprobantes. Se guardan en la
              base de datos y los usan el punto de venta y la facturación electrónica.
            </p>
          </div>
          <div className="cf-cabecera-acciones">
            <button className="hist-btn actualizar" type="button" onClick={recargar} disabled={cargando}>
              <RefreshCw size={16} aria-hidden="true" className={cargando ? 'girando' : ''} />
              Actualizar
            </button>
          </div>
        </header>

        {error && (
          <div className="hist-error" role="alert">
            <p>{error}</p>
            <button className="hist-btn" type="button" onClick={recargar}>Reintentar</button>
          </div>
        )}

        {cargando && !error && <p className="hist-vacio">Cargando configuración…</p>}

        {!cargando && !error && (
          <form className="cf-form" onSubmit={manejarSubmit} noValidate>
            <div className="cf-columnas">
              <section className="cf-bloque" aria-labelledby="cf-datos-comerciales">
                <div className="cf-bloque-cabecera">
                  <h2 id="cf-datos-comerciales">Datos comerciales</h2>
                  <p>Identifican a tu empresa en la boleta, la factura y el XML que se envía a SUNAT.</p>
                </div>

                <div className="cf-identidad">
                  <span className="cf-identidad-nombre">{tienda?.nombre || 'Tu tienda'}</span>
                  <code>/{tienda?.slug || ''}</code>
                  <small>El nombre y el enlace público se editan en el registro de la tienda.</small>
                </div>

                <div className="cf-campos">
                  <div className="field cf-ancho">
                    <label htmlFor="razonSocial">Razón social</label>
                    <input
                      id="razonSocial"
                      type="text"
                      maxLength={150}
                      autoComplete="organization"
                      placeholder="Comercial Andina S.A.C."
                      value={formulario.razonSocial}
                      onChange={(evento) => cambiar('razonSocial', evento.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="ruc">RUC</label>
                    <input
                      id="ruc"
                      type="text"
                      inputMode="numeric"
                      maxLength={11}
                      placeholder="20604567891"
                      value={formulario.ruc}
                      onChange={(evento) => cambiar('ruc', evento.target.value)}
                    />
                    <span className="hint">11 dígitos, sin guiones.</span>
                  </div>
                  <div className="field">
                    <label htmlFor="telefono">Teléfono</label>
                    <input
                      id="telefono"
                      type="tel"
                      maxLength={30}
                      placeholder="+51 1 555-1234"
                      value={formulario.telefono}
                      onChange={(evento) => cambiar('telefono', evento.target.value)}
                    />
                    <span className="hint">Opcional. Se imprime como contacto del emisor.</span>
                  </div>
                  <div className="field cf-ancho">
                    <label htmlFor="direccionFiscal">Dirección fiscal</label>
                    <input
                      id="direccionFiscal"
                      type="text"
                      maxLength={200}
                      autoComplete="street-address"
                      placeholder="Av. Los Próceres 123, Lima"
                      value={formulario.direccionFiscal}
                      onChange={(evento) => cambiar('direccionFiscal', evento.target.value)}
                    />
                  </div>
                  <div className="field cf-ancho">
                    <label htmlFor="logoUrl">Logotipo de la tienda</label>
                    <div className="cf-logo">
                      <div className="cf-logo-vista" aria-hidden="true">
                        {logoValido && logoRoto !== logo ? (
                          <img
                            key={logo}
                            src={logo}
                            alt=""
                            onError={() => setLogoRoto(logo)}
                          />
                        ) : (
                          <span>{inicial}</span>
                        )}
                      </div>
                      <input
                        id="logoUrl"
                        type="url"
                        maxLength={255}
                        placeholder="https://cdn.tutienda.com/logo.png"
                        value={formulario.logoUrl}
                        onChange={(evento) => cambiar('logoUrl', evento.target.value)}
                      />
                    </div>
                    <span className="hint">
                      URL de una imagen (PNG, JPG o SVG). Se imprime en la cabecera del
                      comprobante; si no se puede descargar, el PDF sale sin logo.
                    </span>
                  </div>
                </div>
              </section>

              <section className="cf-bloque" aria-labelledby="cf-facturacion">
                <div className="cf-bloque-cabecera">
                  <h2 id="cf-facturacion">Parámetros de facturación</h2>
                  <p>Series con las que se numera cada tipo de comprobante.</p>
                </div>

                <div className="field cf-serie">
                  <label htmlFor="serieBoleta">Serie de boletas</label>
                  <input
                    id="serieBoleta"
                    type="text"
                    maxLength={4}
                    placeholder="B001"
                    value={formulario.serieBoleta}
                    onChange={(evento) => cambiar('serieBoleta', evento.target.value.toUpperCase())}
                  />
                  <span className="hint">Empieza con B y tiene 4 caracteres.</span>
                </div>

                <div className="field cf-serie">
                  <label htmlFor="serieFactura">Serie de facturas</label>
                  <input
                    id="serieFactura"
                    type="text"
                    maxLength={4}
                    placeholder="F001"
                    value={formulario.serieFactura}
                    onChange={(evento) => cambiar('serieFactura', evento.target.value.toUpperCase())}
                  />
                  <span className="hint">Empieza con F y tiene 4 caracteres.</span>
                </div>

                <div className="cf-nota">
                  <ShieldCheck size={18} aria-hidden="true" />
                  <p>
                    El número de cada comprobante se reserva en la base de datos, así que la
                    numeración es correlativa y sin saltos. El punto de venta y la emisión
                    electrónica comparten la misma secuencia de la serie.
                  </p>
                </div>

                <div className="cf-nota suave">
                  <CircleAlert size={18} aria-hidden="true" />
                  <p>
                    Cambia una serie solo cuando SUNAT te haya autorizado la nueva. Los
                    comprobantes ya emitidos conservan su serie original.
                  </p>
                </div>
              </section>
            </div>

            <div className="cf-acciones">
              <button className="btn" type="submit" disabled={guardando || !cambio}>
                <Save size={16} aria-hidden="true" />
                {guardando ? 'Guardando…' : 'Guardar cambios'}
              </button>
              <button
                className="btn ghost"
                type="button"
                onClick={deshacer}
                disabled={guardando || !cambio}
              >
                <Undo2 size={16} aria-hidden="true" />
                Descartar cambios
              </button>
            </div>
          </form>
        )}
      </main>
    </PanelLayout>
  );
}
