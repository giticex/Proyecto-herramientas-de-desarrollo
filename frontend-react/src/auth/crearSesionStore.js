import { create } from 'zustand';

// Factory para no duplicar la lógica de sesión entre ADMIN y CLIENTE: cada
// rol tiene su propia clave de localStorage y su propio store, pero con la
// misma forma { usuario, token, login, logout }.
export function crearSesionStore(key) {
  function leer() {
    try {
      const guardada = localStorage.getItem(key);
      return guardada ? JSON.parse(guardada) : null;
    } catch {
      return null;
    }
  }

  const inicial = leer();

  return create((set) => ({
    usuario: inicial?.usuario ?? null,
    token: inicial?.token ?? null,

    login(usuario, token) {
      localStorage.setItem(key, JSON.stringify({ usuario, token }));
      set({ usuario, token });
    },

    logout() {
      localStorage.removeItem(key);
      set({ usuario: null, token: null });
    },
  }));
}
