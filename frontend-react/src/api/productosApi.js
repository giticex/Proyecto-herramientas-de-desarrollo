// Productos: siempre desde backend.
import { apiClient } from './client';

export async function listProductos() {
  const token = (await import('./client')).tokenActivo?.();
  if (token) {
    return apiClient.get('/productos', { token });
  }
  return apiClient.get('/productos');
}

export async function listProductosTienda(slug) {
  const token = (await import('./client')).tokenActivo?.();
  if (token) {
    return apiClient.get(`/tiendas/${encodeURIComponent(slug)}/productos`, { token });
  }
  return apiClient.get(`/tiendas/${encodeURIComponent(slug)}/productos`);
}

export async function createProducto(producto) {
  return apiClient.post('/productos', producto);
}

export async function updateProducto(idProducto, producto) {
  return apiClient.put(`/productos/${idProducto}`, producto);
}

export async function deleteProducto(idProducto) {
  return apiClient.delete(`/productos/${idProducto}`);
}
