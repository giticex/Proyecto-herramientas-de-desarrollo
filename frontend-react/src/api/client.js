// Base para hablar con el backend Spring Boot. Centraliza la URL y el manejo
// de errores para que cada *Api.js solo arme el endpoint y el body.
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function request(path, { method = 'GET', body } = {}) {
  const respuesta = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!respuesta.ok) {
    throw new Error(`Error ${respuesta.status} al llamar ${method} ${path}`);
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
