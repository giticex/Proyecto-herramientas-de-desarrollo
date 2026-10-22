package com.example.proyec_herramientas.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

// Firma y valida los JWT (HS256, 8 horas). El secreto sale de JWT_SECRET,
// nunca se escribe en el codigo.
@Component
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ID_TIENDA = "idTienda";
    private static final long VIGENCIA_HORAS = 8;

    private final Algorithm algoritmo;

    public JwtService(@Value("${jwt.secret}") String secreto) {
        this.algoritmo = Algorithm.HMAC256(secreto);
    }

    public String generar(String subject, String rol, Integer idTienda) {
        JWTCreator.Builder builder = JWT.create()
                .withSubject(subject)
                .withClaim(CLAIM_ROL, rol)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(VIGENCIA_HORAS, ChronoUnit.HOURS)));
        if (idTienda != null) {
            builder.withClaim(CLAIM_ID_TIENDA, idTienda);
        }
        return builder.sign(algoritmo);
    }

    // Devuelve null si el token no es valido o expiro.
    public UsuarioAutenticado validar(String token) {
        try {
            DecodedJWT jwt = JWT.require(algoritmo).build().verify(token);
            String id = jwt.getSubject();
            String rol = jwt.getClaim(CLAIM_ROL).asString();
            Integer idTienda = jwt.getClaim(CLAIM_ID_TIENDA).asInt();
            return new UsuarioAutenticado(id, rol, idTienda);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
