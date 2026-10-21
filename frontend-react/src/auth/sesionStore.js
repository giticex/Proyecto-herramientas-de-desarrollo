// Sesión del administrador. Igual que Store.login/isLogged/logout de
// frontend/js/store.js, pero guardando también un token (hoy falso).
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

  login(usuario, token) {
    localStorage.setItem(KEY, JSON.stringify({ usuario, token }));
    set({ usuario, token });
  },

  logout() {
    localStorage.removeItem(KEY);
    set({ usuario: null, token: null });
  },
}));
