// Pub/sub mínimo para disparar el toast desde cualquier panel sin pasar
// callbacks por props (equivalente a la función global toast() de nav.js).
const listeners = new Set();

export function showToast(mensaje) {
  listeners.forEach((fn) => fn(mensaje));
}

export function subscribeToast(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
