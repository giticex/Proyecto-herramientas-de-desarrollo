package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/**
 * Fila de la tabla de empresas del SUPER_ADMIN: identificacion de la tienda y
 * sus metricas de actividad, para comparar empresas de un vistazo.
 */
public record EmpresaResumenDTO(
        Integer idTienda,
        String nombre,
        String rubro,
        String slug,
        String colorPrimario,
        String logoUrl,
        long totalProductos,
        long productosSinStock,
        long totalVentas,
        BigDecimal ventasHoy,
        BigDecimal ventasMes,
        BigDecimal ventasTotales,
        String ultimaVenta,
        String estado,
        String estadoAcceso,
        String plan,
        Integer limiteProductos,
        Integer limiteUsuarios,
        long totalUsuarios) {
}
