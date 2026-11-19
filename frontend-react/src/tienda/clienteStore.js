import { create } from 'zustand';
import { showToast } from '../components/toastBus';

const CART_KEY = 'clienteCarrito';
const CLIENTE_SESION_KEY = 'clienteSesion';

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

  // El catálogo indica disponibilidad. Spring valida cantidades al emitir.
  agregarAlCarrito(producto, cantidad = 1) {
    if (!producto.disponible) {
      showToast(`"${producto.nombre}" está agotado actualmente.`);
      return false;
    }

    const actual = get().carrito;
    const existente = actual.find((item) => item.idProducto === producto.idProducto);
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
          // Datos fiscales del catalogo: solo se usan para la vista previa del
          // checkout, el backend vuelve a leerlos del catalogo al emitir.
          afectacionIgv: producto.afectacionIgv || '01',
          unidadMedida: producto.unidadMedida || 'NIU',
        },
      ];
    }

    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
    showToast(`¡${producto.nombre} agregado al carrito!`);
    return true;
  },

  cambiarCantidad(idProducto, nuevaCantidad) {
    if (nuevaCantidad <= 0) {
      get().quitarDelCarrito(idProducto);
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
}));
