import { useCallback, useEffect, useState } from 'react';
import {
  Banknote,
  CalendarDays,
  Download,
  FileSpreadsheet,
  FileText,
  RefreshCw,
  Scale,
  Receipt,
  Coins,
} from 'lucide-react';
import { obtenerArqueo, rutaExportacionArqueo } from '../api/arqueoApi';
import { descargarArchivoSeguro } from '../utils/descargas';
import { showToast } from '../components/toastBus';
import { useStore } from '../store/store';
import PanelLayout from './PanelLayout';
import './HistorialVentas.css';
import './ArqueoCajaPanel.css';

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});
const porcentaje = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const entero = new Intl.NumberFormat('es-PE');

function fechaLocalIso() {
  const ahora = new Date();
  const desfase = ahora.getTimezoneOffset() * 60000;
  return new Date(ahora.getTime() - desfase).toISOString().slice(0, 10);
}

function Tarjeta({ icono, etiqueta, valor, detalle, acento }) {
  return (
    <article className={`arq-kpi${acento ? ' acento' : ''}`}>
      <div className="arq-kpi-cabecera">
        <span className="arq-kpi-icono" aria-hidden="true">{icono}</span>
        <span className="arq-kpi-etiqueta">{etiqueta}</span>
      </div>
      <p className="arq-kpi-valor">{valor}</p>
      <p className="arq-kpi-detalle">{detalle}</p>
    </article>
  );
}

export default function ArqueoCajaPanel() {
  const colorPrimario = useStore((estado) => estado.tienda.colorPrimario);
  const [periodo, setPeriodo] = useState('diario');
  const [fechaDia, setFechaDia] = useState(fechaLocalIso);
  const [fechaMes, setFechaMes] = useState(() => fechaLocalIso().slice(0, 7));
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [exportando, setExportando] = useState('');

  const fechaConsulta = periodo === 'mensual' ? `${fechaMes}-01` : fechaDia;
  const acento = colorPrimario || '#0b4a6f';

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setDatos(await obtenerArqueo({ periodo, fecha: fechaConsulta }));
    } catch (problema) {
      setError(problema?.message || 'No se pudo cargar el corte de caja.');
      setDatos(null);
    } finally {
      setCargando(false);
    }
  }, [periodo, fechaConsulta]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  async function exportar(formato) {
    if (exportando) return;
    setExportando(formato);
    const nombre = `arqueo-${periodo}-${periodo === 'mensual' ? fechaMes : fechaDia}`;
    try {
      await descargarArchivoSeguro(
        rutaExportacionArqueo({ periodo, fecha: fechaConsulta, formato }),
        formato,
        nombre,
      );
      showToast(`Reporte ${formato.toUpperCase()} descargado.`);
    } catch (problema) {
      showToast(problema?.message || 'No se pudo exportar el reporte.');
    } finally {
      setExportando('');
    }
  }

  const efectivo = datos?.metodosPago?.find((grupo) => grupo.grupo === 'Efectivo');
  const hayVentas = Boolean(datos && datos.comprobantes > 0);

  return (
    <PanelLayout>
      <main className="page arq-page">
        <header className="head top arq-cabecera">
          <div>
            <h1>Corte de caja.</h1>
            <p>
              Cuánto ingresó, por qué medio, quién lo atendió y en qué turno. Exporta el
              reporte para tu contabilidad.
            </p>
          </div>
          <button className="hist-btn actualizar" type="button" onClick={cargar} disabled={cargando}>
            <RefreshCw size={16} aria-hidden="true" />
            Actualizar
          </button>
        </header>

        <div className="arq-toolbar">
          <div className="arq-segmento" role="group" aria-label="Periodo del corte">
            <button
              type="button"
              className={periodo === 'diario' ? 'activo' : ''}
              aria-pressed={periodo === 'diario'}
              onClick={() => setPeriodo('diario')}
            >
              Diario
            </button>
            <button
              type="button"
              className={periodo === 'mensual' ? 'activo' : ''}
              aria-pressed={periodo === 'mensual'}
              onClick={() => setPeriodo('mensual')}
            >
              Mensual
            </button>
          </div>

          <label className="arq-fecha">
            <CalendarDays size={16} aria-hidden="true" />
            <span className="arq-fecha-etiqueta">
              {periodo === 'mensual' ? 'Mes' : 'Día'}
            </span>
            {periodo === 'mensual' ? (
              <input
                type="month"
                value={fechaMes}
                max={fechaLocalIso().slice(0, 7)}
                onChange={(evento) => setFechaMes(evento.target.value)}
              />
            ) : (
              <input
                type="date"
                value={fechaDia}
                max={fechaLocalIso()}
                onChange={(evento) => setFechaDia(evento.target.value)}
              />
            )}
          </label>

          <div className="arq-exportar">
            <button
              className="btn ghost arq-btn"
              type="button"
              onClick={() => exportar('csv')}
              disabled={Boolean(exportando)}
            >
              <FileSpreadsheet size={16} aria-hidden="true" />
              {exportando === 'csv' ? 'Exportando…' : 'Exportar CSV'}
            </button>
            <button
              className="btn arq-btn"
              type="button"
              onClick={() => exportar('pdf')}
              disabled={Boolean(exportando)}
            >
              <FileText size={16} aria-hidden="true" />
              {exportando === 'pdf' ? 'Exportando…' : 'Exportar PDF'}
            </button>
          </div>
        </div>

        {error && (
          <div className="hist-error arq-error" role="alert">
            <p>{error}</p>
            <button className="btn small" type="button" onClick={cargar}>Reintentar</button>
          </div>
        )}

        {cargando && !datos && (
          <p className="arq-cargando">
            <Download size={16} aria-hidden="true" />
            Calculando el corte…
          </p>
        )}

        {datos && (
          <div className={cargando ? 'arq-contenido cargando' : 'arq-contenido'}>
            <p className="arq-meta">
              <Scale size={15} aria-hidden="true" />
              <strong>{datos.tiendaNombre || 'Mi tienda'}</strong>
              {datos.tiendaRuc ? <span>· RUC {datos.tiendaRuc}</span> : null}
              <span>· Del {datos.desde} al {datos.hasta}</span>
              <span>· Generado el {datos.generadoEn}</span>
            </p>

            <section className="arq-kpis" aria-label="Resumen del corte">
              <Tarjeta
                icono={<Banknote size={20} />}
                etiqueta="Total vendido"
                valor={dinero.format(Number(datos.totalVentas))}
                detalle={periodo === 'mensual' ? 'Acumulado del mes' : 'Facturado en el día'}
                acento
              />
              <Tarjeta
                icono={<Receipt size={20} />}
                etiqueta="Comprobantes"
                valor={entero.format(datos.comprobantes)}
                detalle="Documentos emitidos en el periodo"
              />
              <Tarjeta
                icono={<Coins size={20} />}
                etiqueta="Ticket promedio"
                valor={dinero.format(Number(datos.ticketPromedio))}
                detalle="Importe medio por comprobante"
              />
              <Tarjeta
                icono={<Scale size={20} />}
                etiqueta="Efectivo en caja"
                valor={dinero.format(Number(efectivo?.total ?? 0))}
                detalle="Para el arqueo físico del cajón"
              />
            </section>

            {!hayVentas && (
              <p className="arq-vacio">
                No hay ventas registradas en este periodo. Cuando vendas, aquí verás el
                desglose por medio de pago, vendedor y turno.
              </p>
            )}

            <section className="arq-panel arq-seccion" aria-label="Ingresos por método de pago">
              <header className="arq-panel-cabecera">
                <div>
                  <h2>Ingresos por método de pago</h2>
                  <p className="small muted">
                    Total del periodo dividido según cómo pagó el cliente.
                  </p>
                </div>
              </header>
              <div className="arq-tabla-envoltura">
                <table className="arq-tabla">
                  <thead>
                    <tr>
                      <th scope="col">Grupo</th>
                      <th scope="col">Medios incluidos</th>
                      <th scope="col" className="num">Comprobantes</th>
                      <th scope="col" className="num">% del total</th>
                      <th scope="col" className="num">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {datos.metodosPago.map((grupo) => (
                      <tr key={grupo.grupo}>
                        <th scope="row">{grupo.grupo}</th>
                        <td className="arq-medios">
                          {grupo.metodos.length > 0 ? grupo.metodos.join(', ') : '—'}
                        </td>
                        <td className="num">{entero.format(grupo.comprobantes)}</td>
                        <td className="num arq-porcentaje">
                          <span>{porcentaje.format(Number(grupo.porcentaje))} %</span>
                          <span className="arq-barra" aria-hidden="true">
                            <span
                              className="arq-barra-relleno"
                              style={{
                                width: `${Math.min(100, Math.max(0, Number(grupo.porcentaje)))}%`,
                                background: acento,
                              }}
                            />
                          </span>
                        </td>
                        <td className="num arq-total">{dinero.format(Number(grupo.total))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <div className="arq-grid">
              <section className="arq-panel" aria-label="Total por vendedor">
                <header className="arq-panel-cabecera">
                  <div>
                    <h2>Total por vendedor</h2>
                    <p className="small muted">Quién atendió cada venta del periodo.</p>
                  </div>
                </header>
                <div className="arq-tabla-envoltura">
                  <table className="arq-tabla">
                    <thead>
                      <tr>
                        <th scope="col">Vendedor</th>
                        <th scope="col">Usuario</th>
                        <th scope="col" className="num">Comprob.</th>
                        <th scope="col" className="num">Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {datos.vendedores.map((vendedor) => (
                        <tr key={vendedor.usuario || vendedor.nombre}>
                          <th scope="row">{vendedor.nombre}</th>
                          <td className="arq-usuario">{vendedor.usuario || '—'}</td>
                          <td className="num">{entero.format(vendedor.comprobantes)}</td>
                          <td className="num arq-total">{dinero.format(Number(vendedor.total))}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>

              <section className="arq-panel" aria-label="Total por turno">
                <header className="arq-panel-cabecera">
                  <div>
                    <h2>Total por turno</h2>
                    <p className="small muted">Caja de mañana, tarde y noche.</p>
                  </div>
                </header>
                <ul className="arq-turnos">
                  {datos.turnos.map((turno) => (
                    <li key={turno.turno} className="arq-turno">
                      <div>
                        <p className="arq-turno-nombre">{turno.turno}</p>
                        <p className="small muted">{turno.rango}</p>
                      </div>
                      <div className="arq-turno-cifras">
                        <p className="arq-turno-total">{dinero.format(Number(turno.total))}</p>
                        <p className="small muted">
                          {entero.format(turno.comprobantes)} comprobante
                          {turno.comprobantes === 1 ? '' : 's'}
                        </p>
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            </div>

            <section className="arq-panel arq-seccion" aria-label="Detalle de comprobantes">
              <header className="arq-panel-cabecera">
                <div>
                  <h2>Detalle de comprobantes</h2>
                  <p className="small muted">
                    Documentos que respaldan los importes del corte.
                  </p>
                </div>
              </header>
              {datos.detalle.length === 0 ? (
                <p className="arq-detalle-vacio">Sin comprobantes en el periodo.</p>
              ) : (
                <div className="arq-tabla-envoltura arq-detalle">
                  <table className="arq-tabla arq-tabla-detalle">
                    <thead>
                      <tr>
                        <th scope="col">Fecha y hora</th>
                        <th scope="col">Comprobante</th>
                        <th scope="col">Cliente</th>
                        <th scope="col">Documento</th>
                        <th scope="col">Medio de pago</th>
                        <th scope="col">Turno</th>
                        <th scope="col">Vendedor</th>
                        <th scope="col" className="num">Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {datos.detalle.map((comprobante) => (
                        <tr key={comprobante.idVenta}>
                          <td>{comprobante.fechaHora}</td>
                          <td>{comprobante.numeroDocumento || '—'}</td>
                          <td>{comprobante.cliente || '—'}</td>
                          <td>{comprobante.documentoCliente || '—'}</td>
                          <td>{comprobante.medioPago || '—'}</td>
                          <td>{comprobante.turno}</td>
                          <td>{comprobante.vendedor}</td>
                          <td className="num arq-total">{dinero.format(Number(comprobante.total))}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <p className="arq-nota">
              Los importes por método de pago incluyen las ventas al crédito. El grupo
              Créditos no representa efectivo cobrado en caja: úsalo para conciliar la
              cuenta por cobrar, no el cajón.
            </p>
          </div>
        )}
      </main>
    </PanelLayout>
  );
}
