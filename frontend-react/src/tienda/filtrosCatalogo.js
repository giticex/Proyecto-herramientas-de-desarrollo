export function esAtributoMarca(clave) {
  return clave.trim().toLowerCase() === 'marca';
}

function marcaDe(producto) {
  const atributo = Object.entries(producto.atributos ?? {}).find(
    ([clave, valor]) => esAtributoMarca(clave) && String(valor ?? '').trim(),
  );
  return atributo ? String(atributo[1]).trim() : '';
}

export function opcionesDeMarca(productos) {
  const marcas = new Map();
  for (const producto of productos) {
    const marca = marcaDe(producto);
    const valor = marca.toLowerCase();
    if (marca && !marcas.has(valor)) marcas.set(valor, marca);
  }
  return Array.from(marcas, ([valor, etiqueta]) => ({ valor, etiqueta }))
    .sort((a, b) => a.etiqueta.localeCompare(b.etiqueta, 'es'));
}

export function errorRangoPrecio(desde, hasta) {
  if ([desde, hasta].some((valor) => valor !== '' && (!Number.isFinite(Number(valor)) || Number(valor) < 0))) {
    return 'Ingresa precios válidos desde S/ 0.';
  }
  if (desde !== '' && hasta !== '' && Number(desde) > Number(hasta)) {
    return 'El precio mínimo debe ser menor o igual al máximo.';
  }
  return '';
}

export function coincidenciaMarcaYPrecio({ marca, desde, hasta }) {
  if (errorRangoPrecio(desde, hasta)) return () => false;

  const minimo = desde === '' ? -Infinity : Number(desde);
  const maximo = hasta === '' ? Infinity : Number(hasta);
  return (producto) => {
    const precio = Number(producto.precio);
    return (!marca || marcaDe(producto).toLowerCase() === marca)
      && precio >= minimo && precio <= maximo;
  };
}
