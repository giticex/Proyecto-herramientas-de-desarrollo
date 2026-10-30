// Busqueda y alta rapida de compradores desde el punto de venta.
import { apiClient } from './client';

/**
 * Busca un comprador por DNI (8 digitos) o RUC (11) dentro de la tienda del
 * usuario autenticado. La respuesta siempre es 200: si no hay coincidencias
 * devuelve `encontrado: false` para que el POS ofrezca registrarlo.
 *
 * @param {string} documento
 * @returns {Promise<{encontrado: boolean, idCliente: string|null,
 *   tipoDocumento: 'DNI'|'RUC', numeroDocumento: string, nombre: string,
 *   direccion: string, telefono: string, correo: string,
 *   origen: 'TIENDA'|'HISTORIAL'|'REGISTRADO'|'NINGUNO'}>}
 */
export function buscarClientePorDocumento(documento) {
  const parametros = new URLSearchParams({ documento: documento.trim() });
  return apiClient.get(`/clientes/buscar?${parametros.toString()}`);
}

/**
 * Registra una ficha rapida para una venta de mostrador. La tienda sale del
 * token; solo se envian documento, nombre, direccion y telefono.
 */
export function registrarClienteRapido({ documento, nombre, direccion = '', telefono = '' }) {
  return apiClient.post('/clientes', { documento, nombre, direccion, telefono });
}
