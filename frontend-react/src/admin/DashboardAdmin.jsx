import { useCallback, useEffect, useState } from 'react';
import { Banknote, Crown, RefreshCw, ShoppingBag, TrendingUp } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { obtenerResumenDashboard } from '../api/dashboardApi';
import { useStore } from '../store/store';
import PanelLayout from './PanelLayout';
import './DashboardAdmin.css';

const dinero = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  maximumFractionDigits: 2,
});
const entero = new Intl.NumberFormat('es-PE');

function recortar(texto, maximo = 18) {
  return texto.length > maximo ? `${texto.slice(0, maximo - 1)}…` : texto;
}

function TarjetaKpi({ icono, etiqueta, valor, detalle, acento }) {
  return (
    <article className={`dash-kpi${acento ? ' acento' : ''}`}>
      <div className="dash-kpi-cabecera">
        <span className="dash-kpi-icono" aria-hidden="true">{icono}</span>
        <span className="dash-kpi-etiqueta">{etiqueta}</span>
      </div>
      <p className="dash-kpi-valor">{valor}</p>
      <p className="dash-kpi-detalle">{detalle}</p>
    </article>
  );
}

function mensajeError(problema) {
  return problema?.message || 'No se pudo cargar el resumen.';
}

export default function DashboardAdmin() {
  const colorPrimario = useStore((estado) => estado.tienda.colorPrimario);
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setDatos(await obtenerResumenDashboard());
    } catch (problema) {
      setError(mensajeError(problema));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    let vigente = true;
    (async () => {
      try {
        const resumen = await obtenerResumenDashboard();
        if (!vigente) return;
        setDatos(resumen);
      } catch (problema) {
        if (vigente) setError(mensajeError(problema));
      } finally {
        if (vigente) setCargando(false);
      }
    })();
    return () => {
      vigente = false;
    };
  }, []);

  const topProductos = datos?.topProductos ?? [];
  const masVendido = topProductos[0];
  const acento = colorPrimario || '#db2777';

  return (
    <PanelLayout>
      <main className="page">
        <header className="head top">
          <div>
            <h1>Dashboard.</h1>
            <p>Cómo va tu tienda hoy y qué productos se mueven más.</p>
          </div>
          <button className="btn ghost" type="button" onClick={cargar} disabled={cargando}>
            <RefreshCw size={16} aria-hidden="true" />
            Actualizar
          </button>
        </header>

        {error && (
          <div className="dash-error section" role="alert">
            <p>{error}</p>
            <button className="btn small" type="button" onClick={cargar}>Reintentar</button>
          </div>
        )}

        {cargando && !datos && <p className="dash-cargando section">Cargando resumen…</p>}

        {datos && (
          <>
            <section className="dash-kpis" aria-label="Resumen de ventas">
              <TarjetaKpi
                icono={<TrendingUp size={20} />}
                etiqueta="Ventas de hoy"
                valor={dinero.format(datos.ventasHoy)}
                detalle="Total facturado en el día"
              />
              <TarjetaKpi
                icono={<Banknote size={20} />}
                etiqueta="Ingresos del mes"
                valor={dinero.format(datos.ingresosMes)}
                detalle="Acumulado del mes en curso"
              />
              <TarjetaKpi
                icono={<Crown size={20} />}
                etiqueta="Producto estrella"
                valor={masVendido ? masVendido.nombre : 'Sin ventas'}
                detalle={
                  masVendido
                    ? `${entero.format(masVendido.cantidad)} unidades vendidas`
                    : 'Aún no hay productos vendidos'
                }
                acento
              />
            </section>

            <section className="dash-panel section" aria-label="Productos más vendidos">
              <header className="dash-panel-cabecera">
                <div>
                  <h2>Top 5 productos</h2>
                  <p className="small muted">Unidades vendidas en toda la historia de la tienda.</p>
                </div>
                <span className="dash-panel-icono" aria-hidden="true">
                  <ShoppingBag size={18} />
                </span>
              </header>

              {topProductos.length === 0 && (
                <p className="dash-vacio">
                  Todavía no hay ventas registradas. Cuando vendas, aquí verás tus productos
                  más pedidos.
                </p>
              )}

              {topProductos.length > 0 && (
                <div className="dash-grafico">
                  <ResponsiveContainer width="100%" height={320}>
                    <BarChart
                      data={topProductos}
                      layout="vertical"
                      margin={{ top: 8, right: 24, bottom: 8, left: 8 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e5e5ea" />
                      <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12, fill: '#6e6e73' }} />
                      <YAxis
                        type="category"
                        dataKey="nombre"
                        width={150}
                        tick={{ fontSize: 12, fill: '#1d1d1f' }}
                        tickFormatter={(valor) => recortar(String(valor))}
                      />
                      <Tooltip
                        cursor={{ fill: 'rgba(0, 0, 0, 0.04)' }}
                        formatter={(valor, nombre) => [
                          nombre === 'cantidad' ? `${entero.format(valor)} u.` : dinero.format(valor),
                          nombre === 'cantidad' ? 'Unidades' : 'Importe',
                        ]}
                        labelStyle={{ color: '#1d1d1f', fontWeight: 500 }}
                        contentStyle={{
                          borderRadius: 10,
                          border: '1px solid #e5e5ea',
                          fontSize: 13,
                          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.08)',
                        }}
                      />
                      <Bar dataKey="cantidad" name="cantidad" radius={[0, 6, 6, 0]} maxBarSize={28}>
                        {topProductos.map((producto, indice) => (
                          <Cell
                            key={producto.idProducto}
                            fill={acento}
                            fillOpacity={1 - indice * 0.12}
                          />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </PanelLayout>
  );
}
