// Ventas del panel de administración / punto de venta.
import { apiClient } from './client';

/**
 * Lista las ventas de la tienda del administrador autenticado.
 *
 * @param {{pagina?: number, tamanio?: number, busqueda?: string}} opciones
 * @returns {Promise<{contenido: Array, pagina: number, tamanio: number,
 *   totalElementos: number, totalPaginas: number}>}
 */
export function listarVentas({ pagina = 0, tamanio = 20, busqueda = '' } = {}) {
  const parametros = new URLSearchParams({
    page: String(pagina),
    size: String(tamanio),
  });
  const filtro = busqueda.trim();
  if (filtro) parametros.set('q', filtro);
  return apiClient.get(`/ventas?${parametros.toString()}`);
}

/**
 * Registra una venta desde el punto de venta o carrito.
 * @param {{tipoDocumento: string, medioPago?: string, idCliente?: string, items: Array<{idProducto: number, cantidad: number}>}} venta
 */
export async function registrarVenta(venta) {
  const { tokenActivo } = await import('./client');
  const token = tokenActivo?.();
  if (token) {
    return apiClient.post('/ventas', venta, { token });
  }
  return apiClient.post('/ventas', venta);
}

/**
 * Devuelve la venta con sus detalles y las unidades ya devueltas por línea,
 * para armar la devolución en el POS.
 * @param {string} idVenta
 */
export function obtenerVentaParaDevolucion(idVenta) {
  return apiClient.get(`/devoluciones/ventas/${encodeURIComponent(idVenta)}`);
}

/**
 * Registra una devolución de productos de una venta previa.
 * @param {{idVenta: string, motivo?: string, items: Array<{idProducto: number, cantidad: number}>}} devolucion
 */
export function registrarDevolucion(devolucion) {
  return apiClient.post('/devoluciones', devolucion);
}

/** Ruta del PDF del comprobante (se descarga con Bearer token). */
export function rutaComprobantePdf(idVenta) {
  return `/comprobantes/${idVenta}/pdf`;
}

/** Ruta del XML UBL 2.1 del comprobante (se descarga con Bearer token). */
export function rutaComprobanteXml(idVenta) {
  return `/comprobantes/${idVenta}/xml`;
}
