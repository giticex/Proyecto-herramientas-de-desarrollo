// Productos del panel de administrador: GET/POST/PUT/DELETE /productos son
// solo-ADMIN y requieren el token (ver store.js). Si el backend no responde
// (falla de conexión, status 0) cae al mock de localStorage; un 401/403/404
// real del backend NO se tapa con el mock: se deja pasar para que
// conAutenticacion y la UI lo manejen.
import { apiClient } from './client';
import { ApiError } from './ApiError';

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

// Solo cae al mock si el backend es inalcanzable; cualquier otra respuesta
// (401, 403, 404...) se re-lanza tal cual.
function esFalloDeConexion(error) {
  return error instanceof ApiError && error.status === 0;
}

export async function listProductos(token) {
  try {
    return await apiClient.get('/productos', token);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    return obtenerMock();
  }
}

export async function createProducto(producto, token) {
  try {
    return await apiClient.post('/productos', producto, token);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    const productos = obtenerMock();
    const siguienteId = productos.reduce((max, p) => Math.max(max, p.idProducto), 0) + 1;
    const nuevo = { ...producto, idProducto: producto.idProducto || siguienteId, atributos: producto.atributos || {} };
    escribirMock([...productos, nuevo]);
    return nuevo;
  }
}

export async function updateProducto(idProducto, producto, token) {
  try {
    return await apiClient.put(`/productos/${idProducto}`, producto, token);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    const productos = obtenerMock().map((p) => (p.idProducto === idProducto ? { ...p, ...producto, idProducto } : p));
    escribirMock(productos);
    return productos.find((p) => p.idProducto === idProducto);
  }
}

export async function deleteProducto(idProducto, token) {
  try {
    await apiClient.delete(`/productos/${idProducto}`, token);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    escribirMock(obtenerMock().filter((p) => p.idProducto !== idProducto));
  }
}
