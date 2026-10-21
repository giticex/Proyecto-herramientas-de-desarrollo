// Misma fórmula de textoSobre() de frontend/js/store.js: luminancia del color
// primario para elegir texto claro u oscuro encima.
export function textoSobre(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = n >> 16;
  const g = (n >> 8) & 255;
  const b = n & 255;
  return 0.299 * r + 0.587 * g + 0.114 * b > 160 ? '#1d1d1f' : '#ffffff';
}
