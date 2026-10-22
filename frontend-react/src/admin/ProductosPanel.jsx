import { useEffect, useRef, useState } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import { useStore } from '../store/store';
import { showToast } from '../components/toastBus';
import AtributosEditor, { atributosAFilas, filasAAtributos } from './AtributosEditor';
import PanelLayout from './PanelLayout';
import './ProductosPanel.css';

const fmt = new Intl.NumberFormat('es', { style: 'currency', currency: 'USD' });

const FORM_VACIO = { nombre: '', categoria: '', precio: '', stock: '' };
const ERRORES_VACIOS = { nombre: '', categoria: '', precio: '', stock: '' };

function IconoAlerta() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <path fill="currentColor" d="M12 2 1 21h22L12 2Zm1 15h-2v-2h2v2Zm0-4h-2V9h2v4Z" />
    </svg>
  );
}

function BadgeStock({ stock, umbral }) {
  const clase = stock === 0 ? 'agotado' : stock <= umbral ? 'critico' : 'ok';
  return <span className={`badge ${clase}`}>{stock === 0 ? 'Agotado' : stock}</span>;
}

// Porta frontend/productos.html + frontend/js/productos.js, mas el editor de
// atributos (clave/valor) que no existía en el sitio viejo.
export default function ProductosPanel() {
  const productos = useStore((estado) => estado.productos);
  const listProductos = useStore((estado) => estado.listProductos);
  const createProducto = useStore((estado) => estado.createProducto);
  const updateProducto = useStore((estado) => estado.updateProducto);
  const deleteProducto = useStore((estado) => estado.deleteProducto);
  const umbral = useStore((estado) => estado.umbral);
  const setUmbral = useStore((estado) => estado.setUmbral);

  const [soloBajo, setSoloBajo] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [categoriaFiltro, setCategoriaFiltro] = useState('');
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(FORM_VACIO);
  const [errores, setErrores] = useState(ERRORES_VACIOS);
  const [atributosFilas, setAtributosFilas] = useState([]);
  const dialogRef = useRef(null);

  useEffect(() => {
    listProductos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const esBajo = (p) => p.stock <= umbral;
  const bajos = productos.filter(esBajo);
  const filtroActivo = soloBajo && bajos.length > 0;
  const visibles = filtroActivo ? bajos : productos;
  const categorias = [...new Set(productos.map((p) => p.categoria))];
  const valorInventario = productos.reduce(
    (total, p) => total + Number(p.precio || 0) * Number(p.stock || 0),
    0,
  );
  const textoBuscado = busqueda.trim().toLowerCase();
  const productosFiltrados = visibles.filter((p) => {
    const coincideTexto = !textoBuscado || p.nombre.toLowerCase().includes(textoBuscado);
    const coincideCategoria = !categoriaFiltro || p.categoria === categoriaFiltro;
    return coincideTexto && coincideCategoria;
  });
  const filtrosActivos = Boolean(textoBuscado || categoriaFiltro);

  function abrir(producto) {
    setEditandoId(producto ? producto.idProducto : null);
    setForm(producto
      ? { nombre: producto.nombre, categoria: producto.categoria, precio: String(producto.precio), stock: String(producto.stock) }
      : FORM_VACIO);
    setAtributosFilas(atributosAFilas(producto?.atributos));
    setErrores(ERRORES_VACIOS);
    dialogRef.current?.showModal();
  }

  function cerrarModal() {
    dialogRef.current?.close();
  }

  async function manejarSubmit(evento) {
    evento.preventDefault();
    const v = {
      nombre: form.nombre.trim(),
      categoria: form.categoria.trim(),
      precio: parseFloat(form.precio),
      stock: parseInt(form.stock, 10),
    };
    const reglas = {
      nombre: v.nombre ? '' : 'Requerido.',
      categoria: v.categoria ? '' : 'Requerida.',
      precio: v.precio >= 0 ? '' : 'Precio inválido.',
      stock: Number.isInteger(v.stock) && v.stock >= 0 ? '' : 'Stock inválido.',
    };
    setErrores(reglas);
    if (Object.values(reglas).some(Boolean)) return;

    const payload = { ...v, atributos: filasAAtributos(atributosFilas) };
    // BACKEND: POST /productos o PUT /productos/{id}, ya existen ambos.
    if (editandoId) {
      await updateProducto(editandoId, payload);
    } else {
      await createProducto(payload);
    }
    cerrarModal();
    showToast(editandoId ? 'Producto actualizado' : 'Producto creado');
    await listProductos();
  }

  async function eliminar(producto) {
    if (!confirm(`¿Eliminar "${producto.nombre}"?`)) return;
    await deleteProducto(producto.idProducto);
    showToast('Producto eliminado');
    await listProductos();
  }

  function cambiarUmbral(valor) {
    const n = parseInt(valor, 10);
    if (Number.isInteger(n) && n >= 0) setUmbral(n);
  }

  return (
    <PanelLayout>
      <main className="page">
        <header className="head top">
          <div>
            <h1>Productos.</h1>
            <p>Crea, edita y controla tu inventario.</p>
          </div>
          <button className="btn" type="button" onClick={() => abrir(null)}>Nuevo producto</button>
        </header>

        <section className="kpi-grid" aria-label="Resumen del inventario">
          <article className="kpi-card">
            <div className="kpi-cabecera">
              <span className="kpi-icono" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M12 2 3 7v10l9 5 9-5V7l-9-5Zm0 2.3 6.2 3.4L12 11.1 5.8 7.7 12 4.3ZM5 9.4l6 3.3v6.4l-6-3.3V9.4Zm8 9.7v-6.4l6-3.3v6.4l-6 3.3Z" />
                </svg>
              </span>
              <span className="kpi-etiqueta">Total productos</span>
            </div>
            <p className="kpi-valor">{productos.length}</p>
            <p className="kpi-detalle">
              {categorias.length === 1 ? '1 categoría' : `${categorias.length} categorías`}
            </p>
          </article>

          <article className={`kpi-card${bajos.length > 0 ? ' critico' : ''}`}>
            <div className="kpi-cabecera">
              <span className="kpi-icono" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M12 2 1 21h22L12 2Zm1 15h-2v-2h2v2Zm0-4h-2V9h2v4Z" />
                </svg>
              </span>
              <span className="kpi-etiqueta">Stock crítico</span>
            </div>
            <p className="kpi-valor">{bajos.length}</p>
            <p className="kpi-detalle">{umbral} unidades o menos</p>
          </article>

          <article className="kpi-card">
            <div className="kpi-cabecera">
              <span className="kpi-icono" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm1 14h-2v-1.2a3 3 0 0 1-2.2-2.3l1.9-.5c.2.8.8 1.3 1.6 1.3.7 0 1.2-.3 1.2-.9 0-.5-.3-.8-1.4-1.1-1.5-.4-2.9-.9-2.9-2.6 0-1.2.8-2.1 2-2.4V6h2v1.2c1 .2 1.7.9 2 1.9l-1.8.6c-.2-.6-.7-1-1.3-1-.6 0-1 .3-1 .8s.5.7 1.6 1c1.6.4 2.7 1 2.7 2.7 0 1.3-.9 2.2-2.2 2.5V17Z" />
                </svg>
              </span>
              <span className="kpi-etiqueta">Valor del inventario</span>
            </div>
            <p className="kpi-valor">{fmt.format(valorInventario)}</p>
            <p className="kpi-detalle">Precio × stock de cada producto</p>
          </article>
        </section>

        {bajos.length > 0 && (
          <section className="alerta section" aria-live="polite">
            <IconoAlerta />
            <div className="alerta-txt">
              <b>{bajos.length === 1 ? '1 producto con stock bajo' : `${bajos.length} productos con stock bajo`}</b>
              <span className="small">{bajos.map((p) => `${p.nombre} (${p.stock})`).join(' · ')}</span>
            </div>
            <div className="umbral">
              <label htmlFor="umbral" className="small">Avisar con</label>
              <input id="umbral" type="number" min="0" step="1" value={umbral} onChange={(e) => cambiarUmbral(e.target.value)} />
              <span className="small muted">o menos</span>
              <button
                className="btn small ghost"
                type="button"
                aria-pressed={filtroActivo}
                onClick={() => setSoloBajo((v) => !v)}
              >
                {filtroActivo ? 'Ver todos' : 'Ver solo estos'}
              </button>
            </div>
          </section>
        )}
        {bajos.length === 0 && (
          <div className="umbral solo">
            <label htmlFor="umbral2" className="small muted">Umbral de stock bajo</label>
            <input id="umbral2" type="number" min="0" step="1" value={umbral} onChange={(e) => cambiarUmbral(e.target.value)} />
          </div>
        )}

        <section className="toolbar" aria-label="Buscar y filtrar productos">
          <div className="buscador">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path fill="currentColor" d="M10 2a8 8 0 1 0 4.9 14.3l5.4 5.4 1.4-1.4-5.4-5.4A8 8 0 0 0 10 2Zm0 2a6 6 0 1 1 0 12 6 6 0 0 1 0-12Z" />
            </svg>
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar producto por nombre..."
              aria-label="Buscar producto por nombre"
            />
          </div>
          <label className="filtro-categoria">
            <span className="small muted">Categoría</span>
            <select value={categoriaFiltro} onChange={(e) => setCategoriaFiltro(e.target.value)}>
              <option value="">Todas</option>
              {categorias.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </label>
        </section>

        <section className="section lista" aria-label="Lista de productos">
          {productosFiltrados.length === 0 && (
            <p className="vacio">
              {productos.length === 0
                ? 'Aún no hay productos. Crea el primero.'
                : filtrosActivos
                  ? 'Ningún producto coincide con la búsqueda.'
                  : 'No hay productos con stock crítico.'}
            </p>
          )}
          {productosFiltrados.length > 0 && (
            <div className="fila cab">
              {['Nombre', 'Categoría', 'Precio', 'Stock', ''].map((h) => <span key={h || 'acciones'}>{h}</span>)}
            </div>
          )}
          {productosFiltrados.map((p) => {
            const bajo = esBajo(p);
            return (
              <div className={`fila${bajo ? ' bajo' : ''}`} key={p.idProducto}>
                <span className="nombre">{p.nombre}</span>
                <span data-label="Categoría">{p.categoria}</span>
                <span data-label="Precio">{fmt.format(p.precio)}</span>
                <span data-label="Stock"><BadgeStock stock={p.stock} umbral={umbral} /></span>
                <span className="acciones">
                  <button
                    className="icon-btn editar"
                    type="button"
                    onClick={() => abrir(p)}
                    title="Editar"
                    aria-label={`Editar ${p.nombre}`}
                  >
                    <Pencil size={16} strokeWidth={2} aria-hidden="true" />
                  </button>
                  <button
                    className="icon-btn eliminar"
                    type="button"
                    onClick={() => eliminar(p)}
                    title="Eliminar"
                    aria-label={`Eliminar ${p.nombre}`}
                  >
                    <Trash2 size={16} strokeWidth={2} aria-hidden="true" />
                  </button>
                </span>
              </div>
            );
          })}
        </section>
      </main>

      <dialog ref={dialogRef}>
        <form className="form" onSubmit={manejarSubmit} noValidate>
          <h1>{editandoId ? 'Editar producto' : 'Nuevo producto'}</h1>

          <div className={`field ${errores.nombre ? 'invalid' : ''}`}>
            <label htmlFor="f-nombre">Nombre</label>
            <input id="f-nombre" type="text" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
            <span className="error">{errores.nombre}</span>
          </div>

          <div className={`field ${errores.categoria ? 'invalid' : ''}`}>
            <label htmlFor="f-categoria">Categoría</label>
            <input
              id="f-categoria"
              type="text"
              list="cats"
              value={form.categoria}
              onChange={(e) => setForm({ ...form, categoria: e.target.value })}
            />
            <datalist id="cats">
              {categorias.map((c) => <option key={c} value={c} />)}
            </datalist>
            <span className="error">{errores.categoria}</span>
          </div>

          <div className="row2">
            <div className={`field ${errores.precio ? 'invalid' : ''}`}>
              <label htmlFor="f-precio">Precio</label>
              <input id="f-precio" type="number" min="0" step="0.01" value={form.precio} onChange={(e) => setForm({ ...form, precio: e.target.value })} />
              <span className="error">{errores.precio}</span>
            </div>
            <div className={`field ${errores.stock ? 'invalid' : ''}`}>
              <label htmlFor="f-stock">Stock</label>
              <input id="f-stock" type="number" min="0" step="1" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} />
              <span className="error">{errores.stock}</span>
            </div>
          </div>

          <AtributosEditor filas={atributosFilas} onChange={setAtributosFilas} />

          <div className="actions">
            <button className="btn" type="submit">Guardar</button>
            <button className="btn ghost" type="button" onClick={cerrarModal}>Cancelar</button>
          </div>
        </form>
      </dialog>
    </PanelLayout>
  );
}
