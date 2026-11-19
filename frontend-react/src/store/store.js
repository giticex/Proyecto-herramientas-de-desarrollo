import { create } from 'zustand';
import {
  listProductos as listProductosApi,
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
  productos: [],
  umbral: leerUmbral(),

  async listProductos() {
    const productos = await listProductosApi();
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
