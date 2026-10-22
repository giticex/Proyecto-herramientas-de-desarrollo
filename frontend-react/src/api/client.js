// Base para hablar con el backend Spring Boot. Centraliza la URL, el token de
// sesion y el manejo de errores para que cada *Api.js solo arme el endpoint y
// el body.
import { useSesion } from '../auth/sesionStore';
import { useClienteStore } from '../tienda/clienteStore';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

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

  const respuesta = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!respuesta.ok) {
    const detalle = await mensajeDeError(respuesta);
    throw new Error(detalle || `Error ${respuesta.status} al llamar ${method} ${path}`);
  }

  if (respuesta.status === 204) return null;
  return respuesta.json();
}

export const apiClient = {
  get: (path, options) => request(path, options),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};
