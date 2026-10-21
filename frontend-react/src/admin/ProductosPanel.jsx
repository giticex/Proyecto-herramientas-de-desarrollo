import { useEffect, useRef, useState } from 'react';
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

function Chip({ stock, bajo }) {
  const agotado = stock === 0;
  return (
    <span className={`chip${agotado ? ' agotado' : ''}`}>
      {bajo && (
        <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
          <path fill="currentColor" d="M12 2 1 21h22L12 2Zm1 15h-2v-2h2v2Zm0-4h-2V9h2v4Z" />
        </svg>
      )}
      {agotado ? 'Agotado' : bajo ? `${stock} · Stock bajo` : String(stock)}
    </span>
  );
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
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(FORM_VACIO);
  const [errores, setErrores] = useState(ERRORES_VACIOS);
  const [atributosFilas, setAtributosFilas] = useState([]);
  const dialogRef = useRef(null);

  useEffect(() => {
    listProductos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const esBajo = (p) => p.stock < umbral;
  const bajos = productos.filter(esBajo);
  const filtroActivo = soloBajo && bajos.length > 0;
  const visibles = filtroActivo ? bajos : productos;
  const categorias = [...new Set(productos.map((p) => p.categoria))];

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

        {bajos.length > 0 && (
          <section className="alerta section" aria-live="polite">
            <IconoAlerta />
            <div className="alerta-txt">
              <b>{bajos.length === 1 ? '1 producto con stock bajo' : `${bajos.length} productos con stock bajo`}</b>
              <span className="small">{bajos.map((p) => `${p.nombre} (${p.stock})`).join(' · ')}</span>
            </div>
            <div className="umbral">
              <label htmlFor="umbral" className="small">Avisar con menos de</label>
              <input id="umbral" type="number" min="0" step="1" value={umbral} onChange={(e) => cambiarUmbral(e.target.value)} />
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

        <section className="section lista" aria-label="Lista de productos">
          {visibles.length === 0 && <p className="vacio">Aún no hay productos. Crea el primero.</p>}
          {visibles.length > 0 && (
            <div className="fila cab">
              {['Nombre', 'Categoría', 'Precio', 'Stock', ''].map((h) => <span key={h || 'acciones'}>{h}</span>)}
            </div>
          )}
          {visibles.map((p) => {
            const bajo = esBajo(p);
            return (
              <div className={`fila${bajo ? ' bajo' : ''}`} key={p.idProducto}>
                <span className="nombre">{p.nombre}</span>
                <span data-label="Categoría">{p.categoria}</span>
                <span data-label="Precio">{fmt.format(p.precio)}</span>
                <span data-label="Stock"><Chip stock={p.stock} bajo={bajo} /></span>
                <span className="acciones">
                  <button className="btn small secondary" type="button" onClick={() => abrir(p)}>Editar</button>
                  <button className="btn small ghost" type="button" onClick={() => eliminar(p)}>Eliminar</button>
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
