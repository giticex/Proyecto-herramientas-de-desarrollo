import { useEffect, useRef, useState } from 'react';
import { subscribeToast } from './toastBus';

// Porta el toast() de frontend/js/nav.js. Se monta una vez en App.
const DURACION_MS = { info: 2200, exito: 2600, error: 4500 };

export default function Toast() {
  const [mensaje, setMensaje] = useState('');
  const [tipo, setTipo] = useState('info');
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef(null);

  useEffect(() => subscribeToast((msg, tipoRecibido) => {
    setMensaje(msg);
    setTipo(tipoRecibido);
    setVisible(true);
    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setVisible(false), DURACION_MS[tipoRecibido] ?? DURACION_MS.info);
  }), []);

  const esError = tipo === 'error';

  return (
    <div
      className={`toast toast-${tipo}${visible ? ' show' : ''}`}
      role={esError ? 'alert' : 'status'}
      aria-live={esError ? 'assertive' : 'polite'}
    >
      {mensaje}
    </div>
  );
}
