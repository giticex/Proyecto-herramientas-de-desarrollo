package com.example.proyec_herramientas.model;

import java.util.List;

// Personal de una tienda visto desde su propio panel. Solo incluye cuentas
// VENDEDOR; el cupo del plan mide puestos de trabajo y suma tambien al ADMIN.
public record PersonalTiendaDTO(
        Integer idTienda,
        String tiendaNombre,
        int limitePersonal,
        long personalEnUso,
        List<UsuarioStaffDTO> personal) {
}
