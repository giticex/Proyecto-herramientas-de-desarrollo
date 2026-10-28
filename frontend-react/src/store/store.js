// Store central: porta frontend/js/store.js a zustand. Expone la marca
// (tienda) y los productos con los mismos nombres de acción que el store viejo.
import { create } from 'zustand';
import { getTienda as getTiendaApi, saveTienda as saveTiendaApi, TIENDA_DEFAULT } from '../api/tiendaApi';
import {
  listProductos as listProductosApi,
  listProductosTienda as listProductosTiendaApi,
  createProducto as createProductoApi,
  updateProducto as updateProductoApi,
  deleteProducto as deleteProductoApi,
} from '../api/productosApi';

const UMBRAL_KEY = 'umbralStock';

function leerUmbral() {
  try {
    const guardado = localStorage.getItem(UMBRAL_KEY);
    return guardado ? Number(guardado) : 5;
  } catch {
    return 5;
  }
}

export const useStore = create((set, get) => ({
  tienda: { ...TIENDA_DEFAULT },
  productos: [],
  umbral: leerUmbral(),

  setTienda(tienda) {
    set({ tienda });
  },

  async getTienda(slug = get().tienda.slug) {
    const tienda = await getTiendaApi(slug);
    set({ tienda });
    return tienda;
  },

  async saveTienda(tienda) {
    const guardada = await saveTiendaApi(tienda);
    set({ tienda: guardada });
    return guardada;
  },

  async listProductos(slug) {
    const respuesta = slug
      ? await listProductosTiendaApi(slug)
      : await listProductosApi();
    let datos = respuesta;
    if (respuesta && !Array.isArray(respuesta)) {
      datos = respuesta.data ?? respuesta.content ?? respuesta.items ?? respuesta.productos ?? [];
    }
    if (!Array.isArray(datos)) datos = [];
    const productos = datos.map((p) => ({
      idProducto: p.idProducto ?? p.id ?? p.id_producto ?? '',
      codigo: p.codigo ?? p.cod ?? p.code ?? '',
      nombre: p.nombre ?? p.descripcion ?? p.name ?? '',
      categoria: p.categoria ?? p.rubro ?? p.categoriaProducto ?? '',
      precio: Number(p.precio ?? p.precioVenta ?? p.valor ?? p.price ?? 0),
      stock: Number(p.stock ?? p.cantidad ?? p.existencias ?? p.cant ?? 0),
      atributos: p.atributos ?? {},
      unidadMedida: p.unidadMedida,
      afectacionIgv: p.afectacionIgv,
      idTienda: p.idTienda,
    }));
    set({ productos });
    return productos;
  },

  async createProducto(producto) {
    const nuevo = await createProductoApi(producto);
    const mapeado = {
      idProducto: nuevo?.idProducto ?? nuevo?.id ?? nuevo?.id_producto ?? producto.idProducto,
      codigo: nuevo?.codigo ?? nuevo?.cod ?? producto.codigo ?? '',
      nombre: nuevo?.nombre ?? nuevo?.descripcion ?? producto.nombre ?? '',
      categoria: nuevo?.categoria ?? nuevo?.rubro ?? producto.categoria ?? '',
      precio: Number(nuevo?.precio ?? nuevo?.precioVenta ?? producto.precio ?? 0),
      stock: Number(nuevo?.stock ?? nuevo?.cantidad ?? producto.stock ?? 0),
      atributos: nuevo?.atributos ?? producto.atributos ?? {},
      unidadMedida: nuevo?.unidadMedida ?? producto.unidadMedida,
      afectacionIgv: nuevo?.afectacionIgv ?? producto.afectacionIgv,
      idTienda: nuevo?.idTienda ?? producto.idTienda,
    };
    set((estado) => ({ productos: [...estado.productos, mapeado] }));
    return mapeado;
  },

  async updateProducto(idProducto, producto) {
    const actualizado = await updateProductoApi(idProducto, producto);
    const mapeado = {
      idProducto: actualizado?.idProducto ?? actualizado?.id ?? idProducto,
      codigo: actualizado?.codigo ?? actualizado?.cod ?? producto.codigo ?? '',
      nombre: actualizado?.nombre ?? actualizado?.descripcion ?? producto.nombre ?? '',
      categoria: actualizado?.categoria ?? actualizado?.rubro ?? producto.categoria ?? '',
      precio: Number(actualizado?.precio ?? actualizado?.precioVenta ?? producto.precio ?? 0),
      stock: Number(actualizado?.stock ?? actualizado?.cantidad ?? producto.stock ?? 0),
      atributos: actualizado?.atributos ?? producto.atributos ?? {},
      unidadMedida: actualizado?.unidadMedida ?? producto.unidadMedida,
      afectacionIgv: actualizado?.afectacionIgv ?? producto.afectacionIgv,
      idTienda: actualizado?.idTienda ?? producto.idTienda,
    };
    set((estado) => ({
      productos: estado.productos.map((p) => (p.idProducto == idProducto ? mapeado : p)),
    }));
    return mapeado;
  },

  async deleteProducto(idProducto) {
    await deleteProductoApi(idProducto);
    set((estado) => ({ productos: estado.productos.filter((p) => p.idProducto !== idProducto) }));
  },

  getUmbral() {
    return get().umbral;
  },

  setUmbral(n) {
    localStorage.setItem(UMBRAL_KEY, String(n));
    set({ umbral: n });
  },
}));
