package com.example.proyec_herramientas.exception;

/**
 * Datos invalidos en una solicitud del panel (configuracion, filtros, etc.).
 * Se traduce a una respuesta HTTP 400 en {@link GlobalExceptionHandler}.
 */
public class ValidacionSolicitudException extends RuntimeException {

    public ValidacionSolicitudException(String mensaje) {
        super(mensaje);
    }
}
