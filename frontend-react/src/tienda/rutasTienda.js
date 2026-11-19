export function rutaTienda(slug) {
  return `/tienda/${encodeURIComponent(slug)}`;
}

export function destinoCliente(slug, destino) {
  const base = rutaTienda(slug);
  if (typeof destino !== 'string') return base;
  const pantalla = destino.split(/[?#]/, 1)[0].replace(/\/+$/, '');
  return ['', '/carrito', '/checkout', '/confirmacion', '/mis-compras'].some(
    (sufijo) => pantalla === base + sufijo
  ) ? destino : base;
}
