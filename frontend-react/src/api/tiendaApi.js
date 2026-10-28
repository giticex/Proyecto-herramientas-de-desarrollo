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

// Solo se recurre a los datos locales cuando el backend no responde o la
// tienda no existe (404). Un 401/403 se propaga: ocultarlo dejaria la app
// mostrando datos de una tienda que el backend no autoriza.
export async function getTienda(slug) {
  const targetSlug = slug || 'mi-tienda';
  try {
    if (targetSlug === 'mi-tienda') {
      return await apiClient.get('/tiendas/mi-tienda');
    }
    return await apiClient.get(`/tiendas/${encodeURIComponent(targetSlug)}`);
  } catch (error) {
    if (error?.status === 401 || error?.status === 403) throw error;
    const mock = leerMock();
    return mock || { ...TIENDA_DEFAULT, slug: targetSlug };
  }
}

// El PUT exige ADMIN: si falla, el error debe llegar al panel para avisar que
// la marca no se guardo (antes se simulaba un guardado exitoso).
export async function saveTienda(tienda) {
  return apiClient.put(`/tiendas/${tienda.slug}`, tienda);
}