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
    const productos = slug
      ? await listProductosTiendaApi(slug)
      : await listProductosApi();
    set({ productos });
    return productos;
  },

  async createProducto(producto) {
    const nuevo = await createProductoApi(producto);
    set((estado) => ({ productos: [...estado.productos, nuevo] }));
    return nuevo;
  },

  async updateProducto(idProducto, producto) {
    const actualizado = await updateProductoApi(idProducto, producto);
    set((estado) => ({
      productos: estado.productos.map((p) => (p.idProducto === idProducto ? actualizado : p)),
    }));
    return actualizado;
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
