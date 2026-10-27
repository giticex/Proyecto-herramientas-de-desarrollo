package com.example.proyec_herramientas.dto;

/** Respuesta de la consulta de DNI a RENIEC. */
public record DniResponseDTO(
        String numero,
        String nombres,
        String apellidos,
        String direccion,
        String estado) {
}
