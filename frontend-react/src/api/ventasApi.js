// Ventas: solo CLIENTE. idCliente lo resuelve el backend a partir del
// token, nunca se manda en el body. Sin fallback a mock: una compra real
// tiene que tocar el servidor (el stock se descuenta ahí).
import { apiClient } from './client';

export function registrarVenta(carrito, token) {
  return apiClient.post('/ventas', carrito, token);
}

export function misVentas(token) {
  return apiClient.get('/ventas/mias', token);
}
