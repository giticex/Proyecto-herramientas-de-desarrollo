import { create } from 'zustand';

const KEY = 'adminSesion';

function leerSesion() {
  try {
    const guardada = localStorage.getItem(KEY);
    return guardada ? JSON.parse(guardada) : null;
  } catch {
    return null;
  }
}

const sesionGuardada = leerSesion();

export const useSesion = create((set, get) => ({
  usuario: sesionGuardada?.usuario ?? null,
  token: sesionGuardada?.token ?? null,
  rol: sesionGuardada?.rol ?? null,
  tienda: sesionGuardada?.tienda ?? null,

  login(usuario, token, rol, tienda = null) {
    const sesion = { usuario, token, rol, tienda };
    localStorage.setItem(KEY, JSON.stringify(sesion));
    set(sesion);
  },

  actualizarTienda(tienda) {
    const { usuario, token, rol, login } = get();
    if (token) login(usuario, token, rol, tienda);
  },

  logout() {
    localStorage.removeItem(KEY);
    set({ usuario: null, token: null, rol: null, tienda: null });
  },
}));

export function esAdmin(sesion) {
  return sesion?.rol === 'ADMIN';
}
