package com.example.proyec_herramientas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Token de sesion opaco y firmado. Formato: payload.firma, donde el payload
// es "usuario|rol|idCliente|idTienda|expira" en base64url y la firma es
// HMAC-SHA256 del payload con el secreto del servidor.
//
// El rol, el cliente y la tienda viajan dentro del token justamente para no
// volver a consultarlos en cada peticion; el token esta firmado, asi que no
// se puede alterar a mano.
@Service
public class TokenService {

    private static final String ALGORITMO = "HmacSHA256";
    private static final String SEPARADOR = ".";
    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    private final byte[] secreto;
    private final long expiracionMinutos;

    // Sin secreto configurado se genera uno aleatorio por arranque: el valor
    // por defecto conocido permitia firmar tokens validos en cualquier equipo.
    // Con secreto aleatorio, reiniciar el backend invalida las sesiones.
    public TokenService(@Value("${app.token.secreto:}") String secreto,
            @Value("${app.token.expiracion-minutos:120}") long expiracionMinutos) {
        String valor = secreto == null ? "" : secreto.trim();
        if (valor.isBlank()) {
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.secreto = aleatorio;
            LOG.warn("APP_TOKEN_SECRETO no esta configurado: se genero un secreto aleatorio "
                    + "para esta ejecucion. Configura la variable en produccion.");
        } else {
            this.secreto = valor.getBytes(StandardCharsets.UTF_8);
        }
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(String usuario, String rol, String idCliente, Integer idTienda) {
        long expira = System.currentTimeMillis() / 1000 + expiracionMinutos * 60;
        String payload = usuario + "|" + rol + "|" + (idCliente == null ? "" : idCliente)
                + "|" + (idTienda == null ? "" : idTienda) + "|" + expira;
        String codificado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return codificado + SEPARADOR + firmar(codificado);
    }

    // Devuelve null si el token no es valido, esta manipulado o ya expiro.
    // Acepta el payload de 4 campos (sin idTienda) que se emitia antes de
    // multi-tienda, para no cerrar sesiones de golpe al desplegar.
    public Token validar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] partes = token.split("\\" + SEPARADOR, -1);
        if (partes.length != 2) {
            return null;
        }
        String codificado = partes[0];
        String firma = partes[1];
        if (!MessageDigest.isEqual(firma.getBytes(StandardCharsets.UTF_8),
                firmar(codificado).getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(codificado), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String[] campos = payload.split("\\|", -1);
        if (campos.length != 4 && campos.length != 5) {
            return null;
        }
        String idTiendaTexto = campos.length == 5 ? campos[3] : "";
        String expiraTexto = campos[campos.length - 1];
        long expira;
        Integer idTienda;
        try {
            expira = Long.parseLong(expiraTexto);
            idTienda = idTiendaTexto.isBlank() ? null : Integer.valueOf(idTiendaTexto);
        } catch (NumberFormatException e) {
            return null;
        }
        if (expira < System.currentTimeMillis() / 1000) {
            return null;
        }
        return new Token(campos[0], campos[1], campos[2], idTienda, expira);
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    // Datos que viajan dentro del token. idCliente va vacio para las cuentas
    // del panel; idTienda va vacio para los compradores.
    public record Token(String usuario, String rol, String idCliente, Integer idTienda, long expira) {
    }
}
