// Solo para demos sin backend (VITE_USE_MOCK=true). Acepta cualquier
// credencial y devuelve una respuesta con la misma forma que el backend
// real ({ token, usuario, tienda }). Nunca se usa por defecto.
import { TIENDA_DEFAULT } from '../api/tiendaApi';

function token() {
  return `mock-token-${Date.now()}`;
}

export async function mockLogin(usuario, rolEsperado) {
  await new Promise((resolve) => setTimeout(resolve, 150));
  if (rolEsperado === 'CLIENTE') {
    return { token: token(), usuario: { id: 'mock-cliente', correo: usuario, rol: 'CLIENTE' } };
  }
  return {
    token: token(),
    usuario: { id: 'mock-admin', correo: usuario, rol: 'ADMIN' },
    tienda: { ...TIENDA_DEFAULT },
  };
}

export async function mockRegistroTienda(datos) {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return {
    token: token(),
    usuario: { id: 'mock-admin', correo: datos.correo, rol: 'ADMIN' },
    tienda: {
      nombre: datos.nombre,
      rubro: datos.rubro,
      slug: datos.slug,
      colorPrimario: TIENDA_DEFAULT.colorPrimario,
      colorSecundario: TIENDA_DEFAULT.colorSecundario,
      logoUrl: '',
    },
  };
}

export async function mockRegistroCliente(datos) {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return { token: token(), usuario: { id: 'mock-cliente', correo: datos.correo, rol: 'CLIENTE' } };
}
