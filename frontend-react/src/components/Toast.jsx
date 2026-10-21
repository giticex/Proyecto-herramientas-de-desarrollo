import { useEffect, useRef, useState } from 'react';
import { subscribeToast } from './toastBus';

// Porta el toast() de frontend/js/nav.js. Se monta una vez en App.
export default function Toast() {
  const [mensaje, setMensaje] = useState('');
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef(null);

  useEffect(() => subscribeToast((msg) => {
    setMensaje(msg);
    setVisible(true);
    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setVisible(false), 2200);
  }), []);

  return <div className={`toast${visible ? ' show' : ''}`}>{mensaje}</div>;
}
