// Base para hablar con el backend Spring Boot. Centraliza la URL, el token
// y el manejo de errores para que cada *Api.js solo arme el endpoint y el body.
import { ApiError } from './ApiError';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function request(path, { method = 'GET', body, token } = {}) {
  let respuesta;
  try {
    respuesta = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    // Backend apagado, sin red, CORS, etc.
    throw new ApiError(0, 'No se pudo conectar con el servidor.');
  }

  if (!respuesta.ok) {
    throw new ApiError(respuesta.status, await mensajeDeError(respuesta));
  }

  if (respuesta.status === 204) return null;
  return respuesta.json();
}

async function mensajeDeError(respuesta) {
  try {
    const data = await respuesta.json();
    return data?.error || `Error ${respuesta.status}`;
  } catch {
    return `Error ${respuesta.status}`;
  }
}

export const apiClient = {
  get: (path, token) => request(path, { token }),
  post: (path, body, token) => request(path, { method: 'POST', body, token }),
  put: (path, body, token) => request(path, { method: 'PUT', body, token }),
  delete: (path, token) => request(path, { method: 'DELETE', token }),
};
