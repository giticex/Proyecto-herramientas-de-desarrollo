// Traduce un error de login/registro (lanzado por api/client.js, con
// .status cuando el backend respondió) a un mensaje claro en español.
export function mensajeLogin(problema) {
  const status = problema?.status;

  if (!status) {
    return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
  }
  if (status === 400) {
    return 'Revisa los datos ingresados.';
  }
  if (status === 401) {
    return 'Usuario o contraseña incorrectos.';
  }
  if (status === 403) {
    return 'No tienes permisos para entrar aquí.';
  }
  if (status >= 500) {
    return 'El servidor tuvo un problema. Inténtalo de nuevo en unos minutos.';
  }
  return problema.message || 'No se pudo iniciar sesión.';
}

export const MENSAJE_SIN_ROL_ADMIN = 'Tu cuenta no tiene permisos de administrador para entrar al panel.';
export const MENSAJE_SIN_ROL_CLIENTE = 'Tu cuenta no es de comprador. Inicia sesión con una cuenta de cliente.';
