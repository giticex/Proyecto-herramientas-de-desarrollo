import { useEffect, useState } from 'react';
import { listProductosTienda } from '../api/productosApi';

const SIN_PRODUCTOS = [];

export function useCatalogoTienda(slug) {
  const [respuesta, setRespuesta] = useState(null);

  useEffect(() => {
    if (!slug) return undefined;
    let vigente = true;

    listProductosTienda(slug).then(
      (productos) => {
        if (vigente) setRespuesta({ slug, productos, estado: 'listo' });
      },
      () => {
        if (vigente) setRespuesta({ slug, productos: SIN_PRODUCTOS, estado: 'error' });
      }
    );

    return () => { vigente = false; };
  }, [slug]);

  return respuesta?.slug === slug
    ? respuesta
    : { productos: SIN_PRODUCTOS, estado: 'cargando' };
}
