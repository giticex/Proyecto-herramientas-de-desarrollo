// Corte de caja del administrador. El backend resuelve la tienda desde el
// token; aquí solo se envían el periodo y la fecha de referencia.
import { apiClient } from './client';

/**
 * @param {{periodo?: 'diario' | 'mensual', fecha?: string}} opciones
 *   fecha en formato ISO (yyyy-MM-dd); en el periodo mensual elige el mes.
 */
export function obtenerArqueo({ periodo = 'diario', fecha } = {}) {
  const parametros = new URLSearchParams({ periodo });
  if (fecha) parametros.set('fecha', fecha);
  return apiClient.get(`/admin/arqueo?${parametros.toString()}`);
}

/** Ruta autenticada del reporte CSV o PDF para descargar con el token. */
export function rutaExportacionArqueo({ periodo = 'diario', fecha, formato = 'csv' } = {}) {
  const parametros = new URLSearchParams({ periodo, formato });
  if (fecha) parametros.set('fecha', fecha);
  return `/admin/arqueo/export?${parametros.toString()}`;
}
