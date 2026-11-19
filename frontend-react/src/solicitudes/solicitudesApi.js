import { apiClient } from '../api/client';

const BASE = '/api/solicitudes';

export const solicitarTienda = (datos) => apiClient.post(BASE, datos, { token: '' });
export const consultarSolicitud = () => apiClient.get(BASE + '/mia');
export const enviarRespuesta = (mensaje) => apiClient.post(BASE + '/mia/respuesta', { mensaje });
export const listarSolicitudes = () => apiClient.get(BASE + '/maestro');
export const resolverSolicitud = (id, estado, mensaje) =>
  apiClient.put(BASE + '/maestro/' + encodeURIComponent(id), { estado, mensaje });
