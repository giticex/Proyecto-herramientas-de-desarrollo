import { useState } from 'react';
import { apiClient } from '../api/client';
import { urlComprobanteQr } from '../api/comprobantesApi';
import { showToast } from '../components/toastBus';
import './ComprobanteImpresion.css';

const money = (valor) => `S/ ${Number(valor || 0).toFixed(2)}`;

const cantidad = (valor) => Number(valor || 0).toString();

/** Datos del emisor usados cuando el comprobante no los trae (compras del navegador). */
const EMISOR_FALLBACK = {
  razonSocial: 'INNOVATION STORE S.A.C.',
  nombreComercial: 'TECHZONE',
  ruc: '20512345678',
  domicilioFiscal: 'Av. Javier Prado Este 4200',
  ubigeo: '150131',
  distrito: 'Santiago de Surco',
  provincia: 'Lima',
  departamento: 'Lima',
  pais: 'PE',
};

const URL_REPRESENTACION = 'https://www.techzone.pe/comprobantes';

/** Solo las compras emitidas por el backend tienen XML, PDF y QR descargables. */
function tieneArchivoBackend(idVenta) {
  return Boolean(idVenta) && /^[0-9a-f]{8}-[0-9a-f]{4}/i.test(String(idVenta));
}

function tipoDocumentoTexto(codigo) {
  if (codigo === '1') return 'RUC';
  if (codigo === '6') return 'DNI';
  if (codigo === '4') return 'Carnet de extranjería';
  if (codigo === '7') return 'Pasaporte';
  if (codigo === 'A') return 'Cédula diplomática';
  return 'Documento';
}

function afectacionTexto(afectacion) {
  if (afectacion === '02') return 'Inafecta';
  if (afectacion === '03') return 'Exonerada';
  if (afectacion === '04') return 'Gratuita';
  return 'Gravada';
}

/**
 * Leyendas de la representacion impresa. Si el comprobante no las trae (por
 * ejemplo una compra guardada solo en el navegador) se reconstruyen con la
 * glosa legal que exige la RSM 300-2019/SUNAT.
 */
function leyendasDe(comprobante, emisor) {
  const leyendas = Array.isArray(comprobante.leyendas) ? comprobante.leyendas : [];
  if (leyendas.length > 0) return leyendas;

  const esFactura = comprobante.tipoComprobante === '01';
  return [
    { codigo: null, texto: `Representación impresa de ${emisor.razonSocial}, consulte en ${URL_REPRESENTACION}` },
    { codigo: null, texto: esFactura ? 'Factura electrónica' : 'Boleta de venta electrónica' },
  ];
}

/**
 * Marcador visual del codigo QR: cuando el comprobante no trae la imagen
 * (compras locales) se dibuja un cuadrante de referencia con los tres
 * patrones localizadores, para senalar donde va el QR en la impresion.
 */
function QrPlaceholder({ semilla }) {
  const lado = 11;
  const base = String(semilla || 'TECHZONE');
  const celdas = [];

  for (let y = 0; y < lado; y += 1) {
    for (let x = 0; x < lado; x += 1) {
      const localizador =
        (x < 4 && y < 4) || (x > lado - 5 && y < 4) || (x < 4 && y > lado - 5);
      const centro = x === 5 && y === 5;
      const patron = base.charCodeAt((y * lado + x) % base.length) % 3 === 0;
      celdas.push(localizador || centro || patron);
    }
  }

  return (
    <div className="ci-qr-marcador" aria-label="Marcador del código QR">
      <div className="ci-qr-matriz">
        {celdas.map((oscuro, indice) => (
          <span key={indice} className={oscuro ? 'on' : 'off'} />
        ))}
      </div>
      <span className="ci-qr-pie">Código QR</span>
    </div>
  );
}

/**
 * Representacion impresa del comprobante de pago electronico.
 *
 * <p>Reproduce la estructura que exige la RSM 300-2019/SUNAT: cabecera del
 * emisor, datos del adquirente, detalle de los items, desglose de los
 * totales, leyendas y, en la parte inferior, el codigo QR (con su zona de
 * silencio) junto al Valor Resumen.</p>
 */
export default function ComprobanteImpresion({ comprobante, onClose }) {
  const [verQr, setVerQr] = useState(false);
  const [descargando, setDescargando] = useState(null);

  if (!comprobante) return null;

  const emisor = { ...EMISOR_FALLBACK, ...(comprobante.emisor || {}) };
  const receptor = comprobante.receptor || {};
  const totales = comprobante.totales || {};
  const detalles = comprobante.detalles || [];
  const mediosPago = comprobante.mediosPago || [];
  const domicilio = receptor.domicilioFiscal || {};
  const entrega = comprobante.entrega || {};
  const leyendas = leyendasDe(comprobante, emisor);
  const esFactura = comprobante.tipoComprobante === '01';

  const srcQr = comprobante.qrPngBase64
    ? `data:image/png;base64,${comprobante.qrPngBase64}`
    : tieneArchivoBackend(comprobante.idVenta)
      ? urlComprobanteQr(comprobante.idVenta)
      : '';

  const totalVenta = Number(totales.totalVenta || totales.montoTotalPagar || 0);
  const totalPagar = Number(totales.montoTotalPagar || totalVenta);
  const semillaQr = comprobante.hashComprobante || comprobante.numeroDocumento || 'TECHZONE';

  // Los endpoints de PDF/XML exigen el token: se descarga el archivo con el
  // cliente HTTP (que inyecta el Bearer) y se abre el blob en una pestaña.
  const descargarArchivoSeguro = async (ruta, tipo) => {
    setDescargando(tipo);
    try {
      const blob = await apiClient.getBlob(ruta);
      const url = window.URL.createObjectURL(blob);
      const ventana = window.open(url, '_blank');
      if (!ventana) {
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `${comprobante.numeroDocumento || 'comprobante'}.${tipo.toLowerCase()}`;
        enlace.click();
      }
      setTimeout(() => window.URL.revokeObjectURL(url), 60000);
    } catch (error) {
      showToast(error?.message || `No se pudo abrir el ${tipo}.`);
    } finally {
      setDescargando(null);
    }
  };

  return (
    <div className="comprobante-overlay" role="dialog" aria-modal="true">
      <div className="comprobante-modal">
        <div className="comprobante-modal-barra">
          <strong>Representación impresa</strong>
          <div className="comprobante-modal-acciones">
            {tieneArchivoBackend(comprobante.idVenta) && (
              <>
                <button
                  type="button"
                  className="btn-link"
                  disabled={descargando === 'PDF'}
                  onClick={() =>
                    descargarArchivoSeguro(`/comprobantes/${comprobante.idVenta}/pdf`, 'PDF')
                  }
                >
                  {descargando === 'PDF' ? 'Abriendo…' : 'Ver PDF'}
                </button>
                <button
                  type="button"
                  className="btn-link"
                  disabled={descargando === 'XML'}
                  onClick={() =>
                    descargarArchivoSeguro(`/comprobantes/${comprobante.idVenta}/xml`, 'XML')
                  }
                >
                  {descargando === 'XML' ? 'Abriendo…' : 'Ver XML'}
                </button>
              </>
            )}
            <button type="button" className="btn-link" onClick={() => window.print()}>
              Imprimir
            </button>
            <button type="button" className="btn-link" onClick={onClose}>
              Cerrar
            </button>
          </div>
        </div>

        <div className="comprobante-hoja">
          <header className="ci-emisor">
            <h3>{emisor.razonSocial}</h3>
            {emisor.nombreComercial && <p className="ci-nombre-comercial">{emisor.nombreComercial}</p>}
            <p className="ci-ruc">R.U.C. {emisor.ruc}</p>
            <p className="ci-direccion">
              {emisor.domicilioFiscal}
              {emisor.distrito ? ` - ${emisor.distrito}` : ''}
              {emisor.provincia ? `, ${emisor.provincia}` : ''}
            </p>
          </header>

          <h4 className="ci-titulo">{comprobante.tipoComprobanteDescripcion || (esFactura ? 'Factura' : 'Boleta')} ELECTRÓNICA</h4>
          <p className="ci-numero">{comprobante.numeroDocumento}</p>
          <p className="ci-fecha">
            Fecha de emisión: {comprobante.fechaEmision} {comprobante.horaEmision}
          </p>

          <section className="ci-datos">
            <div className="ci-dato">
              <span>Señor(es):</span>
              <strong>{receptor.razonSocial}</strong>
            </div>
            <div className="ci-dato">
              <span>{tipoDocumentoTexto(receptor.tipoDocumento)}:</span>
              <strong>{receptor.numeroDocumento}</strong>
            </div>
            {domicilio.direccion && (
              <div className="ci-dato">
                <span>Dirección:</span>
                <strong>
                  {domicilio.direccion}
                  {domicilio.distrito ? ` - ${domicilio.distrito}` : ''}
                </strong>
              </div>
            )}
            {entrega.direccion && entrega.direccion !== domicilio.direccion && (
              <div className="ci-dato">
                <span>Entrega:</span>
                <strong>{entrega.direccion}</strong>
              </div>
            )}
          </section>

          <table className="ci-items">
            <thead>
              <tr>
                <th>N°</th>
                <th>CANT</th>
                <th>DESCRIPCIÓN</th>
                <th>V. UNIT</th>
                <th>P. UNIT</th>
                <th>IGV</th>
                <th>IMPORTE</th>
              </tr>
            </thead>
            <tbody>
              {detalles.length === 0 && (
                <tr>
                  <td colSpan="7" className="ci-centro">
                    Sin detalle de productos
                  </td>
                </tr>
              )}
              {detalles.map((detalle, indice) => (
                <tr key={detalle.numero || indice}>
                  <td className="ci-centro">{detalle.numero ?? indice + 1}</td>
                  <td className="ci-centro">{cantidad(detalle.cantidad)}</td>
                  <td>
                    {detalle.descripcion}
                    {detalle.afectacionIgv && detalle.afectacionIgv !== '01' && (
                      <em className="ci-nota-item"> ({afectacionTexto(detalle.afectacionIgv)})</em>
                    )}
                  </td>
                  <td className="ci-derecha">{Number(detalle.valorUnitario || 0).toFixed(2)}</td>
                  <td className="ci-derecha">{Number(detalle.precioUnitario || 0).toFixed(2)}</td>
                  <td className="ci-derecha">{Number(detalle.igv || 0).toFixed(2)}</td>
                  <td className="ci-derecha">{Number(detalle.totalItem || 0).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <section className="ci-totales">
            {Number(totales.opGravada) > 0 && (
              <div className="ci-fila">
                <span>Op. Gravada (base imponible):</span>
                <span>{money(totales.opGravada)}</span>
              </div>
            )}
            {Number(totales.opInafecta) > 0 && (
              <div className="ci-fila">
                <span>Op. Inafecta:</span>
                <span>{money(totales.opInafecta)}</span>
              </div>
            )}
            {Number(totales.opExonerada) > 0 && (
              <div className="ci-fila">
                <span>Op. Exonerada:</span>
                <span>{money(totales.opExonerada)}</span>
              </div>
            )}
            {Number(totales.opGratuita) > 0 && (
              <div className="ci-fila">
                <span>Op. Gratuitas:</span>
                <span>{money(totales.opGratuita)}</span>
              </div>
            )}
            {Number(totales.totalDescuentos) > 0 && (
              <div className="ci-fila">
                <span>Descuentos:</span>
                <span>- {money(totales.totalDescuentos)}</span>
              </div>
            )}
            <div className="ci-fila">
              <span>I.G.V. (18%):</span>
              <span>{money(totales.igv)}</span>
            </div>
            <div className="ci-fila">
              <span>Total de la venta:</span>
              <span>{money(totalVenta)}</span>
            </div>
            <div className="ci-fila ci-fila-total">
              <span>IMPORTE TOTAL A PAGAR:</span>
              <span>
                {money(totalPagar)} {comprobante.moneda || 'PEN'}
              </span>
            </div>
          </section>

          {totales.totalEnLetras ? (
            <p className="ci-letras">SON: {totales.totalEnLetras}</p>
          ) : (
            <p className="ci-letras ci-letras-vacia">SON: (monto en letras no disponible)</p>
          )}

          {mediosPago.map((medio, indice) => (
            <p className="ci-pago" key={indice}>
              Medio de pago: {medio.descripcion} ({medio.codigo}) - {money(medio.monto)}
              {medio.referencia ? ` - Ref. ${medio.referencia}` : ''}
            </p>
          ))}

          <section className="ci-leyendas">
            {leyendas.map((leyenda, indice) => (
              <p key={indice}>{leyenda.texto}</p>
            ))}
          </section>

          {/* Marcadores visuales: QR, codigo hash y leyenda legal de SUNAT */}
          <section className="ci-marcadores">
            <div className="ci-marcador ci-marcador-qr">
              {srcQr ? (
                <img src={srcQr} alt="Código QR del comprobante" width="200" height="200" />
              ) : (
                <QrPlaceholder semilla={semillaQr} />
              )}
              <p className="ci-marcador-titulo">Código QR</p>
              <p className="ci-marcador-nota">
                {srcQr
                  ? 'Escanea para verificar el comprobante en SUNAT.'
                  : 'Marcador de posición: el QR se genera al emitir el comprobante.'}
              </p>
            </div>

            <div className="ci-marcador ci-marcador-hash">
              <p className="ci-marcador-titulo">Código Hash (Valor Resumen)</p>
              {comprobante.hashComprobante ? (
                <>
                  <p className="ci-resumen-valor">{comprobante.hashComprobante}</p>
                  {comprobante.valorResumen && (
                    <>
                      <p className="ci-marcador-titulo">Valor Resumen (base 64)</p>
                      <p className="ci-resumen-valor">{comprobante.valorResumen}</p>
                    </>
                  )}
                </>
              ) : (
                <p className="ci-marcador-vacio">
                  Código hash no disponible: el comprobante aún no fue emitido por el backend.
                </p>
              )}
            </div>

            <div className="ci-marcador ci-marcador-legal">
              <p className="ci-marcador-titulo">Leyenda legal SUNAT</p>
              <p>
                Representación impresa de {emisor.razonSocial}, consulte en {URL_REPRESENTACION}
              </p>
              <p>{esFactura ? 'Factura electrónica' : 'Boleta de venta electrónica'}</p>
              <p className="ci-marcador-nota">
                Comprobante emitido electrónicamente. El código QR y el Valor Resumen permiten
                verificar la autenticidad del documento ante SUNAT (RSM 300-2019).
              </p>
              {comprobante.codigoQr && (
                <>
                  <button
                    type="button"
                    className="btn-link ci-qr-toggle"
                    onClick={() => setVerQr(!verQr)}
                  >
                    {verQr ? 'Ocultar contenido del QR' : 'Ver contenido del QR'}
                  </button>
                  {verQr && <p className="ci-resumen-valor">{comprobante.codigoQr}</p>}
                </>
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
