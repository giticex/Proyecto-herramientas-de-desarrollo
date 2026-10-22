import { create } from 'zustand';
import { showToast } from '../components/toastBus';

const CART_KEY = 'clienteCarrito';
const CLIENTE_SESION_KEY = 'clienteSesion';
const COMPRAS_KEY = 'clienteMisCompras';

function leerJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

export const useClienteStore = create((set, get) => ({
  carrito: leerJson(CART_KEY, []),
  cliente: leerJson(CLIENTE_SESION_KEY, null),
  misCompras: leerJson(COMPRAS_KEY, []),

  // --- SESIÓN DEL CLIENTE ---
  // La sesión guarda token y rol: el rol (CLIENTE) lo devuelve el backend y
  // es lo que Revisa RutaProtegidaCliente antes de abrir el checkout.
  loginCliente(datosCliente) {
    localStorage.setItem(CLIENTE_SESION_KEY, JSON.stringify(datosCliente));
    set({ cliente: datosCliente });
  },

  logoutCliente() {
    localStorage.removeItem(CLIENTE_SESION_KEY);
    set({ cliente: null });
  },

  // --- CARRITO CON VALIDACIÓN ESTRICTA DE STOCK CONTRA EL STORE ---
  agregarAlCarrito(producto, cantidad = 1) {
    const stockReal = Number(producto.stock ?? 0);
    if (stockReal <= 0) {
      showToast(`"${producto.nombre}" está agotado actualmente.`);
      return false;
    }

    const actual = get().carrito;
    const existente = actual.find((item) => item.idProducto === producto.idProducto);
    const cantidadEnCarrito = existente ? existente.cantidad : 0;

    if (cantidadEnCarrito + cantidad > stockReal) {
      showToast(`Stock máximo alcanzado (${stockReal} disponibles de ${producto.nombre}).`);
      return false;
    }

    let nuevoCarrito;
    if (existente) {
      nuevoCarrito = actual.map((item) =>
        item.idProducto === producto.idProducto
          ? { ...item, cantidad: item.cantidad + cantidad }
          : item
      );
    } else {
      nuevoCarrito = [
        ...actual,
        {
          idProducto: producto.idProducto,
          nombre: producto.nombre,
          precio: Number(producto.precio),
          categoria: producto.categoria,
          atributos: producto.atributos || {},
          cantidad,
        },
      ];
    }

    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
    showToast(`¡${producto.nombre} agregado al carrito!`);
    return true;
  },

  cambiarCantidad(idProducto, nuevaCantidad, stockDisponible) {
    if (nuevaCantidad <= 0) {
      get().quitarDelCarrito(idProducto);
      return;
    }
    if (nuevaCantidad > stockDisponible) {
      showToast(`Solo quedan ${stockDisponible} unidades en stock.`);
      return;
    }
    const nuevoCarrito = get().carrito.map((item) =>
      item.idProducto === idProducto ? { ...item, cantidad: nuevaCantidad } : item
    );
    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
  },

  quitarDelCarrito(idProducto) {
    const nuevoCarrito = get().carrito.filter((item) => item.idProducto !== idProducto);
    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
  },

  vaciarCarrito() {
    localStorage.removeItem(CART_KEY);
    set({ carrito: [] });
  },

  registrarCompra(orden) {
    const nuevasCompras = [orden, ...get().misCompras];
    localStorage.setItem(COMPRAS_KEY, JSON.stringify(nuevasCompras));
    localStorage.removeItem(CART_KEY);
    set({ misCompras: nuevasCompras, carrito: [] });
  },
}));
