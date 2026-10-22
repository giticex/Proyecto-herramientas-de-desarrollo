import { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import { loginApi } from '../auth/authApi';
import { showToast } from '../components/toastBus';
import './ClienteAuth.css';

export default function ClienteAuth() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { tienda, getTienda } = useStore();
  const { loginCliente, cliente } = useClienteStore();

  // Si el checkout manda al login, vuelve a esa ruta al terminar.
  const destino = location.state?.from || `/tienda/${slug}`;

  const [isLogin, setIsLogin] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [formData, setFormData] = useState({
    nombre: '',
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

    // Validación básica
    if (!formData.email || !formData.password || (!isLogin && !formData.nombre)) {
      showToast('Por favor, completa todos los campos requeridos.');
      return;
    }

    // El alta de compradores todavía no tiene endpoint en el backend.
    if (!isLogin) {
      showToast('El registro de compradores todavía no está disponible.');
      return;
    }

    setEnviando(true);
    try {
      // El backend valida la clave y devuelve el rol de la cuenta.
      const respuesta = await loginApi(formData.email.trim(), formData.password);
      if (respuesta.rol !== 'CLIENTE') {
        showToast('Tu cuenta no tiene el rol CLIENTE para comprar en esta tienda.');
        return;
      }

      const sesion = {
        idCliente: respuesta.idCliente,
        nombre: respuesta.nombre || formData.email.split('@')[0],
        correo: respuesta.correo || formData.email,
        email: respuesta.correo || formData.email,
        token: respuesta.token,
        rol: respuesta.rol,
      };

      loginCliente(sesion);
      showToast(`¡Bienvenida/o a ${tienda.nombre}, ${sesion.nombre}!`);
      navigate(destino, { replace: true });
    } catch (problema) {
      showToast(problema.message || 'No se pudo iniciar sesión.');
    } finally {
      setEnviando(false);
    }
  };

  const navLinks = (
    <Link to={`/tienda/${slug}`}>← Volver al Catálogo</Link>
  );

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="auth-page">
        <div className="auth-container">
          <div className="auth-header">
            <h2>{isLogin ? 'Iniciar Sesión' : 'Crear Cuenta'}</h2>
            <p>Para comprar en <strong>{tienda.nombre || 'nuestra tienda'}</strong></p>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {!isLogin && (
              <div className="field">
                <label>Nombre completo</label>
                <input
                  type="text"
                  name="nombre"
                  placeholder="Ej. Anelhi Marcos"
                  value={formData.nombre}
                  onChange={handleChange}
                />
              </div>
            )}

            <div className="field">
              <label>Correo electrónico</label>
              <input
                type="email"
                name="email"
                placeholder="tu@email.com"
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
