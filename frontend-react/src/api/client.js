import { useSesion } from '../auth/sesionStore';
import { useClienteStore } from '../tienda/clienteStore';

const BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '');
// Raiz del backend sin el sufijo /api: las rutas ya prefijadas (las que
// devuelven los helpers de descarga) se resuelven contra esta base.
const BASE_RAIZ = BASE_URL.replace(/\/api\/?$/, '');

/**
 * Normaliza una ruta a la URL absoluta del endpoint.
 *
 * Acepta rutas relativas con o sin el prefijo /api, y los alias historicos
 * /mi-tienda y /tiendas/mi-tienda del catalogo publico.
 */
function construirUrl(path) {
  if (!path) return BASE_URL;

  const limpio = path.startsWith('/') ? path : `/${path}`;
  if (limpio === '/mi-tienda' || limpio === '/tiendas/mi-tienda') {
    return `${BASE_RAIZ}/api/tiendas/mi-tienda`;
  }
  const ruta = limpio === '/api' || limpio.startsWith('/api/') ? limpio : `/api${limpio}`;
  return `${BASE_RAIZ}${ruta}`;
}

export function apiUrl(path) {
  return construirUrl(path);
}

export function tokenActivo() {
  const admin = useSesion.getState().token;
  if (admin) return admin;
  const comprador = useClienteStore.getState().cliente?.token;
  return comprador ?? null;
}

async function mensajeDeError(respuesta) {
  try {
    const cuerpo = await respuesta.json();
    return cuerpo?.error || cuerpo?.message || null;
  } catch {
    return null;
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const cabecera = token ?? tokenActivo();
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (cabecera) headers.Authorization = `Bearer ${cabecera}`;

  const respuesta = await fetch(construirUrl(path), {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!respuesta.ok) {
    const detalle = await mensajeDeError(respuesta);
    const error = new Error(detalle || `Error ${respuesta.status} al llamar ${method} ${path}`);
    error.status = respuesta.status;
    throw error;
  }

  if (respuesta.status === 204) return null;
  return respuesta.json();
}

async function requestBlob(path, { token } = {}) {
  const cabecera = token ?? tokenActivo();
  const headers = {};
  if (cabecera) headers.Authorization = `Bearer ${cabecera}`;

  const respuesta = await fetch(construirUrl(path), { headers });

  if (!respuesta.ok) {
    const detalle = await mensajeDeError(respuesta);
    const error = new Error(detalle || `Error ${respuesta.status} al descargar ${path}`);
    error.status = respuesta.status;
    throw error;
  }

  return respuesta.blob();
}

export const apiClient = {
  get: (path, options) => request(path, options),
  getBlob: (path, options) => requestBlob(path, options),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};
