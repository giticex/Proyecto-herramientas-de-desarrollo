package com.example.proyec_herramientas.reportes;

import com.itextpdf.text.BaseColor;

/**
 * Marca de la tienda usada en la representacion impresa. El color primario
 * pinta la cabecera y los totales; el secundario, los titulos. Si la tienda no
 * tiene colores validos se usa una paleta corporativa neutra. El logo es
 * opcional: si no se puede leer, el comprobante se imprime sin el.
 */
public record MarcaComprobante(String colorPrimario, String colorSecundario, String logoUrl) {

    private static final String PRIMARIO_DEFECTO = "#0B4A6F";
    private static final String SECUNDARIO_DEFECTO = "#0F172A";

    public MarcaComprobante(String colorPrimario, String colorSecundario) {
        this(colorPrimario, colorSecundario, null);
    }

    public static MarcaComprobante porDefecto() {
        return new MarcaComprobante(PRIMARIO_DEFECTO, SECUNDARIO_DEFECTO);
    }

    public BaseColor primario() {
        return color(colorPrimario, PRIMARIO_DEFECTO);
    }

    public BaseColor secundario() {
        return color(colorSecundario, SECUNDARIO_DEFECTO);
    }

    public BaseColor primarioSuave() {
        return mezclarConBlanco(primario(), 0.9f);
    }

    /** Texto legible encima del color primario (negro o blanco). */
    public BaseColor textoPrimario() {
        BaseColor fondo = primario();
        double luminancia = (0.299 * fondo.getRed() + 0.587 * fondo.getGreen() + 0.114 * fondo.getBlue()) / 255d;
        return luminancia > 0.62 ? new BaseColor(0x11, 0x18, 0x27) : BaseColor.WHITE;
    }

    private static BaseColor color(String hex, String defecto) {
        String valor = hex == null ? "" : hex.trim();
        if (valor.startsWith("#")) {
            valor = valor.substring(1);
        }
        if (valor.length() == 3) {
            valor = "" + valor.charAt(0) + valor.charAt(0)
                    + valor.charAt(1) + valor.charAt(1)
                    + valor.charAt(2) + valor.charAt(2);
        }
        if (!valor.matches("[0-9a-fA-F]{6}")) {
            valor = defecto.substring(1);
        }
        // Se usa el constructor de tres componentes: el de un solo entero deja
        // el canal alfa en 0 y iText lo aplica como opacidad nula (texto invisible).
        int rgb = Integer.parseInt(valor, 16);
        return new BaseColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private static BaseColor mezclarConBlanco(BaseColor color, float blanco) {
        float tinte = 1f - blanco;
        return new BaseColor(
                Math.round(color.getRed() * tinte + 255 * blanco),
                Math.round(color.getGreen() * tinte + 255 * blanco),
                Math.round(color.getBlue() * tinte + 255 * blanco));
    }
}
