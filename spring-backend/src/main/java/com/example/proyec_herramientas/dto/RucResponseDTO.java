package com.example.proyec_herramientas.dto;

/** Respuesta de la consulta de RUC a SUNAT. */
public record RucResponseDTO(
        String numero,
        String razonSocial,
        String direccion,
        String estado) {
}
