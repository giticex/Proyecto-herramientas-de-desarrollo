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

export function actualizarEstadoEmpresa(idTienda, estado) {
  return apiClient.put(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/estado`,
    { estado },
  );
}

export function obtenerPlanEmpresa(idTienda) {
  return apiClient.get(`/api/super-admin/empresas/${encodeURIComponent(idTienda)}/plan`);
}

export function actualizarPlanEmpresa(idTienda, { plan, limiteProductos, limiteUsuarios }) {
  return apiClient.put(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/plan`,
    { plan, limiteProductos, limiteUsuarios },
  );
}

export function listarUsuariosEmpresa(idTienda) {
  return apiClient.get(`/api/super-admin/empresas/${encodeURIComponent(idTienda)}/usuarios`);
}

export function crearUsuarioEmpresa(idTienda, datos) {
  return apiClient.post(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/usuarios`,
    datos,
  );
}

export function actualizarUsuarioEmpresa(idTienda, idUsuario, datos) {
  return apiClient.put(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/usuarios/${encodeURIComponent(idUsuario)}`,
    datos,
  );
}

export function cambiarEstadoUsuarioEmpresa(idTienda, idUsuario, estado) {
  return apiClient.put(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/usuarios/${encodeURIComponent(idUsuario)}/estado`,
    { estado },
  );
}

export function eliminarUsuarioEmpresa(idTienda, idUsuario) {
  return apiClient.delete(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/usuarios/${encodeURIComponent(idUsuario)}`,
  );
}

export function obtenerAdminEmpresa(idTienda) {
  return apiClient.get(`/api/super-admin/empresas/${encodeURIComponent(idTienda)}/admin`);
}

export function restablecerPasswordAdminEmpresa(idTienda, password) {
  return apiClient.put(
    `/api/super-admin/empresas/${encodeURIComponent(idTienda)}/admin/password`,
    { password },
  );
}

export function listarAuditoria({ pagina = 0, tamanio = 10, tipo = '', busqueda = '' } = {}) {
  const parametros = new URLSearchParams({
    page: String(pagina),
    size: String(tamanio),
  });
  if (tipo) parametros.set('tipo', tipo);
  const texto = busqueda.trim();
  if (texto) parametros.set('q', texto);
  return apiClient.get(`/api/super-admin/auditoria?${parametros.toString()}`);
}
