import { apiClient } from './client';

export async function listProductos(filtro = '') {
  const token = (await import('./client')).tokenActivo?.();
  // Si envían un filtro, lo añadimos como parámetro ?filtro=...
  const url = filtro ? `/productos?filtro=${encodeURIComponent(filtro)}` : '/productos';
  
  if (token) {
    return apiClient.get(url, { token });
  }
  return apiClient.get(url);
}

export async function listProductosTienda(slug, filtro = '') {
  const token = (await import('./client')).tokenActivo?.();
  const base = `/tiendas/${encodeURIComponent(slug)}/productos`;
  const url = filtro ? `${base}?filtro=${encodeURIComponent(filtro)}` : base;

  if (token) {
    return apiClient.get(url, { token });
  }
  return apiClient.get(url);
}

export async function createProducto(producto) {
  const token = (await import('./client')).tokenActivo?.();
  const payload = { ...producto, idTienda: producto?.idTienda ?? 1 };
  if (token) {
    return apiClient.post('/productos', payload, { token });
  }
  return apiClient.post('/productos', payload);
}

export async function updateProducto(idProducto, producto) {
  const token = (await import('./client')).tokenActivo?.();
  const payload = { ...producto, idTienda: producto?.idTienda ?? 1 };
  if (token) {
    return apiClient.put(`/productos/${idProducto}`, payload, { token });
  }
  return apiClient.put(`/productos/${idProducto}`, payload);
}

export async function deleteProducto(idProducto) {
  const token = (await import('./client')).tokenActivo?.();
  if (token) {
    return apiClient.delete(`/productos/${idProducto}`, { token });
  }
  return apiClient.delete(`/productos/${idProducto}`);
}