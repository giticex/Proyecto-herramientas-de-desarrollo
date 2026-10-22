// status 0 = no se pudo conectar (red caída, backend apagado). El resto son
// los status HTTP reales que devuelve el backend, con su mensaje en español.
export class ApiError extends Error {
  constructor(status, mensaje) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
  }
}
