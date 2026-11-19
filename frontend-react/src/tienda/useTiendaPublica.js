import { useCallback, useEffect, useState } from 'react';
import { getTienda } from '../api/tiendaApi';
import { listProductosTienda } from '../api/productosApi';

const SIN_PRODUCTOS = [];

export function useTiendaPublica(slug) {
  const [marca, setMarca] = useState(null);
  const [catalogo, setCatalogo] = useState(null);
  const [version, setVersion] = useState(0);
  const actualizarCatalogo = useCallback(() => setVersion((anterior) => anterior + 1), []);

  useEffect(() => {
    let vigente = true;
    getTienda(slug).then(
      (tienda) => { if (vigente) setMarca({ slug, tienda }); },
      (error) => { if (vigente) setMarca({ slug, error }); },
    );
    return () => { vigente = false; };
  }, [slug]);

  useEffect(() => {
    let vigente = true;
    listProductosTienda(slug).then(
      (productos) => { if (vigente) setCatalogo({ slug, productos }); },
      (error) => { if (vigente) setCatalogo({ slug, error }); },
    );
    return () => { vigente = false; };
  }, [slug, version]);

  const datos = marca?.slug === slug ? marca : null;
  const productos = catalogo?.slug === slug ? catalogo : null;

  return {
    tienda: datos?.tienda,
    errorTienda: datos?.error,
    productos: productos?.productos ?? SIN_PRODUCTOS,
    errorCatalogo: productos?.error,
    catalogoListo: Boolean(productos),
    actualizarCatalogo,
  };
}
