package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;

/**
 * Datos del emisor y parametros fiscales tomados de la tabla `parametro`.
 */
public interface EmisorService {

    EmisorDTO obtenerEmisor();

    /**
     * Datos del emisor para una tienda: usa su razon social, RUC, direccion y
     * telefono configurados y completa lo que falte con los parametros
     * globales. El nombre comercial es el nombre de la tienda.
     */
    EmisorDTO obtenerEmisor(int idTienda);

    /** Tasa de IGV vigente en porcentaje (18.00). */
    java.math.BigDecimal obtenerIgvPorcentaje();

    /** Moneda del comprobante (PEN). */
    String obtenerMoneda();

    /** Serie configurada para el tipo de comprobante (B001 / F001). */
    String obtenerSerie(String tipoComprobante);

    /**
     * Serie configurada por la tienda para el tipo de comprobante. Si la
     * tienda aun no la definio se usa la serie global.
     */
    String obtenerSerie(int idTienda, String tipoComprobante);

    /** Id de `tipo_documento` para el tipo de comprobante (TD001 / TD002). */
    String obtenerIdTipoDocumento(String tipoComprobante);

    /** Monto maximo de una boleta sin RUC (700.00). */
    java.math.BigDecimal obtenerLimiteBoletaSinRuc();

    /**
     * URL donde el adquirente puede consultar la representacion impresa del
     * comprobante. Es la leyenda obligatoria "Representacion impresa de ...,
     * consulte en ...".
     */
    String obtenerUrlRepresentacionImpresa();

    /** Cliente por defecto para las ventas de mostrador (consumidor final). */
    String obtenerClienteMostrador();

    /** Persona asociada al cliente de mostrador. */
    String obtenerPersonaMostrador();

    String obtenerDescripcionTipoComprobante(String tipoComprobante);
}
