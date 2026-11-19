package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.EmisorDTO;

/**
 * Datos del emisor y parametros fiscales tomados de la tabla `parametro`.
 */
public interface EmisorService {

    EmisorDTO obtenerEmisor();

    String obtenerRucLegado();

    /** Tasa de IGV vigente en porcentaje (18.00). */
    java.math.BigDecimal obtenerIgvPorcentaje();

    /** Moneda del comprobante (PEN). */
    String obtenerMoneda();

    /** Id de `tipo_documento` para el tipo de comprobante (TD001 / TD002). */
    String obtenerIdTipoDocumento(String tipoComprobante);

    /** Monto maximo de una boleta sin RUC (700.00). */
    java.math.BigDecimal obtenerLimiteBoletaSinRuc();

    /** Cliente por defecto para las ventas de mostrador (consumidor final). */
    String obtenerClienteMostrador();

    /** Persona asociada al cliente de mostrador. */
    String obtenerPersonaMostrador();

    String obtenerDescripcionTipoComprobante(String tipoComprobante);
}
