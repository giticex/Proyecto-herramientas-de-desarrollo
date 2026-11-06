// Pub/sub mínimo para disparar el toast desde cualquier panel sin pasar
// callbacks por props (equivalente a la función global toast() de nav.js).
// tipo: 'info' | 'exito' | 'error'. Las llamadas con un solo argumento
// siguen funcionando (tipo por defecto 'info').
const listeners = new Set();

export function showToast(mensaje, tipo = 'info') {
  listeners.forEach((fn) => fn(mensaje, tipo));
}

export function subscribeToast(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
