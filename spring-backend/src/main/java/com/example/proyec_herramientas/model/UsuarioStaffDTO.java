package com.example.proyec_herramientas.model;

// Cuenta de staff de una tienda vista desde la consola global. No incluye la
// contrasena: solo el formato del hash guardado.
public record UsuarioStaffDTO(
        Integer idUsuario,
        Integer idTienda,
        String usuario,
        String nombre,
        String apellido,
        String nombreCompleto,
        String correo,
        String rol,
        String estado,
        String formatoContrasena) {
}
