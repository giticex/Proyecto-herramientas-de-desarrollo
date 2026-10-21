import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import PlantillaTienda from '../components/PlantillaTienda';
import { useClienteStore } from './clienteStore';
import { showToast } from '../components/toastBus';
import './CheckoutTienda.css';

export default function CheckoutTienda() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { carrito, cliente, vaciarCarrito } = useClienteStore();

  const [tipoComprobante, setTipoComprobante] = useState('boleta');
  const [documento, setDocumento] = useState('');
  const [direccion, setDireccion] = useState('');

  const total = carrito.reduce((sum, item) => sum + (Number(item.precio) * item.cantidad), 0);

  // Proteger la ruta: si no hay carrito o cliente, regresar
  useEffect(() => {
    if (!cliente) {
      showToast('Inicia sesión para finalizar tu compra.');
      navigate(`/tienda/${slug}/login`);
    } else if (carrito.length === 0) {
      navigate(`/tienda/${slug}`);
    }
  }, [cliente, carrito, navigate, slug]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!documento || !direccion) {
      showToast('Por favor, completa tu documento y dirección.');
      return;
    }

    // Aquí irá la conexión al backend (Spring Boot) en el futuro.
    // Por ahora simulamos la compra exitosa.
    showToast('¡Compra procesada con éxito!');
    vaciarCarrito();
    navigate(`/tienda/${slug}/confirmacion`);
  };

  const navLinks = <Link to={`/tienda/${slug}/carrito`}>← Volver al Carrito</Link>;

  return (
    <PlantillaTienda navLinks={navLinks}>
      <div className="checkout-page">
        <div className="checkout-header">
          <h2>Finalizar Compra</h2>
        </div>

        <form className="checkout-form" onSubmit={handleSubmit}>
          <div className="field">
            <label>Tipo de Comprobante</label>
            <div className="tipo-comprobante">
              <label className="radio-label">
                <input 
                  type="radio" 
                  name="comprobante" 
                  value="boleta" 
                  checked={tipoComprobante === 'boleta'} 
                  onChange={() => setTipoComprobante('boleta')} 
                />
                Boleta
              </label>
              <label className="radio-label">
                <input 
                  type="radio" 
                  name="comprobante" 
                  value="factura" 
                  checked={tipoComprobante === 'factura'} 
                  onChange={() => setTipoComprobante('factura')} 
                />
                Factura
              </label>
            </div>
          </div>

          <div className="field">
            <label>{tipoComprobante === 'boleta' ? 'DNI' : 'RUC'}</label>
            <input
              type="text"
              placeholder={`Ingresa tu ${tipoComprobante === 'boleta' ? 'DNI' : 'RUC'}`}
              value={documento}
              onChange={(e) => setDocumento(e.target.value)}
              maxLength={tipoComprobante === 'boleta' ? 8 : 11}
            />
          </div>

          <div className="field">
            <label>Dirección de Envío</label>
            <input
              type="text"
              placeholder="Ej. Av. Arequipa 1234, Lima"
              value={direccion}
              onChange={(e) => setDireccion(e.target.value)}
            />
          </div>

          <div className="checkout-resumen">
            <span>Total a Pagar:</span>
            <span>S/ {total.toFixed(2)}</span>
          </div>

          <button type="submit" className="btn block">
            Confirmar y Pagar
          </button>
        </form>
      </div>
    </PlantillaTienda>
  );
}