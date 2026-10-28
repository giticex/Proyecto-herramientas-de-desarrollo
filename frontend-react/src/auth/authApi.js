// Login real contra POST /auth/login. El backend decide el rol de la cuenta
// (ADMIN o CLIENTE) y lo devuelve junto con el token; el frontend solo lo
// guarda y lo revisa antes de dejar entrar a cada zona.
import { apiClient } from '../api/client';

// Devuelve { token, usuario, rol, nombreRol, idCliente, nombre, ... }.
// Si las credenciales no coinciden, apiClient lanza el error con el mensaje
// que devuelve el backend (401).
//
// `tipo` acota la busqueda a una zona: 'CLIENTE' solo compradores y
// 'PERSONAL' solo cuentas del panel. Sin tipo se aceptan ambas.
export async function loginApi(usuario, clave, tipo) {
  const cuerpo = { usuario, clave };
  if (tipo) cuerpo.tipo = tipo;
  return apiClient.post('/auth/login', cuerpo);
}

export async function registrarTienda(datos) {
  return apiClient.post('/auth/registro-tienda', datos);
}

// Alta de comprador: devuelve el mismo LoginResponseDTO con token.
export async function registrarCliente(datos) {
  return apiClient.post('/auth/registro-cliente', datos);
}
