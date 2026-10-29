package com.example.proyec_herramientas.model;

import java.util.List;

/** Pagina de la bitacora global con sus datos de navegacion. */
public record PaginaAuditoriaDTO(
        List<AuditoriaEventoDTO> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
