package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/** KPI globales de toda la plataforma para la consola del SUPER_ADMIN. */
public record SuperAdminResumenDTO(
        long totalEmpresas,
        long totalProductos,
        long productosSinStock,
        long totalVentas,
        BigDecimal ingresoHoy,
        BigDecimal ingresoMes,
        BigDecimal ingresoTotal) {
}
