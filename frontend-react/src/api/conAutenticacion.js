import { ApiError } from './ApiError';
import { showToast } from '../components/toastBus';

// Envuelve una llamada que necesita sesión: si el backend responde 401
// (token vencido o inválido), cierra esa sesión y avisa. El redirect al
// login lo hace RutaProtegida solo con que el token desaparezca del store,
// no hace falta navegar manualmente aquí.
export async function conAutenticacion(llamada, logout) {
  try {
    return await llamada();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      logout();
      showToast('Tu sesión expiró, inicia sesión de nuevo.');
    }
    throw error;
  }
}
