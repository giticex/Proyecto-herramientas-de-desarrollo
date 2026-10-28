package com.example.proyec_herramientas.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Solicitud de canje de un credito: que codigo se aplica y con que monto.
 */
public record CanjeCreditoDTO(
        @NotBlank(message = "es obligatorio") String codigo,
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.01", message = "debe ser mayor a 0") BigDecimal monto) {
}
