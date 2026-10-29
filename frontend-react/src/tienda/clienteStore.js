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

// El carrito sobrevive a recargas y a datos de pruebas viejas. Al leerlo se
// descartan entradas incompletas para que ningún producto residual llegue al
// checkout; la depuración contra el catálogo la hace sincronizarCarrito.
function leerCarrito() {
  const guardado = leerJson(CART_KEY, []);
  if (!Array.isArray(guardado)) return [];
  return guardado.filter(
    (item) =>
      item &&
      item.idProducto !== undefined &&
      item.idProducto !== null &&
      item.idProducto !== '' &&
      Number(item.cantidad) > 0
  );
}

export const useClienteStore = create((set, get) => ({
  carrito: leerCarrito(),
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
    const clave = String(producto.idProducto);
    const existente = actual.find((item) => String(item.idProducto) === clave);
    const cantidadEnCarrito = existente ? existente.cantidad : 0;

    if (cantidadEnCarrito + cantidad > stockReal) {
      showToast(`Stock máximo alcanzado (${stockReal} disponibles de ${producto.nombre}).`);
      return false;
    }

    let nuevoCarrito;
    if (existente) {
      nuevoCarrito = actual.map((item) =>
        String(item.idProducto) === clave
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

  cambiarCantidad(idProducto, nuevaCantidad, stockDisponible) {
    if (nuevaCantidad <= 0) {
      get().quitarDelCarrito(idProducto);
      return;
    }
    if (nuevaCantidad > stockDisponible) {
      showToast(`Solo quedan ${stockDisponible} unidades en stock.`);
      return;
    }
    const clave = String(idProducto);
    const nuevoCarrito = get().carrito.map((item) =>
      String(item.idProducto) === clave ? { ...item, cantidad: nuevaCantidad } : item
    );
    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
  },

  quitarDelCarrito(idProducto) {
    const clave = String(idProducto);
    const nuevoCarrito = get().carrito.filter((item) => String(item.idProducto) !== clave);
    localStorage.setItem(CART_KEY, JSON.stringify(nuevoCarrito));
    set({ carrito: nuevoCarrito });
  },

  // Reconcilia el carrito con el catálogo vigente antes de mostrar totales o
  // emitir un comprobante: quita productos que ya no existen o están agotados,
  // elimina duplicados y recorta cantidades al stock real. Devuelve el carrito
  // depurado para que quien valida use exactamente los items que se compran.
  sincronizarCarrito(productos) {
    const actual = get().carrito;
    if (!Array.isArray(actual) || actual.length === 0) return actual;
    if (!Array.isArray(productos) || productos.length === 0) return actual;

    const catalogo = new Map(productos.map((p) => [String(p.idProducto), p]));
    const porId = new Map();
    let huboCambios = false;

    actual.forEach((item) => {
      const clave = String(item?.idProducto ?? '');
      if (!clave) {
        huboCambios = true;
        return;
      }
      const producto = catalogo.get(clave);
      if (!producto) {
        huboCambios = true;
        return;
      }
      const stock = Number(producto.stock ?? 0);
      if (!Number.isFinite(stock) || stock <= 0) {
        huboCambios = true;
        return;
      }
      const cantidadPrevia = Number(porId.get(clave)?.cantidad) || 0;
      const cantidad = Math.max(1, Math.floor(Number(item.cantidad) || 0)) + cantidadPrevia;
      const cantidadFinal = Math.min(cantidad, stock);
      if (porId.has(clave) || cantidadFinal !== (Number(item.cantidad) || 0)) {
        huboCambios = true;
      }
      porId.set(clave, {
        ...item,
        idProducto: producto.idProducto ?? item.idProducto,
        nombre: producto.nombre ?? item.nombre,
        precio: Number(producto.precio ?? item.precio) || 0,
        afectacionIgv: producto.afectacionIgv ?? item.afectacionIgv,
        unidadMedida: producto.unidadMedida ?? item.unidadMedida,
        cantidad: cantidadFinal,
      });
    });

    const depurado = Array.from(porId.values());
    if (depurado.length !== actual.length) huboCambios = true;
    if (!huboCambios) return actual;

    localStorage.setItem(CART_KEY, JSON.stringify(depurado));
    set({ carrito: depurado });
    return depurado;
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
