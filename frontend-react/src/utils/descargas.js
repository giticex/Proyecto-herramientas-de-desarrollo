// Descarga autenticada de archivos del backend (PDF/XML de comprobantes).
//
// Los endpoints de archivos exigen el Bearer token, asi que no se puede usar
// un <a href> directo: se pide el blob con el cliente HTTP (que inyecta el
// token) y recien despues se abre o se descarga el archivo local.
import { apiClient } from '../api/client';

function descargarEnlace(url, nombreArchivo) {
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
}

/**
 * Descarga un archivo del backend usando el token de la sesion activa.
 *
 * @param {string} ruta ruta relativa del endpoint (ej. /comprobantes/123/pdf)
 * @param {string} tipo extension del archivo (PDF, XML)
 * @param {string} nombre nombre del archivo sin extension
 * @param {{abrir?: boolean}} opciones abrir=true muestra el blob en otra pestana
 */
export async function descargarArchivoSeguro(ruta, tipo, nombre = 'comprobante', { abrir = false } = {}) {
  const blob = await apiClient.getBlob(ruta);
  const url = window.URL.createObjectURL(blob);
  const nombreArchivo = `${nombre || 'comprobante'}.${tipo.toLowerCase()}`;

  if (abrir) {
    const ventana = window.open(url, '_blank');
    if (!ventana) {
      descargarEnlace(url, nombreArchivo);
    }
  } else {
    descargarEnlace(url, nombreArchivo);
  }

  // El navegador necesita la URL viva hasta que termina la descarga/visa.
  setTimeout(() => window.URL.revokeObjectURL(url), 60000);
}
