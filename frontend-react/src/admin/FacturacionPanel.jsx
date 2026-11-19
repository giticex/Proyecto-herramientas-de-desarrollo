import { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import PanelLayout from './PanelLayout';

const ruta = '/api/facturacion/emisor';
const vacio = {
  emisor: {
    ruc: '', razonSocial: '', nombreComercial: '', domicilioFiscal: '', ubigeo: '',
    distrito: '', provincia: '', departamento: '', pais: 'PE', email: '', telefono: '',
  },
  serieBoleta: '',
  serieFactura: '',
};

const datos = [
  ['ruc', 'RUC', '[0-9]{11}', 11, 'text'],
  ['razonSocial', 'Razón social', undefined, 200, 'text'],
  ['nombreComercial', 'Nombre comercial', undefined, 120, 'text'],
  ['domicilioFiscal', 'Dirección fiscal', undefined, 200, 'text'],
  ['ubigeo', 'Ubigeo', '[0-9]{6}', 6, 'text'],
  ['distrito', 'Distrito', undefined, 80, 'text'],
  ['provincia', 'Provincia', undefined, 80, 'text'],
  ['departamento', 'Departamento', undefined, 80, 'text'],
];

export default function FacturacionPanel() {
  const [form, setForm] = useState(vacio);
  const [carga, setCarga] = useState('cargando');
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let vigente = true;
    apiClient.get(ruta)
      .then((configuracion) => {
        if (!vigente) return;
        if (configuracion) setForm({ ...configuracion, emisor: { ...vacio.emisor, ...configuracion.emisor } });
        setCarga('lista');
      })
      .catch((problema) => {
        if (!vigente) return;
        setError(problema.message || 'No se pudo consultar la configuración fiscal.');
        setCarga('error');
      });
    return () => { vigente = false; };
  }, []);

  function cambiarCampo(campo, valor) {
    setForm((actual) => ({ ...actual, emisor: { ...actual.emisor, [campo]: valor } }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    setError('');
    setAviso('');
    setGuardando(true);
    try {
      const emisor = Object.fromEntries(Object.entries(form.emisor).map(([nombre, valor]) => [nombre, valor?.trim() ?? '']));
      const datosGuardados = await apiClient.put(ruta, { ...form, emisor });
      setForm(datosGuardados);
      setAviso('Datos fiscales guardados.');
    } catch (problema) {
      setError(problema.message || 'No se pudo guardar la configuración fiscal.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <PanelLayout>
      <main className="page">
        <header className="head">
          <h1>Facturación</h1>
          <p>Configura el emisor y las series que usará tu tienda al generar boletas y facturas.</p>
        </header>
        {carga === 'cargando' && <p role="status">Cargando datos fiscales…</p>}
        {error && <p className="error" role="alert">{error}</p>}
        {aviso && <p role="status">{aviso}</p>}
        {carga === 'lista' && (
          <form className="form" onSubmit={guardar}>
            {datos.map(([campo, titulo, pattern, maxLength, type]) => (
              <label key={campo} className="field">
                <span>{titulo}</span>
                <input
                  required type={type} value={form.emisor[campo] ?? ''} pattern={pattern} maxLength={maxLength}
                  onChange={(e) => cambiarCampo(campo, e.target.value)}
                />
              </label>
            ))}
            <label className="field">
              <span>Correo de facturación (opcional)</span>
              <input type="email" autoComplete="email" maxLength={120} value={form.emisor.email ?? ''}
                onChange={(e) => cambiarCampo('email', e.target.value)} />
            </label>
            <label className="field">
              <span>Teléfono (opcional)</span>
              <input type="tel" autoComplete="tel" maxLength={30} value={form.emisor.telefono ?? ''}
                onChange={(e) => cambiarCampo('telefono', e.target.value)} />
            </label>
            <label className="field">
              <span>Serie de boleta</span>
              <input required pattern="B[A-Z0-9]{3}" maxLength={4} placeholder="B001" value={form.serieBoleta}
                onChange={(e) => setForm({ ...form, serieBoleta: e.target.value.toUpperCase() })} />
            </label>
            <label className="field">
              <span>Serie de factura</span>
              <input required pattern="F[A-Z0-9]{3}" maxLength={4} placeholder="F001" value={form.serieFactura}
                onChange={(e) => setForm({ ...form, serieFactura: e.target.value.toUpperCase() })} />
            </label>
            <button className="btn" type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar datos fiscales'}</button>
          </form>
        )}
      </main>
    </PanelLayout>
  );
}
