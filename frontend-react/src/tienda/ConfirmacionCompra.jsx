import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { obtenerComprobante } from '../api/comprobantesApi';
import { useClienteStore } from './clienteStore';
import { rutaTienda } from './rutasTienda';
import ComprobanteImpresion from './ComprobanteImpresion';
import './ConfirmacionCompra.css';

export default function ConfirmacionCompra() {
  const { slug } = useParams();
  const [busqueda] = useSearchParams();
  const idVenta = busqueda.get('venta');
  const token = useClienteStore((estado) => estado.cliente?.token);
  const [resultado, setResultado] = useState(null);
  const [mostrando, setMostrando] = useState(false);

  useEffect(() => {
    if (!idVenta || !token) return undefined;
    let vigente = true;
    obtenerComprobante(idVenta, token)
      .then((comprobante) => {
        if (vigente) setResultado({ idVenta, token, comprobante });
      })
      .catch((problema) => {
        if (vigente) setResultado({ idVenta, token, error: problema.message || 'No se pudo recuperar el comprobante.' });
      });
    return () => { vigente = false; };
  }, [idVenta, token]);

  const consulta = resultado?.idVenta === idVenta && resultado?.token === token ? resultado : null;
  const comprobante = consulta?.comprobante;
  return (
    <>
      <main className="confirmacion-page">
        <div className="confirmacion-card">
          <h1>{comprobante ? 'Comprobante generado' : 'Confirmación de compra'}</h1>
          {!idVenta && <p>No se indicó una compra. Puedes consultar tus pedidos anteriores.</p>}
          {idVenta && token && !consulta && <p role="status">Consultando tu comprobante…</p>}
          {consulta?.error && <p role="alert">{consulta.error}</p>}
          {comprobante && <p className="numero-pedido">{comprobante.numeroDocumento}</p>}
          <div className="confirmacion-acciones">
            {comprobante && <button type="button" className="btn" onClick={() => setMostrando(true)}>Ver comprobante</button>}
            <Link to={rutaTienda(slug) + '/mis-compras'} className="btn">Ver mis compras</Link>
            <Link to={rutaTienda(slug)} className="btn ghost">Seguir comprando</Link>
          </div>
        </div>
      </main>
      {mostrando && comprobante && <ComprobanteImpresion comprobante={comprobante} token={token} onClose={() => setMostrando(false)} />}
    </>
  );
}
