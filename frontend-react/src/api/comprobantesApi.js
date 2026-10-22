// Comprobantes de pago electronicos: emision, consulta y archivos del
// comprobante (representacion impresa en PDF, XML UBL 2.1 y codigo QR).
//
// El backend es la fuente de verdad de los importes: aqui solo se envian el
// tipo de comprobante, los datos del adquirente, la direccion de entrega, el
// medio de pago y los identificadores de los productos del carrito.
import { apiClient, apiUrl } from './client';

/** Tipos de comprobante (catalogo 01 de SUNAT). */
export const TIPO_COMPROBANTE = {
  FACTURA: '01',
  BOLETA: '03',
};

/** Tipo de documento del adquirente (catalogo 01 de SUNAT). */
export const TIPO_DOCUMENTO = {
  RUC: '1',
  DNI: '6',
};

/** Condicion de la operacion (catalogo 51 de SUNAT). */
export const CONDICION_OPERACION = {
  CONTADO: '01',
  CREDITO: '02',
};

/** Forma de pago: contado o credito. */
export const FORMA_PAGO = {
  CONTADO: '01',
  CREDITO: '02',
};

/** Moneda del comprobante (ISO 4217). */
export const MONEDA = {
  PEN: 'PEN',
};

/** Medios de pago del catalogo 09 de SUNAT. */
export const MEDIO_PAGO = {
  efectivo: { codigo: '001', descripcion: 'Efectivo' },
  tarjeta: { codigo: '006', descripcion: 'Tarjeta de crédito' },
  yape: { codigo: '009', descripcion: 'Yape' },
  plin: { codigo: '009', descripcion: 'Plin' },
  transferencia: { codigo: '003', descripcion: 'Transferencia bancaria' },
  deposito: { codigo: '002', descripcion: 'Depósito en cuenta' },
};

/** Limite legal de una boleta sin RUC del adquirente. */
export const LIMITE_BOLETA_SIN_RUC = 700;

/** Pais por defecto de los domicilios (ISO 3166-1). */
export const PAIS_DEFECTO = 'PE';

const REGEX_RUC = /^\d{11}$/;
const REGEX_DNI = /^\d{8}$/;
const PREFIJOS_RUC = ['10', '20'];
const LARGO_MINIMO_NOMBRE = 3;
const LARGO_MINIMO_DIRECCION = 5;

/** Un RUC valido tiene 11 digitos y empieza con 10 (persona) o 20 (empresa). */
export function esRucValido(documento) {
  const valor = (documento || '').trim();
  return REGEX_RUC.test(valor) && PREFIJOS_RUC.includes(valor.slice(0, 2));
}

/** Un DNI valido tiene 8 digitos. */
export function esDniValido(documento) {
  return REGEX_DNI.test((documento || '').trim());
}

/**
 * Validaciones fiscales obligatorias del checkout.
 *
 * <ul>
 *   <li>Factura: RUC, razon social y domicilio fiscal del adquirente.</li>
 *   <li>Boleta: DNI y nombres del adquirente; ademas una boleta sin RUC no
 *       puede superar el limite legal de S/ 700.00.</li>
 * </ul>
 *
 * @returns {{valido: boolean, errores: string[]}}
 */
export function validarDatosFacturacion({
  tipoComprobante,
  numeroDocumento,
  nombreAdquirente,
  domicilioFiscal,
  direccionEntrega,
  total = 0,
}) {
  const errores = [];
  const documento = (numeroDocumento || '').trim();
  const nombre = (nombreAdquirente || '').trim();
  const domicilio = (domicilioFiscal || '').trim();
  const entrega = (direccionEntrega || '').trim();

  if (tipoComprobante === TIPO_COMPROBANTE.FACTURA) {
    if (!esRucValido(documento)) {
      errores.push('Ingresa un RUC válido de 11 dígitos (inicia con 10 o 20).');
    }
    if (nombre.length < LARGO_MINIMO_NOMBRE) {
      errores.push('Ingresa la Razón Social de la empresa.');
    }
    if (domicilio.length < LARGO_MINIMO_DIRECCION) {
      errores.push('Ingresa el domicilio fiscal del adquirente.');
    }
  } else {
    if (!esDniValido(documento)) {
      errores.push('Para Boleta Electrónica, el DNI debe tener 8 dígitos.');
    }
    if (nombre.length < LARGO_MINIMO_NOMBRE) {
      errores.push('Ingresa los nombres y apellidos del adquirente.');
    }
    if (Number(total) > LIMITE_BOLETA_SIN_RUC) {
      errores.push(
        `El monto supera S/ ${LIMITE_BOLETA_SIN_RUC}.00. Para ese importe debes solicitar una Factura.`
      );
    }
  }

  if (entrega.length < LARGO_MINIMO_DIRECCION && domicilio.length < LARGO_MINIMO_DIRECCION) {
    errores.push('Ingresa una dirección de entrega válida.');
  }

  return { valido: errores.length === 0, errores };
}

/**
 * Arma el payload de emision con la misma estructura que el
 * `ComprobanteRequestDTO` del backend (Fase 1).
 *
 * No se envian los importes: el precio, la unidad de medida, la afectacion
 * del IGV y los totales los recalcula el backend desde el catalogo de
 * productos, y el monto del medio de pago lo completa el backend con el
 * total del comprobante.
 */
export function construirPayloadComprobante({
  tipoComprobante = TIPO_COMPROBANTE.BOLETA,
  idCliente,
  serie,
  condicionOperacion = CONDICION_OPERACION.CONTADO,
  formaPago = FORMA_PAGO.CONTADO,
  moneda = MONEDA.PEN,
  numeroDocumento,
  nombreAdquirente,
  correo,
  domicilioFiscal,
  direccionEntrega,
  medioPago,
  items = [],
  observacion,
}) {
  const tipoDocumento =
    tipoComprobante === TIPO_COMPROBANTE.FACTURA ? TIPO_DOCUMENTO.RUC : TIPO_DOCUMENTO.DNI;
  const domicilio = (domicilioFiscal || '').trim();
  const entrega = (direccionEntrega || '').trim() || domicilio;
  const medio = medioPago || MEDIO_PAGO.efectivo;

  const payload = {
    tipoComprobante,
    condicionOperacion,
    formaPago,
    moneda,
    receptor: {
      tipoDocumento,
      numeroDocumento: (numeroDocumento || '').trim(),
      razonSocial: (nombreAdquirente || '').trim(),
      correo: (correo || '').trim(),
      domicilioFiscal: {
        direccion: domicilio,
        pais: PAIS_DEFECTO,
      },
    },
    entrega: {
      direccion: entrega,
      pais: PAIS_DEFECTO,
    },
    mediosPago: [
      {
        codigo: medio.codigo,
        descripcion: medio.descripcion,
      },
    ],
    // Solo el id del producto y la cantidad: el resto de los campos del item
    // los completa el backend con los datos del catalogo.
    items: items.map((item) => ({
      idProducto: item.idProducto,
      cantidad: item.cantidad,
    })),
  };

  if (idCliente !== null && idCliente !== undefined && idCliente !== '') {
    payload.idCliente = String(idCliente);
  }
  if (serie) {
    payload.serie = serie;
  }
  if (correo) {
    payload.emailContacto = correo;
  }
  if (observacion) {
    payload.observacion = observacion;
  }

  return payload;
}

/** Emite el comprobante y devuelve la respuesta completa del backend. */
export async function emitirComprobante(payload) {
  return apiClient.post('/comprobantes', payload);
}

/** Consulta un comprobante por el id de la venta. */
export async function obtenerComprobante(idVenta) {
  return apiClient.get(`/comprobantes/${idVenta}`);
}

/** Lista los comprobantes emitidos a un cliente. */
export async function listarComprobantesDeCliente(idCliente) {
  return apiClient.get(`/comprobantes/cliente/${idCliente}`);
}

/** Representacion impresa en PDF (A5 con QR y valor resumen). */
export function urlComprobantePdf(idVenta) {
  return apiUrl(`/comprobantes/${idVenta}/pdf`);
}

/** XML UBL 2.1 con el nombre normativo del archivo. */
export function urlComprobanteXml(idVenta) {
  return apiUrl(`/comprobantes/${idVenta}/xml`);
}

/** Codigo QR del comprobante en PNG. */
export function urlComprobanteQr(idVenta) {
  return apiUrl(`/comprobantes/${idVenta}/qr`);
}
