package com.example.proyec_herramientas.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Verificacion de contrasenas. La carga inicial guarda SHA-256 en hexadecimal
// (ver database/script_mysql.sql) y las cuentas nuevas guardan BCrypt. El
// login acepta los dos formatos y, cuando valida con el hash viejo, lo
// reemplaza por BCrypt (migracion transparente, ver AuthServiceImpl).
@Service
public class ContrasenaService {

    private static final String PREFIJO_BCRYPT = "$2";

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /** SHA-256 en hexadecimal. Solo se usa para verificar la carga inicial. */
    public String hash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }

    /** Hash definitivo para cuentas nuevas y para la migracion desde SHA-256. */
    public String hashBcrypt(String texto) {
        return bcrypt.encode(texto);
    }

    /** true si el hash guardado es el SHA-256 historico (sin sal). */
    public boolean esHashLegado(String hashGuardado) {
        return hashGuardado != null && !hashGuardado.isBlank()
                && !hashGuardado.startsWith(PREFIJO_BCRYPT);
    }

    public boolean coincide(String texto, String hashGuardado) {
        if (texto == null || hashGuardado == null || hashGuardado.isBlank()) {
            return false;
        }
        if (hashGuardado.startsWith(PREFIJO_BCRYPT)) {
            return bcrypt.matches(texto, hashGuardado);
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
