package com.example.proyec_herramientas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

    private final byte[] secreto;
    private final long expiracionMinutos;

    public TokenService(@Value("${app.token.secreto}") String secreto,
            @Value("${app.token.expiracion-minutos:120}") long expiracionMinutos) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
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
        if (!firma.equals(firmar(codificado))) {
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
