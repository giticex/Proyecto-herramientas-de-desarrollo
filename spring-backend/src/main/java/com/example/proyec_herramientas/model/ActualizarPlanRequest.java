package com.example.proyec_herramientas.model;

/** Cambio de plan o de limites solicitado por el SUPER_ADMIN. */
public record ActualizarPlanRequest(
        String plan,
        Integer limiteProductos,
        Integer limiteUsuarios) {
}
