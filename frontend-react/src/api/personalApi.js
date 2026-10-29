// Personal de la propia tienda. El backend resuelve la empresa desde el token
// del ADMIN: ningún método acepta idTienda, así el panel no puede apuntar a
// otra empresa aunque manipule la petición.
import { apiClient } from './client';

/** Listado de vendedores junto con el uso de puestos del plan contratado. */
export function obtenerPersonal() {
  return apiClient.get('/api/admin/personal');
}

export function crearPersonal({ usuario, nombre, apellido, correo, password }) {
  return apiClient.post('/api/admin/personal', {
    usuario,
    nombre,
    apellido,
    correo,
    password,
  });
}

export function actualizarPersonal(idUsuario, { usuario, nombre, apellido, correo, password }) {
  return apiClient.put(`/api/admin/personal/${encodeURIComponent(idUsuario)}`, {
    usuario,
    nombre,
    apellido,
    correo,
    password,
  });
}

export function cambiarEstadoPersonal(idUsuario, estado) {
  return apiClient.put(`/api/admin/personal/${encodeURIComponent(idUsuario)}/estado`, { estado });
}
