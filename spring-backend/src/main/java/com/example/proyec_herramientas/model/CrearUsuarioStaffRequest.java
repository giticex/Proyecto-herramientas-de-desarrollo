package com.example.proyec_herramientas.model;

// Alta de una cuenta de staff desde la consola global. Solo admite los roles
// ADMIN y VENDEDOR: la cuenta queda ligada a la tienda del path.
public record CrearUsuarioStaffRequest(
        String usuario,
        String nombre,
        String apellido,
        String correo,
        String rol,
        String password) {
}
