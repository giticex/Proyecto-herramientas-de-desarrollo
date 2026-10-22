import { create } from 'zustand';
import { showToast } from '../components/toastBus';
import { registrarVenta as registrarVentaApi, misVentas as misVentasApi } from '../api/ventasApi';
import { conAutenticacion } from '../api/conAutenticacion';

const CART_KEY = 'clienteCarrito';
const SESION_KEY = 'sesionCliente';

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
  cliente: leerJson(SESION_KEY, null),
  misCompras: [],

  // --- SESIÓN DEL CLIENTE ---
  // respuesta: { token, usuario: {id, correo, rol} } (forma real de
  // POST /auth/login y /auth/registro-cliente). nombre es opcional: solo lo
  // conocemos cuando lo acabamos de capturar en el formulario de registro,
  // el backend no lo devuelve en la respuesta.
  loginCliente(respuesta, nombre) {
    const sesion = {
      id: respuesta.usuario.id,
      correo: respuesta.usuario.correo,
      rol: respuesta.usuario.rol,
      token: respuesta.token,
      nombre,
    };
    localStorage.setItem(SESION_KEY, JSON.stringify(sesion));
    set({ cliente: sesion });
  },

  logoutCliente() {
    localStorage.removeItem(SESION_KEY);
    set({ cliente: null, misCompras: [] });
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

  // --- COMPRAS (backend real, reemplaza el registrarCompra local) ---
  // Deja la excepcion (ApiError, p.ej. 409 sin stock) pasar: el checkout
  // decide como mostrarla y no vacia el carrito si falla.
  async registrarVenta(carrito) {
    const token = get().cliente?.token;
    const venta = await conAutenticacion(() => registrarVentaApi(carrito, token), get().logoutCliente);
    localStorage.removeItem(CART_KEY);
    set({ carrito: [] });
    return venta;
  },

  async cargarMisCompras() {
    const token = get().cliente?.token;
    const ventas = await conAutenticacion(() => misVentasApi(token), get().logoutCliente);
    set({ misCompras: ventas });
    return ventas;
  },
}));
