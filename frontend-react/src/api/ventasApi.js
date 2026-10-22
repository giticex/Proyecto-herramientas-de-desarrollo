// Historial de ventas del panel de administracion.
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

/** Ruta del PDF del comprobante (se descarga con Bearer token). */
export function rutaComprobantePdf(idVenta) {
  return `/comprobantes/${idVenta}/pdf`;
}

/** Ruta del XML UBL 2.1 del comprobante (se descarga con Bearer token). */
export function rutaComprobanteXml(idVenta) {
  return `/comprobantes/${idVenta}/xml`;
}
