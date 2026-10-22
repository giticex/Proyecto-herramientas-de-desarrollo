package com.example.proyec_herramientas.exception;

/**
 * Error de validacion fiscal u operativa al emitir un comprobante de pago
 * electronico (datos del receptor incompletos, limite de boleta sin RUC
 * superado, medios de pago que no cuadran, etc.).
 *
 * <p>Se traduce a una respuesta HTTP 400 en {@link GlobalExceptionHandler}.</p>
 */
public class ValidacionComprobanteException extends RuntimeException {

    public ValidacionComprobanteException(String mensaje) {
        super(mensaje);
    }
}
