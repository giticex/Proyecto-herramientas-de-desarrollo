package com.example.proyec_herramientas.model;

import java.util.List;

/** Plan contratado por una tienda, su consumo actual y el catalogo de planes. */
public record PlanTiendaDTO(
        Integer idTienda,
        String nombre,
        String estadoAcceso,
        String plan,
        int limiteProductos,
        int limiteUsuarios,
        long totalProductos,
        long totalUsuarios,
        List<PlanInfoDTO> planes) {
}
