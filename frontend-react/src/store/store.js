// Store central: porta frontend/js/store.js a zustand. Expone la marca
// (tienda) y los productos con los mismos nombres de acción que el store viejo.
import { create } from 'zustand';
import { getTienda as getTiendaApi, saveTienda as saveTiendaApi, getProductosDeTienda, TIENDA_DEFAULT } from '../api/tiendaApi';
import {
  listProductos as listProductosApi,
  createProducto as createProductoApi,
  updateProducto as updateProductoApi,
  deleteProducto as deleteProductoApi,
} from '../api/productosApi';
import { conAutenticacion } from '../api/conAutenticacion';
import { useSesion } from '../auth/sesionStore';

const UMBRAL_KEY = 'umbralStock';
const TIENDA_ADMIN_KEY = 'tiendaAdmin';

function leerUmbral() {
  try {
    const guardado = localStorage.getItem(UMBRAL_KEY);
    return guardado ? Number(guardado) : 5;
  } catch {
    return 5;
  }
}

// La tienda del admin se cachea aparte de la sesión para sobrevivir un
// F5 sin tener que volver a pedirla (el panel usa solo la tienda del token).
function leerTiendaAdmin() {
  try {
    const guardada = localStorage.getItem(TIENDA_ADMIN_KEY);
    return guardada ? JSON.parse(guardada) : null;
  } catch {
    return null;
  }
}

// Las acciones de /productos y PUT /tiendas son solo-ADMIN: toman el token
// de la sesión admin y, si el backend responde 401, cierran esa sesión.
function conSesionAdmin(llamada) {
  const { token, logout } = useSesion.getState();
  return conAutenticacion(() => llamada(token), logout);
}

export const useStore = create((set, get) => ({
  tienda: leerTiendaAdmin() || { ...TIENDA_DEFAULT },
  productos: [],
  umbral: leerUmbral(),

  // Publico: lee la marca de cualquier tienda por su slug.
  async getTienda(slug = get().tienda.slug) {
    const tienda = await getTiendaApi(slug);
    set({ tienda });
    return tienda;
  },

  // El panel admin usa esto justo despues de login/registro: la tienda ya
  // vino con el token, no hace falta volver a pedirla por slug.
  setTienda(tienda) {
    localStorage.setItem(TIENDA_ADMIN_KEY, JSON.stringify(tienda));
    set({ tienda });
  },

  async saveTienda(tienda) {
    const guardada = await conSesionAdmin((token) => saveTiendaApi(tienda, token));
    localStorage.setItem(TIENDA_ADMIN_KEY, JSON.stringify(guardada));
    set({ tienda: guardada });
    return guardada;
  },

  // Panel admin: productos de la tienda del token.
  async listProductos() {
    const productos = await conSesionAdmin((token) => listProductosApi(token));
    set({ productos });
    return productos;
  },

  // Publico: catalogo de una tienda por su slug (/tienda/:slug).
  async listProductosTienda(slug) {
    const productos = await getProductosDeTienda(slug);
    set({ productos });
    return productos;
  },

  async createProducto(producto) {
    const nuevo = await conSesionAdmin((token) => createProductoApi(producto, token));
    set((estado) => ({ productos: [...estado.productos, nuevo] }));
    return nuevo;
  },

  async updateProducto(idProducto, producto) {
    const actualizado = await conSesionAdmin((token) => updateProductoApi(idProducto, producto, token));
    set((estado) => ({
      productos: estado.productos.map((p) => (p.idProducto === idProducto ? actualizado : p)),
    }));
    return actualizado;
  },

  async deleteProducto(idProducto) {
    await conSesionAdmin((token) => deleteProductoApi(idProducto, token));
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
