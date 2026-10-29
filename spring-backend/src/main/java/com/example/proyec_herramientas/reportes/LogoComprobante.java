package com.example.proyec_herramientas.reportes;

import com.itextpdf.text.Image;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;

/**
 * Carga el logo de la tienda para incrustarlo en la representacion impresa.
 *
 * <p>El logo se configura como URL, por lo que la descarga es una peticion de
 * red disparada por un dato editable: solo se aceptan http/https, se rechazan
 * los destinos internos (loopback, redes privadas, link-local) para no exponer
 * la red del servidor, no se siguen redirecciones, se limitan los tiempos de
 * espera y el tamano. Cualquier fallo devuelve {@code null}: el logo es
 * opcional y nunca debe impedir imprimir el comprobante.</p>
 */
public final class LogoComprobante {

    private static final int TIEMPO_CONEXION_MS = 2500;
    private static final int TIEMPO_LECTURA_MS = 3500;
    private static final int TAMANO_MAXIMO_BYTES = 1_500_000;

    private LogoComprobante() {
    }

    /** Devuelve el logo listo para incrustar o {@code null} si no se puede usar. */
    public static Image cargar(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        HttpURLConnection conexion = null;
        try {
            URI uri = URI.create(url.trim());
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!"http".equals(esquema) && !"https".equals(esquema)) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank() || !esHostPublico(host)) {
                return null;
            }

            conexion = (HttpURLConnection) uri.toURL().openConnection();
            conexion.setConnectTimeout(TIEMPO_CONEXION_MS);
            conexion.setReadTimeout(TIEMPO_LECTURA_MS);
            conexion.setInstanceFollowRedirects(false);
            conexion.setRequestProperty("Accept", "image/*");
            if (conexion.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            String tipo = conexion.getContentType();
            if (tipo == null || !tipo.toLowerCase().startsWith("image/")) {
                return null;
            }
            byte[] bytes = leerLimitado(conexion.getInputStream());
            return bytes == null ? null : Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private static boolean esHostPublico(String host) throws Exception {
        for (InetAddress direccion : InetAddress.getAllByName(host)) {
            if (direccion.isAnyLocalAddress()
                    || direccion.isLoopbackAddress()
                    || direccion.isLinkLocalAddress()
                    || direccion.isSiteLocalAddress()
                    || direccion.isMulticastAddress()) {
                return false;
            }
        }
        return true;
    }

    private static byte[] leerLimitado(InputStream entrada) throws Exception {
        try (InputStream flujo = entrada) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = flujo.read(buffer)) != -1) {
                if (salida.size() + leidos > TAMANO_MAXIMO_BYTES) {
                    return null;
                }
                salida.write(buffer, 0, leidos);
            }
            return salida.toByteArray();
        }
    }
}
