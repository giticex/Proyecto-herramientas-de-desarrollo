import { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import { login as loginApi, registroCliente as registroClienteApi } from '../auth/authApi';
import { ApiError } from '../api/ApiError';
import { showToast } from '../components/toastBus';
import './ClienteAuth.css';

export default function ClienteAuth() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { tienda, getTienda } = useStore();
  const { loginCliente, cliente } = useClienteStore();

  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    email: '',
    password: '',
    telefono: '',
  });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const destinoVuelta = location.state?.from || `/tienda/${slug}`;

  // Si el cliente ya está logueado, lo redirigimos automáticamente al catálogo
  useEffect(() => {
    getTienda(slug);
    if (cliente?.token) {
      navigate(destinoVuelta);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug, cliente]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.email || !formData.password || (!isLogin && (!formData.nombre || !formData.apellido))) {
      setError('Por favor, completa todos los campos requeridos.');
      return;
    }
    if (!isLogin && formData.password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    setEnviando(true);
    try {
      const respuesta = isLogin
        ? await loginApi(formData.email.trim(), formData.password, 'CLIENTE')
        : await registroClienteApi({
            nombre: formData.nombre.trim(),
            apellido: formData.apellido.trim(),
            correo: formData.email.trim(),
            contrasena: formData.password,
            telefono: formData.telefono.trim(),
          });

      if (respuesta.usuario.rol !== 'CLIENTE') {
        setError('Esta cuenta es de administrador, usa el acceso de administrador.');
        return;
      }

      loginCliente(respuesta, isLogin ? undefined : formData.nombre.trim());
      showToast(`¡Bienvenida/o a ${tienda.nombre}!`);
      navigate(destinoVuelta);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Ese correo ya está registrado.');
      } else if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Ocurrió un error. Intenta de nuevo.');
      }
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

          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {!isLogin && (
              <>
                <div className="field">
                  <label>Nombre</label>
                  <input
                    type="text"
                    name="nombre"
                    placeholder="Ej. Anelhi"
                    value={formData.nombre}
                    onChange={handleChange}
                  />
                </div>
                <div className="field">
                  <label>Apellido</label>
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
              <label>Correo electrónico</label>
              <input
                type="email"
                name="email"
                placeholder="tu@email.com"
                autoComplete="username"
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
                autoComplete={isLogin ? 'current-password' : 'new-password'}
                value={formData.password}
                onChange={handleChange}
              />
              {!isLogin && <span className="hint">Mínimo 8 caracteres.</span>}
            </div>

            {!isLogin && (
              <div className="field">
                <label>Teléfono</label>
                <input
                  type="tel"
                  name="telefono"
                  placeholder="Ej. 987654321"
                  value={formData.telefono}
                  onChange={handleChange}
                />
              </div>
            )}

            {error && <p className="error" role="alert">{error}</p>}

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
                onClick={() => { setIsLogin(!isLogin); setError(''); }}
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
