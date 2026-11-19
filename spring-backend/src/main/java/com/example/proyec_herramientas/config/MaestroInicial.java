package com.example.proyec_herramientas.config;

import com.example.proyec_herramientas.service.AuthServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class MaestroInicial implements ApplicationRunner {
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final AuthServiceImpl auth;
    private final String correo;
    private final String clave;

    public MaestroInicial(AuthServiceImpl auth, @Value("${APP_MAESTRO_CORREO:}") String correo,
            @Value("${APP_MAESTRO_CLAVE:}") String clave) {
        this.auth = auth;
        this.correo = correo.strip();
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (correo.isEmpty() && clave.isEmpty()) return;
        if (correo.length() > 50 || !CORREO.matcher(correo).matches()
                || clave.codePointCount(0, clave.length()) < 12
                || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configura un correo maestro y una contraseña válida en el entorno privado.");
        }
        auth.aprovisionarMaestro(correo, clave);
    }
}
