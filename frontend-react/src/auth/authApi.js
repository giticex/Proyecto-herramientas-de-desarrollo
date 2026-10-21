// TODO: backend aun no tiene POST /auth/login. Cuando exista, reemplazar el
// cuerpo de esta función por un fetch real { usuario, clave } -> { token } y
// solo guardar la sesión si responde 200 (igual que indica login.js viejo).
export async function loginApi(usuario) {
  await new Promise((resolve) => setTimeout(resolve, 150));
  const token = `demo-token-${Date.now()}`;
  return { usuario, token };
}
