// Base para hablar con el backend Spring Boot. Centraliza la URL, el token de
// sesion y el manejo de errores para que cada *Api.js solo arme el endpoint y
// el body.
import { useSesion } from '../auth/sesionStore';
import { useClienteStore } from '../tienda/clienteStore';

const BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

/** URL absoluta de un endpoint (para abrir el PDF o el XML en el navegador). */
export function apiUrl(path) {
  if (!path) return BASE_URL;
  const p = path.startsWith('/') ? path : `/${path}`;
  return `${BASE_URL}${p}`;
}

// El token vive en los stores (admin o comprador). Se lee aquí para que cada
// llamada salga con "Authorization: Bearer ..." sin repetirlo en cada vista.
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

  const p = path?.startsWith('/') ? path : `/${path || ''}`;
  const respuesta = await fetch(`${BASE_URL}${p}`, {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!respuesta.ok) {
    const detalle = await mensajeDeError(respuesta);
    const error = new Error(detalle || `Error ${respuesta.status} al llamar ${method} ${path}`);
    // El status permite a las vistas reaccionar a 401/403 (sesión vencida).
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

  const p = path?.startsWith('/') ? path : `/${path || ''}`;
  const respuesta = await fetch(`${BASE_URL}${p}`, { headers });

  if (!respuesta.ok) {
    const detalle = await mensajeDeError(respuesta);
    const error = new Error(detalle || `Error ${respuesta.status} al descargar ${path}`);
    error.status = respuesta.status;
    throw error;
  }

  // Equivalente a responseType: 'blob' de axios, con fetch nativo.
  return respuesta.blob();
}

export const apiClient = {
  get: (path, options) => request(path, options),
  getBlob: (path, options) => requestBlob(path, options),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};
