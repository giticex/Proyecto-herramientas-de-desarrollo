// Sesión del administrador. Igual que Store.login/isLogged/logout de
// frontend/js/store.js, pero guardando el token real y el rol que devolvió el
// backend (ADMIN o CLIENTE). El rol no se deduce acá: viene de la base.
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

export const useSesion = create((set) => ({
  usuario: sesionGuardada?.usuario ?? null,
  token: sesionGuardada?.token ?? null,
  rol: sesionGuardada?.rol ?? null,

  login(usuario, token, rol) {
    localStorage.setItem(KEY, JSON.stringify({ usuario, token, rol }));
    set({ usuario, token, rol });
  },

  logout() {
    localStorage.removeItem(KEY);
    set({ usuario: null, token: null, rol: null });
  },
}));

// Solo el ADMIN entra al panel de administración.
export function esAdmin(sesion) {
  return sesion?.rol === 'ADMIN';
}
