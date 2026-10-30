// Turno de caja del POS. La tienda y el vendedor los resuelve el backend
// desde el token: aquí solo viajan el fondo inicial y el arqueo fisico.
import { apiClient } from './client';

/** Turno abierto del usuario, el ultimo cierre del dia o `SIN_TURNO`. */
export function obtenerCaja() {
  return apiClient.get('/caja');
}

export function abrirCaja(montoApertura) {
  return apiClient.post('/caja/apertura', { montoApertura });
}

export function cerrarCaja({ efectivoContado, observacion = '' }) {
  return apiClient.post('/caja/cierre', { efectivoContado, observacion });
}
