package com.example.proyec_herramientas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Se dispara cuando falta un token valido en una ruta protegida.
@Component
public class SeguridadEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public SeguridadEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        SeguridadRespuestas.escribir(objectMapper, response, HttpStatus.UNAUTHORIZED, "No autenticado.");
    }
}
