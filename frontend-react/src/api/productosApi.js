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

export async function listProductos() {
  try {
    return await apiClient.get('/productos');
  } catch {
    return obtenerMock();
  }
}

export async function listProductosTienda(slug) {
  try {
    return await apiClient.get(`/tiendas/${encodeURIComponent(slug)}/productos`);
  } catch {
    return obtenerMock();
  }
}

export async function createProducto(producto) {
  try {
    return await apiClient.post('/productos', producto);
  } catch {
    const productos = obtenerMock();
    const siguienteId = productos.reduce((max, p) => Math.max(max, p.idProducto), 0) + 1;
    const nuevo = { ...producto, idProducto: producto.idProducto || siguienteId, atributos: producto.atributos || {} };
    escribirMock([...productos, nuevo]);
    return nuevo;
  }
}

export async function updateProducto(idProducto, producto) {
  try {
    return await apiClient.put(`/productos/${idProducto}`, producto);
  } catch {
    const productos = obtenerMock().map((p) => (p.idProducto === idProducto ? { ...p, ...producto, idProducto } : p));
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
