package com.example.proyec_herramientas.facturacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un importe a letras en espanol, como exige la representacion
 * impresa del comprobante ("Son: MIL QUINIENTOS SETENTA Y CINCO CON 00/100").
 */
public final class NumerosALetras {

    private static final String[] UNIDADES = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };

    private static final String[] DECENAS = {
            "", "", "VEINTI", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private NumerosALetras() {
    }

    public static String convertir(BigDecimal valor) {
        BigDecimal redondeado = valor.setScale(2, RoundingMode.HALF_UP);
        long entero = redondeado.longValue();
        int centimos = redondeado.subtract(BigDecimal.valueOf(entero)).multiply(new BigDecimal("100")).intValue();

        String letras = convertirEntero(entero);
        String centavos = convertirEntero(centimos);
        return letras + " CON " + centavos + "/100";
    }

    private static String convertirEntero(long numero) {
        if (numero == 0) {
            return "CERO";
        }
        StringBuilder texto = new StringBuilder();
        long millares = numero / 1000;
        long resto = numero % 1000;

        if (millares > 0) {
            if (millares == 1) {
                texto.append("MIL ");
            } else {
                texto.append(convertirEntero(millares)).append(" MILLONES ");
            }
        }
        texto.append(convertirCientos((int) resto));
        return texto.toString().trim();
    }

    private static String convertirCientos(int numero) {
        if (numero == 0) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        int centena = numero / 100;
        int resto = numero % 100;

        if (centena > 0) {
            texto.append(resto == 0 && centena == 1 ? "CIEN" : CENTENAS[centena]);
            if (resto > 0) {
                texto.append(' ');
            }
        }
        if (resto > 0) {
            if (resto < 30) {
                texto.append(decena(resto));
            } else {
                texto.append(DECENAS[resto / 10]);
                int unidad = resto % 10;
                if (unidad > 0) {
                    texto.append(" Y ").append(UNIDADES[unidad]);
                }
            }
        }
        return texto.toString();
    }

    private static String decena(int numero) {
        if (numero <= 20) {
            return UNIDADES[numero];
        }
        String unidad = UNIDADES[numero % 10];
        return "VEINTI" + ("UNO".equals(unidad) ? "UNO" : unidad);
    }
}
