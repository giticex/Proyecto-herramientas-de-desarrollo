package com.example.proyec_herramientas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

// Mismo formato de error {status, error} que usa GlobalExceptionHandler,
// pero para los rechazos que Spring Security resuelve antes de llegar a un
// controller (401 sin token, 403 con rol insuficiente).
final class SeguridadRespuestas {

    private SeguridadRespuestas() {
    }

    static void escribir(ObjectMapper objectMapper, HttpServletResponse response, HttpStatus status, String mensaje)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", mensaje);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
