package com.example.proyec_herramientas.model;

// Edicion de una cuenta de staff. La contrasena es opcional: si llega vacia
// se conserva la actual.
public record ActualizarUsuarioStaffRequest(
        String usuario,
        String nombre,
        String apellido,
        String correo,
        String rol,
        String password) {
}
