import { apiClient } from './client';

export const TIENDA_DEFAULT = {
  nombre: 'Mi Tienda',
  rubro: 'moda',
  slug: 'mi-tienda',
  colorPrimario: '#0071e3',
  colorSecundario: '#1d1d1f',
  logoUrl: '',
};

export function getTienda(slug) {
  return apiClient.get(`/tiendas/${encodeURIComponent(slug)}`, { token: '' });
}

export function saveTienda(tienda) {
  return apiClient.put(`/tiendas/${encodeURIComponent(tienda.slug)}`, tienda);
}
