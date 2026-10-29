// Endpoints globales de la consola del SUPER_ADMIN. Todas las consultas
// eligen la empresa por idTienda: esta cuenta no pertenece a una tienda.
import { apiClient } from './client';

export function obtenerResumenGlobal() {
  return apiClient.get('/api/super-admin/resumen');
}

export function listarEmpresas() {
  return apiClient.get('/api/super-admin/empresas');
}

export function obtenerDashboardEmpresa(idTienda) {
  return apiClient.get(`/api/super-admin/empresas/${encodeURIComponent(idTienda)}/dashboard`);
}

export function listarProductosEmpresa(idTienda) {
  return apiClient.get(`/api/super-admin/empresas/${encodeURIComponent(idTienda)}/productos`);
}

export function listarComprobantesEmpresa(idTienda, { pagina = 0, tamanio = 8, busqueda = '' } = {}) {
  const parametros = new URLSearchParams({
    page: String(pagina),
    size: String(tamanio),
  });
  const filtro = busqueda.trim();
  if (filtro) parametros.set('q', filtro);
  return apiClient.get(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/comprobantes?${parametros.toString()}`,
  );
}
