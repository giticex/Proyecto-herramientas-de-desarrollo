package com.example.proyec_herramientas.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de reembolso en caja de un credito vigente.
 */
public record ReembolsoCreditoDTO(
        @NotBlank(message = "es obligatorio") String codigo) {
}
