export function coincidenciaBusquedaCatalogo(productos, busqueda) {
  const texto = busqueda.trim().toLowerCase();
  if (!texto) return () => true;

  const coincideSku = (producto) => producto.sku?.toLowerCase() === texto;
  if (productos.some(coincideSku)) return coincideSku;

  return (producto) => producto.nombre.toLowerCase().includes(texto)
    || Boolean(producto.categoria?.toLowerCase().includes(texto));
}
