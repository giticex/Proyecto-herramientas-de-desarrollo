// Base para hablar con el backend Spring Boot. Centraliza la URL y el manejo
// de errores para que cada *Api.js solo arme el endpoint y el body.
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/** URL absoluta de un endpoint (para abrir el PDF o el XML en el navegador). */
export function apiUrl(path) {
  return `${BASE_URL}${path}`;
}

async function leerMensajeError(respuesta) {
  try {
    const datos = await respuesta.json();
    return datos.error || datos.message || `Error ${respuesta.status}`;
  } catch {
    return `Error ${respuesta.status}`;
  }
}

async function request(path, { method = 'GET', body } = {}) {
  const respuesta = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!respuesta.ok) {
    // El backend responde {status, error}: se muestra ese mensaje al usuario.
    throw new Error(await leerMensajeError(respuesta));
  }

  if (respuesta.status === 204) return null;
  return respuesta.json();
}

export const apiClient = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
