package com.example.proyec_herramientas.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Sin este handler el catch-all de Exception convertia cualquier ruta
    // inexistente (por ejemplo los alias sin /api que se eliminaron) en un
    // 500 en lugar de un 404.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaInexistente(NoResourceFoundException e) {
        return construir(HttpStatus.NOT_FOUND, "No existe la ruta solicitada");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + e.getMethod() + " no está permitido en esta ruta");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> stock(StockInsuficienteException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> duplicado(RecursoDuplicadoException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ValidacionComprobanteException.class)
    public ResponseEntity<Map<String, Object>> comprobante(ValidacionComprobanteException e) {
        return construir(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ValidacionSolicitudException.class)
    public ResponseEntity<Map<String, Object>> solicitud(ValidacionSolicitudException e) {
        return construir(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Solicitud inválida");
        return construir(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String mensaje = e.getReason() == null ? e.getMessage() : e.getReason();
        return construir(status == null ? HttpStatus.BAD_REQUEST : status, mensaje);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denegado(AccessDeniedException e) {
        return construir(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es JSON válido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException e) {
        return construir(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        LOG.error("Error no controlado", e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}