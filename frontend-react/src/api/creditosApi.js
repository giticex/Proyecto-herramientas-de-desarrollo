// Créditos a favor generados por devoluciones (panel de administración).
import { apiClient } from './client';

/** Lista todos los créditos generados, del más reciente al más antiguo. */
export function listarCreditos() {
  return apiClient.get('/creditos');
}

/**
 * Consulta el saldo de un crédito por su código (BOL-CR-XXXXXX).
 * @param {string} codigo
 */
export function buscarCredito(codigo) {
  return apiClient.get(`/creditos/codigo/${encodeURIComponent(codigo.trim())}`);
}

/**
 * Canjea (total o parcialmente) el saldo de un crédito.
 * @param {{codigo: string, monto: number|string}} canje
 */
export function canjearCredito({ codigo, monto }) {
  return apiClient.post('/creditos/canjear', { codigo, monto: Number(monto) });
}

/**
 * Marca el crédito como Reembolsado y deja su saldo disponible en cero.
 * @param {{codigo: string}} reembolso
 */
export function reembolsarCredito({ codigo }) {
  return apiClient.post('/creditos/reembolsar', { codigo });
}
