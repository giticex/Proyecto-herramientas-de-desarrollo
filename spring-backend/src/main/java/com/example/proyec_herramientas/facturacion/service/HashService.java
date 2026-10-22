package com.example.proyec_herramientas.facturacion.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Calcula el hash del comprobante y construye el contenido del codigo QR
 * segun la RSM N.° 300-2019/SUNAT (Anexo I, numeral 6).
 *
 * <p>El <b>Valor Resumen</b> es "la cadena resumen en base 64, el cual es el
 * resultado de aplicar la funcion hash al formato XML que representa el
 * comprobante de pago electronico". En la practica es el digest SHA-256 del
 * XML codificado en base 64.</p>
 *
 * <p>El codigo QR consigna, separados por barra vertical ("|"), en este orden:</p>
 *
 * <pre>
 * RUC | TIPO DE DOCUMENTO | SERIE | NÚMERO | MONTO TOTAL IGV | MONTO TOTAL DEL COMPROBANTE |
 * FECHA DE EMISIÓN | TIPO DE DOCUMENTO ADQUIRENTE | NÚMERO DE DOCUMENTO ADQUIRENTE | VALOR RESUMEN
 * </pre>
 *
 * <p>Los importes y la fecha se consignan con el mismo formato que se usa en
 * el comprobante electronico (dos decimales y {@code yyyy-MM-dd}).</p>
 */
@Service
public class HashService {

    /** Digitos del correlativo, igual que en el cbc:ID del XML (F001-000001). */
    private static final int DIGITOS_CORRELATIVO = 6;

    /**
     * SHA-256 en hexadecimal del XML del comprobante.
     */
    public String calcularHashXml(String xml) {
        return toHex(digest(xml));
    }

    /**
     * Valor Resumen: cadena resumen en base 64 del hash SHA-256 del XML.
     * Es el ultimo campo del codigo QR y debe consignarse en la parte
     * inferior de la representacion impresa.
     */
    public String calcularValorResumen(String xml) {
        return Base64.getEncoder().encodeToString(digest(xml));
    }

    /**
     * Valor Resumen a partir del hash hexadecimal ya almacenado en la venta:
     * evita tener que regenerar el XML solo para recuperar la cadena base 64.
     */
    public String valorResumenDesdeHash(String hashHexadecimal) {
        if (hashHexadecimal == null || hashHexadecimal.isBlank()) {
            return "";
        }
        String hex = hashHexadecimal.trim();
        if (hex.length() % 2 != 0) {
            return "";
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Construye el contenido del codigo QR con el orden del Anexo I de la
     * RSM 300-2019/SUNAT.
     *
     * @param ruc                  RUC del emisor
     * @param tipoDocumento        tipo de comprobante (01 factura, 03 boleta)
     * @param serie                serie del comprobante (F001 / B001)
     * @param correlativo          correlativo numerico de la serie
     * @param montoIgv             sumatoria del IGV
     * @param montoTotal           importe total del comprobante
     * @param fechaEmision         fecha de emision en formato yyyy-MM-dd
     * @param tipoDocAdquirente    tipo de documento del adquirente (1 RUC, 6 DNI)
     * @param numDocAdquirente     numero de documento del adquirente
     * @param valorResumen         Valor Resumen (base 64 del hash del XML)
     */
    public String construirContenidoQr(String ruc, String tipoDocumento, String serie, int correlativo,
                                       BigDecimal montoIgv, BigDecimal montoTotal, String fechaEmision,
                                       String tipoDocAdquirente, String numDocAdquirente, String valorResumen) {
        return String.join("|",
                ruc,
                tipoDocumento,
                serie,
                String.format("%0" + DIGITOS_CORRELATIVO + "d", correlativo),
                importe(montoIgv),
                importe(montoTotal),
                fechaEmision,
                tipoDocAdquirente == null ? "" : tipoDocAdquirente,
                numDocAdquirente == null ? "" : numDocAdquirente,
                valorResumen == null ? "" : valorResumen);
    }

    /** Importe con dos decimales, igual que en el XML. */
    private String importe(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private byte[] digest(String xml) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest((xml == null ? "" : xml).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la plataforma", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
