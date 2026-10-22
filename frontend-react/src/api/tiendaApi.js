// Tienda (marca) y su catálogo público. GET /tiendas/{slug} y
// GET /tiendas/{slug}/productos son públicos (sin token). PUT /tiendas/{slug}
// es solo-ADMIN y requiere el token de quien es dueño de esa tienda.
// Solo cae al mock si el backend es inalcanzable (status 0); cualquier otra
// respuesta real (404, 401, 403...) se re-lanza tal cual.
import { apiClient } from './client';
import { ApiError } from './ApiError';

const KEY = 'tienda';

export const TIENDA_DEFAULT = {
  nombre: 'Mi Tienda',
  rubro: 'moda',
  slug: 'mi-tienda',
  colorPrimario: '#0071e3',
  colorSecundario: '#1d1d1f',
  logoUrl: '',
};

function leerMock() {
  try {
    const guardada = localStorage.getItem(KEY);
    return guardada ? JSON.parse(guardada) : null;
  } catch {
    return null;
  }
}

function escribirMock(tienda) {
  localStorage.setItem(KEY, JSON.stringify(tienda));
}

function esFalloDeConexion(error) {
  return error instanceof ApiError && error.status === 0;
}

export async function getTienda(slug) {
  try {
    return await apiClient.get(`/tiendas/${slug}`);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    return leerMock() || { ...TIENDA_DEFAULT };
  }
}

export async function getProductosDeTienda(slug) {
  try {
    return await apiClient.get(`/tiendas/${slug}/productos`);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    return [];
  }
}

export async function saveTienda(tienda, token) {
  try {
    return await apiClient.put(`/tiendas/${tienda.slug}`, tienda, token);
  } catch (error) {
    if (!esFalloDeConexion(error)) throw error;
    escribirMock(tienda);
    return tienda;
  }
}
