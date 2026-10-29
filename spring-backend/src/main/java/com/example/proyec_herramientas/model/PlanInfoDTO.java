package com.example.proyec_herramientas.model;

/** Plan que ofrece la plataforma con los limites que trae por defecto. */
public record PlanInfoDTO(
        String codigo,
        String nombre,
        int limiteProductos,
        int limiteUsuarios) {
}
