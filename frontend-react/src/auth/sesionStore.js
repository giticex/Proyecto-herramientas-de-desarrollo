// Sesión del administrador: { usuario: {id, correo, rol}, token }.
import { crearSesionStore } from './crearSesionStore';

export const useSesion = crearSesionStore('sesionAdmin');
