package com.example.proyec_herramientas.model;

import java.util.List;

/** Pagina del historial de ventas con los datos de navegacion. */
public record PaginaVentasDTO(
        List<VentaResumenDTO> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
