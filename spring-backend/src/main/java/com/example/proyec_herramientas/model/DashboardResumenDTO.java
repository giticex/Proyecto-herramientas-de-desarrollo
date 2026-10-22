package com.example.proyec_herramientas.model;

import java.math.BigDecimal;
import java.util.List;

// Resumen que alimenta las tarjetas y el grafico del dashboard del ADMIN.
// Todo esta acotado a la tienda del token: un ADMIN nunca ve ventas de otra.
public record DashboardResumenDTO(
        BigDecimal ventasHoy,
        BigDecimal ingresosMes,
        List<ProductoVendidoDTO> topProductos) {
}
