package com.example.proyec_herramientas.model;

/** Fila de la bitacora global de la plataforma. */
public record AuditoriaEventoDTO(
        Long idEvento,
        String fecha,
        String tipo,
        String severidad,
        String descripcion,
        Integer idTienda,
        String tiendaNombre,
        String actor) {
}
