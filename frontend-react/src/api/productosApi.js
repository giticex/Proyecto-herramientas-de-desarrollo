// Productos: el backend SI tiene GET/POST/PUT/DELETE /productos, así que se
// usa primero y solo se cae al mock de localStorage si la llamada falla
// (backend apagado, sin red, etc.), igual que hacía frontend/js/store.js.
import { apiClient } from './client';

const KEY = 'productos';

function leerMock() {
  try {
    const guardado = localStorage.getItem(KEY);
    return guardado ? JSON.parse(guardado) : null;
  } catch {
    return null;
  }
}

function escribirMock(productos) {
  localStorage.setItem(KEY, JSON.stringify(productos));
}

const SEED = () => [
  { idProducto: 1, nombre: 'Camiseta básica', stock: 42, precio: 24.9, categoria: 'Ropa', atributos: { talla: 'M', color: 'blanco' } },
  { idProducto: 2, nombre: 'Jean slim', stock: 4, precio: 59, categoria: 'Ropa', atributos: { talla: '32', color: 'azul' } },
  { idProducto: 3, nombre: 'Zapatillas urbanas', stock: 0, precio: 89.5, categoria: 'Calzado', atributos: {} },
  { idProducto: 4, nombre: 'Gorra', stock: 18, precio: 15, categoria: 'Accesorios', atributos: {} },
];

function obtenerMock() {
  let productos = leerMock();
  if (!productos) {
    productos = SEED();
    escribirMock(productos);
  }
  return productos;
}

function skuParaMock(productos, sku, idProducto) {
  const actual = productos.find((producto) => producto.idProducto === idProducto);
  const codigo = sku?.trim().toUpperCase() || actual?.sku
    || `SKU-${globalThis.crypto.randomUUID().replaceAll('-', '').toUpperCase()}`;
  if (productos.some((producto) => producto.idProducto !== idProducto && producto.sku === codigo)) {
    throw new Error('Ya existe un producto con ese SKU.');
  }
  return codigo;
}

export async function listProductos() {
  try {
    return await apiClient.get('/productos');
  } catch {
    return obtenerMock();
  }
}

export function listProductosTienda(slug) {
  return apiClient.get(`/tiendas/${encodeURIComponent(slug)}/productos`, { token: '' });
}

export async function createProducto(producto) {
  try {
    return await apiClient.post('/productos', producto);
  } catch (error) {
    if (error.status) throw error;
    const productos = obtenerMock();
    const siguienteId = productos.reduce((max, p) => Math.max(max, p.idProducto), 0) + 1;
    const sku = skuParaMock(productos, producto.sku);
    const nuevo = { ...producto, idProducto: producto.idProducto || siguienteId, atributos: producto.atributos || {}, sku };
    escribirMock([...productos, nuevo]);
    return nuevo;
  }
}

export async function updateProducto(idProducto, producto) {
  try {
    return await apiClient.put(`/productos/${idProducto}`, producto);
  } catch (error) {
    if (error.status) throw error;
    const guardados = obtenerMock();
    const sku = skuParaMock(guardados, producto.sku, idProducto);
    const productos = guardados.map((p) => (p.idProducto === idProducto ? { ...p, ...producto, idProducto, sku } : p));
    escribirMock(productos);
    return productos.find((p) => p.idProducto === idProducto);
  }
}

export async function deleteProducto(idProducto) {
  try {
    await apiClient.delete(`/productos/${idProducto}`);
  } catch {
    escribirMock(obtenerMock().filter((p) => p.idProducto !== idProducto));
  }
}
