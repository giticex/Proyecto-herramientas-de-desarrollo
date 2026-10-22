// Login y registro reales contra el backend. Sin fallback a localStorage:
// un login simulado por defecto dejaría pasar a cualquiera. Para demos sin
// backend, exporta VITE_USE_MOCK=true (por defecto false).
import { apiClient } from '../api/client';
import { mockLogin, mockRegistroTienda, mockRegistroCliente } from './authMock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

// rolEsperado es solo para el mock (el backend real decide el rol según en
// qué tabla encuentra la cuenta, usuario o cliente).
export async function login(usuario, contrasena, rolEsperado) {
  if (USE_MOCK) return mockLogin(usuario, rolEsperado);
  return apiClient.post('/auth/login', { usuario, contrasena });
}

export async function registroTienda(datos) {
  if (USE_MOCK) return mockRegistroTienda(datos);
  return apiClient.post('/auth/registro-tienda', datos);
}

export async function registroCliente(datos) {
  if (USE_MOCK) return mockRegistroCliente(datos);
  return apiClient.post('/auth/registro-cliente', datos);
}
