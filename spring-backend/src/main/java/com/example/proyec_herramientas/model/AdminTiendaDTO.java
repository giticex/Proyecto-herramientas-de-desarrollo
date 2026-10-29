package com.example.proyec_herramientas.model;

/**
 * Cuenta ADMIN que opera una tienda, vista desde la consola global para dar
 * soporte. Nunca incluye la contraseña ni su hash: solo el formato del hash
 * guardado, para detectar cuentas que aún usan el SHA-256 histórico.
 */
public record AdminTiendaDTO(
        Integer idTienda,
        String tiendaNombre,
        String estadoAcceso,
        Integer idUsuario,
        String usuario,
        String nombreCompleto,
        String correo,
        String rol,
        String estadoRol,
        String formatoContrasena) {
}
