// Resumen del dashboard del administrador. El backend acota los datos a la
// tienda firmada en el token; aquí solo se pide el endpoint.
import { apiClient } from './client';

export function obtenerResumenDashboard() {
  return apiClient.get('/api/dashboard/resumen');
}
