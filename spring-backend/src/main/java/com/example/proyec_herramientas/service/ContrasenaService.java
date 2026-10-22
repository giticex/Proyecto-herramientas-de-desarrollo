package com.example.proyec_herramientas.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Verificacion de contrasenas. La carga inicial guarda SHA-256 en hexadecimal
// (ver database/script_mysql.sql), asi que ese es el formato que se acepta.
// Si la cuenta ya tiene un hash BCrypt (prefijo "$2"), tambien se valida:
// asi el cambio de algoritmo se puede hacer cuenta por cuenta.
@Service
public class ContrasenaService {

    private static final String PREFIJO_BCRYPT = "$2";

    public String hash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }

    public boolean coincide(String texto, String hashGuardado) {
        if (texto == null || hashGuardado == null || hashGuardado.isBlank()) {
            return false;
        }
        if (hashGuardado.startsWith(PREFIJO_BCRYPT)) {
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .matches(texto, hashGuardado);
        }
        return comparar(hashGuardado, hash(texto));
    }

    // Comparacion en tiempo constante para no filtrar informacion por el
    // tiempo que tarda en responder.
    private boolean comparar(String esperado, String calculado) {
        byte[] a = esperado.getBytes(StandardCharsets.UTF_8);
        byte[] b = calculado.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int diferencia = 0;
        for (int i = 0; i < a.length; i++) {
            diferencia |= a[i] ^ b[i];
        }
        return diferencia == 0;
    }
}
