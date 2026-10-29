package com.example.proyec_herramientas.model;

import java.util.List;

/** Dashboard de una empresa concreta: sus metricas y sus productos estrella. */
public record SuperAdminEmpresaDTO(
        EmpresaResumenDTO empresa,
        List<ProductoVendidoDTO> topProductos) {
}
