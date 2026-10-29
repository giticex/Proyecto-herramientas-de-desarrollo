// Configuración comercial y de facturación de la propia tienda. El backend
// resuelve la empresa desde el token del ADMIN: ningún método acepta idTienda,
// así el panel no puede leer ni guardar la configuración de otra empresa.
import { apiClient } from './client';

export function obtenerConfiguracion() {
  return apiClient.get('/api/admin/configuracion');
}

export function guardarConfiguracion({
  razonSocial,
  ruc,
  direccionFiscal,
  telefono,
  logoUrl,
  serieBoleta,
  serieFactura,
}) {
  return apiClient.put('/api/admin/configuracion', {
    razonSocial,
    ruc,
    direccionFiscal,
    telefono,
    logoUrl,
    serieBoleta,
    serieFactura,
  });
}
