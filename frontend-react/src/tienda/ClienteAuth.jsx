import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useStore } from '../store/store';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import './ClienteAuth.css';

export default function ClienteAuth() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { tienda, getTienda } = useStore();
  const { loginCliente, cliente } = useClienteStore();

  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    nombre: '',
    email: '',
    password: '',
  });

  // Si el cliente ya está logueado, lo redirigimos automáticamente al catálogo
  useEffect(() => {
    getTienda(slug);
    if (cliente) {
      navigate(`/tienda/${slug}`);
    }
  }, [slug, getTienda, cliente, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    // Validación básica
    if (!formData.email || !formData.password || (!isLogin && !formData.nombre)) {
      showToast('Por favor, completa todos los campos requeridos.');
      return;
    }

    // Simulamos la autenticación exitosa (Luego lo conectaremos al backend)
    const nombreUsuario = isLogin ? (formData.email.split('@')[0]) : formData.nombre;
    const mockUser = {
      idCliente: Date.now(),
      nombre: nombreUsuario,
      email: formData.email,
    };

    loginCliente(mockUser);
    showToast(`¡Bienvenida/o a ${tienda.nombre}, ${nombreUsuario}!`);
    navigate(`/tienda/${slug}`);
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

            <button type="submit" className="btn block" style={{ marginTop: 12 }}>
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