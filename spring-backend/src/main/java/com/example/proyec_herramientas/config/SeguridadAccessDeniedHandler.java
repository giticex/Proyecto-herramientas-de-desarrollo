package com.example.proyec_herramientas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Se dispara cuando hay token valido pero el rol no alcanza (ej. CLIENTE
// intentando escribir /productos, o ADMIN intentando /ventas).
@Component
public class SeguridadAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SeguridadAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        SeguridadRespuestas.escribir(objectMapper, response, HttpStatus.FORBIDDEN, "No tienes permiso para esta acción.");
    }
}
