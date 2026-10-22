package com.example.proyec_herramientas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Soporte para los hashes SHA-256 de la carga inicial (usuario y cliente).
// Las contrasenas nuevas siempre usan BCrypt; esto solo sirve para aceptar
// el hash viejo en el primer login y reemplazarlo ahi mismo.
public final class HashLegado {

    private HashLegado() {
    }

    public static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public static boolean esHashBCrypt(String hash) {
        return hash != null
                && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
}
