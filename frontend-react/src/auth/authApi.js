// Login real contra POST /auth/login. El backend decide el rol de la cuenta
// (ADMIN o CLIENTE) y lo devuelve junto con el token; el frontend solo lo
// guarda y lo revisa antes de dejar entrar a cada zona.
import { apiClient } from '../api/client';

// Devuelve { token, usuario, rol, nombreRol, idCliente, nombre, ... }.
// Si las credenciales no coinciden, apiClient lanza el error con el mensaje
// que devuelve el backend (401).
export async function loginApi(usuario, clave) {
  return apiClient.post('/auth/login', { usuario, clave });
}
