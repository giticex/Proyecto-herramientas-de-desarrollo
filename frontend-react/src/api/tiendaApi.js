// Tienda (marca): el backend TODAVIA no tiene /tiendas, así que estas llamadas
// siempre caen al mock de localStorage por ahora. El try/catch ya queda listo
// para cuando el endpoint exista: solo hay que quitar el TODO.
import { apiClient } from './client';

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

// TODO: backend aun no tiene GET /tiendas/{slug}. Cuando exista, este fetch
// dejará de fallar siempre y se podrá quitar el fallback a TIENDA_DEFAULT.
export async function getTienda(slug) {
  try {
    return await apiClient.get(`/tiendas/${slug}`);
  } catch {
    return leerMock() || { ...TIENDA_DEFAULT };
  }
}

// TODO: backend aun no tiene POST /tiendas (registro) ni PUT /tiendas/{slug} (marca).
export async function saveTienda(tienda) {
  try {
    return await apiClient.put(`/tiendas/${tienda.slug}`, tienda);
  } catch {
    escribirMock(tienda);
    return tienda;
  }
}
