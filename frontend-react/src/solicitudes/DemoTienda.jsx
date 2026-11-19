import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowUpRight, Check, Package, Palette, Receipt, ShoppingBag } from 'lucide-react';
import './Solicitudes.css';

const INICIAL = [{ id: 1, nombre: 'Bolso de lona', precio: 49, stock: 8 }, { id: 2, nombre: 'Pañuelo estampado', precio: 25, stock: 12 }];
const VISTAS = [['marca', 'Diseño', Palette], ['productos', 'Inventario', Package],
  ['catalogo', 'Catálogo', ShoppingBag], ['venta', 'Venta y comprobante', Receipt]];
const COLORES = [['#5939ad', 'Violeta'], ['#147363', 'Verde'], ['#a33a4d', 'Rojo frambuesa']];
const soles = (valor) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor);

export default function DemoTienda() {
  const [parametros] = useSearchParams();
  const modulo = parametros.get('modulo');
  const [vista, setVista] = useState(() => VISTAS.some(([id]) => id === modulo) ? modulo : 'marca');
  const [marca, setMarca] = useState('Moda Demo');
  const [color, setColor] = useState(COLORES[0][0]);
  const [productos, setProductos] = useState(INICIAL);
  const [nombre, setNombre] = useState('');
  const [precio, setPrecio] = useState('');
  const [stock, setStock] = useState('');
  const [carrito, setCarrito] = useState(null);
  const [venta, setVenta] = useState(null);
  const [tipo, setTipo] = useState('Boleta');
  const [consecutivo, setConsecutivo] = useState(0);
  const [aviso, setAviso] = useState('');

  function agregar(evento) {
    evento.preventDefault();
    if (!nombre.trim() || Number(precio) <= 0 || !Number.isInteger(Number(stock)) || Number(stock) < 0) return;
    setProductos((lista) => [...lista, { id: Date.now(), nombre: nombre.trim(), precio: +precio, stock: +stock }]);
    setNombre('');
    setPrecio('');
    setStock('');
    setAviso('Producto agregado. Ya aparece en el catálogo.');
  }

  function simularVenta() {
    if (!carrito || carrito.stock < 1) return;
    setProductos((lista) => lista.map((producto) =>
      producto.id === carrito.id ? { ...producto, stock: producto.stock - 1 } : producto));
    const siguiente = consecutivo + 1;
    setConsecutivo(siguiente);
    setVenta({ ...carrito, tipo, numero: String(siguiente).padStart(4, '0') });
    setCarrito(null);
    setAviso('Venta simulada. El inventario de la demo se actualizó.');
  }

  function ir(destino) { setVista(destino); setAviso(''); }

  function reiniciar() {
    setMarca('Moda Demo'); setColor(COLORES[0][0]); setProductos(INICIAL);
    setNombre(''); setPrecio(''); setStock(''); setTipo('Boleta');
    setCarrito(null); setVenta(null); setConsecutivo(0); ir('marca');
  }

  return (
    <main className="page flujo-page">
      <header className="flujo-cabecera">
        <div><h1>Prueba una tienda</h1><p className="muted">Cambia su diseño, agrega un producto y simula tu primera venta.</p></div>
        <Link className="btn" to="/registro-tienda">Solicitar mi tienda <ArrowUpRight size={17} /></Link>
      </header>
      <nav className="flujo-demo-nav" aria-label="Módulos de la demostración">
        {VISTAS.map(([id, etiqueta, Icono]) => (
          <button type="button" className="btn ghost" key={id} aria-pressed={vista === id} onClick={() => ir(id)}>
            <Icono size={18} aria-hidden="true" />{etiqueta}
          </button>
        ))}
      </nav>
      <p className="small muted">Los cambios de esta demo son temporales. Cada venta y comprobante es una simulación.</p>
      {aviso && <p className="flujo-aviso" role="status">{aviso}</p>}
      <div className="flujo-grid flujo-demo-grid">
        <aside className="flujo-demo-marca" style={{ '--demo-color': color }}>
          <span>VISTA PREVIA · DEMOSTRACIÓN</span>
          <h2>{marca || 'Tu tienda'}</h2>
          <p>{productos.length} productos · {productos.reduce((total, p) => total + p.stock, 0)} unidades</p>
          <button type="button" className="btn ghost" onClick={() => ir('catalogo')}>Ver catálogo</button>
        </aside>
        <section className="flujo-card flujo-form">
          {vista === 'marca' && <>
            <h2>Diseño de la tienda</h2>
            <label className="field">Nombre visible<input maxLength={40} value={marca} onChange={(e) => setMarca(e.target.value)} /></label>
            <div className="flujo-colores" aria-label="Colores de marca">
              {COLORES.map(([clave, etiqueta]) => (
                <button type="button" key={clave} className="btn ghost" aria-pressed={color === clave} onClick={() => setColor(clave)}>
                  <span style={{ background: clave }} />{etiqueta}{color === clave && <Check size={15} aria-hidden="true" />}
                </button>
              ))}
            </div>
            <button className="btn" type="button" onClick={() => ir('productos')}>Gestionar inventario</button>
          </>}
          {vista === 'productos' && <>
            <h2>Inventario</h2>
            <ul className="flujo-productos">
              {productos.map((p) => <li key={p.id}><strong>{p.nombre}</strong><span>{p.stock} en stock · {soles(p.precio)}</span></li>)}
            </ul>
            <form className="flujo-form" onSubmit={agregar}>
              <label className="field">Producto de prueba<input required maxLength={60} value={nombre} onChange={(e) => setNombre(e.target.value)} /></label>
              <div className="flujo-campos-dobles">
                <label className="field">Precio<input type="number" step=".01" min=".01" max="99999" required value={precio} onChange={(e) => setPrecio(e.target.value)} /></label>
                <label className="field">Stock<input type="number" min="0" max="999" required value={stock} onChange={(e) => setStock(e.target.value)} /></label>
              </div>
              <button className="btn" type="submit">Agregar producto</button>
            </form>
          </>}
          {vista === 'catalogo' && <>
            <h2>Catálogo del comprador</h2>
            <ul className="flujo-productos">
              {productos.map((p) => <li key={p.id}>
                <div><strong>{p.nombre}</strong><p className="small muted">{soles(p.precio)}</p></div>
                <button type="button" className="btn ghost small" disabled={p.stock < 1}
                  onClick={() => { setCarrito(p); ir('venta'); }}>{p.stock ? 'Simular compra' : 'Agotado'}</button>
              </li>)}
            </ul>
          </>}
          {vista === 'venta' && <>
            <h2>Venta y comprobante</h2>
            {carrito && <>
              <p>{carrito.nombre} · {soles(carrito.precio)}</p>
              <label className="field">Tipo de comprobante
                <select value={tipo} onChange={(e) => setTipo(e.target.value)}><option>Boleta</option><option>Factura</option></select>
              </label>
              <button type="button" className="btn" onClick={simularVenta}>Confirmar venta simulada</button>
            </>}
            {venta && <article className="flujo-recibo">
              <strong>{venta.tipo} DEMO-{venta.numero}</strong>
              <p>{venta.nombre} × 1</p><p>Total simulado {soles(venta.precio)}</p>
              <p className="small muted">Comprobante de demostración. Sin valor fiscal.</p>
            </article>}
            {!venta && !carrito && <p className="muted">Elige un producto en el catálogo para simular una venta.</p>}
            <button type="button" className="btn ghost" onClick={() => ir('catalogo')}>Volver al catálogo</button>
          </>}
        </section>
      </div>
      <button type="button" className="btn ghost" onClick={reiniciar}>Reiniciar demo</button>
    </main>
  );
}
