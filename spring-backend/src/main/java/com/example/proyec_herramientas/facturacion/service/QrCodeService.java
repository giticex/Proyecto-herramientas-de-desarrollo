package com.example.proyec_herramientas.facturacion.service;

import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import com.itextpdf.text.pdf.qrcode.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

/**
 * Genera la imagen PNG del codigo QR del comprobante.
 * El contenido lo arma {@link HashService}; aqui solo se dibuja.
 *
 * <p>La RSM N.° 300-2019/SUNAT (Anexo I, numeral 6.4) exige simbologia QR Code
 * 2005 (ISO/IEC 18004:2006), nivel de correccion de error <b>Q</b> y
 * codificacion UTF-8. El tamano maximo es de 6 cm por 6 cm (incluida la zona
 * de silencio) y debe ir en la parte inferior de la representacion impresa.</p>
 */
@Service
public class QrCodeService {

    /** Lado en pixeles de la imagen generada. */
    private static final int LADO_PX = 240;

    /**
     * Devuelve el QR en PNG codificado en base64 (listo para embeber en el
     * PDF o para el atributo src de un {@code <img>} del frontend).
     */
    public String generarPngBase64(String contenido) {
        byte[] png = generarPng(contenido);
        return Base64.getEncoder().encodeToString(png);
    }

    public byte[] generarPng(String contenido) {
        BufferedImage imagen = generarImagen(contenido);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el codigo QR", e);
        }
    }

    /**
     * Dibuja el QR como imagen. Los modulos se escalan con un factor entero
     * para que las aristas queden nitidas y la zona de silencio se respete.
     */
    public BufferedImage generarImagen(String contenido) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);

        BarcodeQRCode barcode = new BarcodeQRCode(contenido, LADO_PX, LADO_PX, hints);
        BufferedImage nativa = aBufferedImage(barcode.createAwtImage(Color.BLACK, Color.WHITE));

        int escala = Math.max(1, LADO_PX / Math.max(nativa.getWidth(), nativa.getHeight()));
        int lado = Math.max(nativa.getWidth(), nativa.getHeight()) * escala;
        int margen = (LADO_PX - lado) / 2;

        BufferedImage salida = new BufferedImage(LADO_PX, LADO_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = salida.createGraphics();
        graficos.setColor(Color.WHITE);
        graficos.fillRect(0, 0, LADO_PX, LADO_PX);
        graficos.setColor(Color.BLACK);
        for (int y = 0; y < nativa.getHeight(); y++) {
            for (int x = 0; x < nativa.getWidth(); x++) {
                if (esOscuro(nativa.getRGB(x, y))) {
                    graficos.fillRect(margen + x * escala, margen + y * escala, escala, escala);
                }
            }
        }
        graficos.dispose();
        return salida;
    }

    private boolean esOscuro(int rgb) {
        int rojo = (rgb >> 16) & 0xFF;
        int verde = (rgb >> 8) & 0xFF;
        int azul = rgb & 0xFF;
        return (rojo + verde + azul) / 3 < 128;
    }

    private BufferedImage aBufferedImage(java.awt.Image imagen) {
        if (imagen instanceof BufferedImage) {
            return (BufferedImage) imagen;
        }
        BufferedImage salida = new BufferedImage(imagen.getWidth(null), imagen.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = salida.createGraphics();
        graficos.setColor(Color.WHITE);
        graficos.fillRect(0, 0, salida.getWidth(), salida.getHeight());
        graficos.drawImage(imagen, 0, 0, null);
        graficos.dispose();
        return salida;
    }
}
