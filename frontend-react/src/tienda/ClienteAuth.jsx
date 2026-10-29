import { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import { loginApi, registrarCliente } from '../auth/authApi';
import { useSesion } from '../auth/sesionStore';
import { showToast } from '../components/toastBus';
import './ClienteAuth.css';

export default function ClienteAuth() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { tienda, getTienda } = useStore();
  const { loginCliente, logoutCliente, cliente } = useClienteStore();
  const loginAdmin = useSesion((estado) => estado.login);
  const logoutAdmin = useSesion((estado) => estado.logout);

  // Si el checkout manda al login, vuelve a esa ruta al terminar. En cualquier
  // otro caso el comprador entra directo a su portal, no al panel ni al catálogo.
  const destino = location.state?.from || '/portal-cliente';

  const [isLogin, setIsLogin] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    email: '',
    password: '',
  });

  // Si el cliente ya está logueado, lo redirigimos a donde intentaba ir.
  useEffect(() => {
    getTienda(slug);
    if (cliente?.rol === 'CLIENTE') {
      navigate(destino, { replace: true });
    }
  }, [slug, getTienda, cliente, navigate, destino]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const identificador = formData.email.trim();

    // Validación básica
    if (!identificador || !formData.password || (!isLogin && (!formData.nombre.trim() || !formData.apellido.trim()))) {
      showToast('Por favor, completa todos los campos requeridos.');
      return;
    }
    if (!isLogin && formData.password.length < 8) {
      showToast('La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    setEnviando(true);
    try {
      // Con "@" se valida como comprador; sin "@" se busca una cuenta del
      // panel (usuario), para que el login de tienda no exija un correo.
      const esCorreo = identificador.includes('@');
      const respuesta = isLogin
        ? await loginApi(identificador, formData.password, esCorreo ? 'CLIENTE' : 'PERSONAL')
        : await registrarCliente({
          nombre: formData.nombre.trim(),
          apellido: formData.apellido.trim(),
          correo: identificador,
          contrasena: formData.password,
        });

      if (respuesta.rol !== 'CLIENTE') {
        if (respuesta.rol === 'ADMIN' || respuesta.rol === 'VENDEDOR') {
          logoutCliente();
          loginAdmin(respuesta.usuario ?? identificador, respuesta.token, respuesta.rol);
          showToast(`Bienvenido/a, ${respuesta.nombre || identificador}`);
          navigate(respuesta.rol === 'VENDEDOR' ? '/admin/pos' : '/admin', { replace: true });
          return;
        }
        showToast('Tu cuenta no tiene el rol CLIENTE para comprar en esta tienda.');
        return;
      }

      const sesion = {
        idCliente: respuesta.idCliente,
        nombre: respuesta.nombre || identificador.split('@')[0],
        correo: respuesta.correo || identificador,
        email: respuesta.correo || identificador,
        token: respuesta.token,
        rol: respuesta.rol,
      };

      // Una sola identidad activa por navegador: se descarta cualquier sesión
      // del panel para que las llamadas del comprador usen su propio token.
      logoutAdmin();
      loginCliente(sesion);
      showToast(
        isLogin
          ? `¡Bienvenida/o a ${tienda.nombre}, ${sesion.nombre}!`
          : `¡Tu cuenta fue creada, ${sesion.nombre}! Ya puedes comprar en ${tienda.nombre}.`
      );
      navigate(destino, { replace: true });
    } catch (problema) {
      showToast(problema.message || (isLogin ? 'No se pudo iniciar sesión.' : 'No se pudo crear la cuenta.'));
    } finally {
      setEnviando(false);
    }
  };

  const navLinks = (
    <>
      <Link to={`/tienda/${slug}`}>← Volver al Catálogo</Link>
      <Link to="/login">Acceso unificado</Link>
    </>
  );

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="auth-page">
        <div className="auth-container">
          <div className="auth-header">
            <h2>{isLogin ? 'Iniciar Sesión' : 'Crear Cuenta'}</h2>
            <p>Para comprar en <strong>{tienda.nombre || 'nuestra tienda'}</strong></p>
            <p className="auth-unified-link">
              <Link to="/login">¿Eres administrador, vendedor o cliente? Inicia sesión aquí</Link>
            </p>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {!isLogin && (
              <>
                <div className="field">
                  <label>Nombres</label>
                  <input
                    type="text"
                    name="nombre"
                    placeholder="Ej. Anelhi"
                    value={formData.nombre}
                    onChange={handleChange}
                  />
                </div>
                <div className="field">
                  <label>Apellidos</label>
                  <input
                    type="text"
                    name="apellido"
                    placeholder="Ej. Marcos"
                    value={formData.apellido}
                    onChange={handleChange}
                  />
                </div>
              </>
            )}

            <div className="field">
              <label>{isLogin ? 'Correo o usuario' : 'Correo electrónico'}</label>
              <input
                type={isLogin ? 'text' : 'email'}
                name="email"
                placeholder={isLogin ? 'tu@email.com o tu usuario' : 'tu@email.com'}
                value={formData.email}
                onChange={handleChange}
              />
            </div>

            <div className="field">
              <label>Contraseña</label>
              <input
                type="password"
                name="password"
                placeholder="••••••••"
                value={formData.password}
                onChange={handleChange}
              />
            </div>

            <button type="submit" className="btn block" style={{ marginTop: 12 }} disabled={enviando}>
              {isLogin ? 'Ingresar a mi cuenta' : 'Registrarme ahora'}
            </button>
          </form>

          <div className="auth-footer">
            <p>
              {isLogin ? '¿No tienes una cuenta aún?' : '¿Ya tienes una cuenta registrada?'}{' '}
              <button
                type="button"
                className="btn-link"
                onClick={() => setIsLogin(!isLogin)}
              >
                {isLogin ? 'Crea una aquí' : 'Inicia sesión'}
              </button>
            </p>
          </div>
        </div>
      </div>
    </PlantillaTienda>
  );
}
