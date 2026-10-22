package com.example.proyec_herramientas.reportes;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.facturacion.model.DetalleComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.DomicilioDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import com.example.proyec_herramientas.facturacion.model.LeyendaDTO;
import com.example.proyec_herramientas.facturacion.model.MedioPagoDTO;
import com.example.proyec_herramientas.facturacion.model.ReceptorDTO;
import com.example.proyec_herramientas.facturacion.model.TotalesDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;

/**
 * Representacion impresa del comprobante de pago electronico.
 *
 * <p>La RSM 300-2019/SUNAT (Anexo I, numeral 6) exige que el codigo QR se
 * imprima en la parte inferior de la representacion, con un tamano maximo de
 * 6 cm por 6 cm (incluida la zona de silencio de 1 mm) y en color negro. El
 * Valor Resumen puede consignarse fuera del QR.</p>
 */
public class ComprobantePdfGenerator {

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_BOLD_SMALL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);

    /** Lado del codigo QR impreso: 4,2 cm (el maximo_normativo es 6 cm). */
    private static final float LADO_QR_PT = 119f;

    public static byte[] generarPdfBytes(ComprobanteRespuestaDTO comprobante) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            escribir(comprobante, out);
            return out.toByteArray();
        }
    }

    private static void escribir(ComprobanteRespuestaDTO comprobante, ByteArrayOutputStream out) throws Exception {
        Document doc = new Document(PageSize.A5, 30, 30, 36, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        try {
            EmisorDTO emisor = comprobante.getEmisor();
            ReceptorDTO receptor = comprobante.getReceptor();
            TotalesDTO totales = comprobante.getTotales();

            // --- Encabezado del emisor ---
            Paragraph razonSocial = new Paragraph(emisor.getRazonSocial(), FONT_TITLE);
            razonSocial.setAlignment(Element.ALIGN_CENTER);
            doc.add(razonSocial);

            Paragraph nombreComercial = new Paragraph(
                    emisor.getNombreComercial() == null ? "" : emisor.getNombreComercial(), FONT_SMALL);
            nombreComercial.setAlignment(Element.ALIGN_CENTER);
            doc.add(nombreComercial);

            Paragraph ruc = new Paragraph("R.U.C. " + emisor.getRuc(), FONT_BOLD);
            ruc.setAlignment(Element.ALIGN_CENTER);
            doc.add(ruc);

            Paragraph domicilio = new Paragraph(emisor.getDomicilioFiscal() + " - "
                    + emisor.getDistrito() + ", " + emisor.getProvincia(), FONT_SMALL);
            domicilio.setAlignment(Element.ALIGN_CENTER);
            doc.add(domicilio);

            Paragraph titulo = new Paragraph(comprobante.getTipoComprobanteDescripcion() + " ELECTRÃ“NICA", FONT_TITLE);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(6);
            doc.add(titulo);

            Paragraph numero = new Paragraph(comprobante.getNumeroDocumento(), FONT_BOLD);
            numero.setAlignment(Element.ALIGN_CENTER);
            doc.add(numero);

            Paragraph fecha = new Paragraph("Fecha de emisiÃ³n: " + comprobante.getFechaEmision()
                    + " " + comprobante.getHoraEmision(), FONT_NORMAL);
            fecha.setAlignment(Element.ALIGN_CENTER);
            doc.add(fecha);

            doc.add(Chunk.NEWLINE);

            // --- Datos del adquirente ---
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{32, 68});
            datos.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            datos.addCell(PdfUtil.celdaSinBorde("SeÃ±or(es):", FONT_BOLD));
            datos.addCell(PdfUtil.celdaSinBorde(valor(receptor.getRazonSocial()), FONT_NORMAL));

            String tipoDocumento = "1".equals(receptor.getTipoDocumento()) ? "RUC" : "DNI";
            datos.addCell(PdfUtil.celdaSinBorde(tipoDocumento + ":", FONT_BOLD));
            datos.addCell(PdfUtil.celdaSinBorde(valor(receptor.getNumeroDocumento()), FONT_NORMAL));

            DomicilioDTO domicilioReceptor = receptor.getDomicilioFiscal();
            if (domicilioReceptor != null && !valor(domicilioReceptor.getDireccion()).isBlank()) {
                datos.addCell(PdfUtil.celdaSinBorde("DirecciÃ³n:", FONT_BOLD));
                datos.addCell(PdfUtil.celdaSinBorde(domicilioReceptor.getDireccion()
                        + " - " + valor(domicilioReceptor.getDistrito()), FONT_NORMAL));
            }
            doc.add(datos);
            doc.add(Chunk.NEWLINE);

            // --- Detalle de los items ---
            PdfPTable tabla = new PdfPTable(new float[]{0.7f, 1.1f, 4.4f, 1.8f, 1.8f, 1.4f, 1.8f});
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{0.7f, 1.1f, 4.4f, 1.8f, 1.8f, 1.4f, 1.8f});

            PdfUtil.addHeader(tabla, "NÂ°", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "CANT", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "DESCRIPCIÃ“N", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "V. UNIT", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "P. UNIT", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "IGV", FONT_BOLD_SMALL);
            PdfUtil.addHeader(tabla, "IMPORTE", FONT_BOLD_SMALL);

            for (DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
                tabla.addCell(PdfUtil.celda(String.valueOf(detalle.getNumero()), FONT_SMALL, Element.ALIGN_CENTER));
                tabla.addCell(PdfUtil.celda(cantidad(detalle.getCantidad()), FONT_SMALL, Element.ALIGN_CENTER));
                tabla.addCell(PdfUtil.celda(valor(detalle.getDescripcion()), FONT_SMALL, Element.ALIGN_LEFT));
                tabla.addCell(PdfUtil.celda(importe(detalle.getValorUnitario()), FONT_SMALL, Element.ALIGN_RIGHT));
                tabla.addCell(PdfUtil.celda(importe(detalle.getPrecioUnitario()), FONT_SMALL, Element.ALIGN_RIGHT));
                tabla.addCell(PdfUtil.celda(importe(detalle.getIgv()), FONT_SMALL, Element.ALIGN_RIGHT));
                tabla.addCell(PdfUtil.celda(importe(detalle.getTotalItem()), FONT_SMALL, Element.ALIGN_RIGHT));
            }
            doc.add(tabla);
            doc.add(Chunk.NEWLINE);

            // --- Totales ---
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(62);
            tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaTotales.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            if (esMayorCero(totales.getOpGravada())) {
                fila(tablaTotales, "Op. Gravada:", importe(totales.getOpGravada()), FONT_NORMAL);
            }
            if (esMayorCero(totales.getOpInafecta())) {
                fila(tablaTotales, "Op. Inafecta:", importe(totales.getOpInafecta()), FONT_NORMAL);
            }
            if (esMayorCero(totales.getOpExonerada())) {
                fila(tablaTotales, "Op. Exonerada:", importe(totales.getOpExonerada()), FONT_NORMAL);
            }
            if (esMayorCero(totales.getOpGratuita())) {
                fila(tablaTotales, "Op. Gratuitas:", importe(totales.getOpGratuita()), FONT_NORMAL);
            }
            if (esMayorCero(totales.getTotalDescuentos())) {
                fila(tablaTotales, "Descuentos:", importe(totales.getTotalDescuentos()), FONT_NORMAL);
            }
            fila(tablaTotales, "I.G.V. (18%):", importe(totales.getIgv()), FONT_NORMAL);
            fila(tablaTotales, "IMPORTE TOTAL:", importe(totales.getMontoTotalPagar()) + " " + comprobante.getMoneda(),
                    FONT_BOLD);
            doc.add(tablaTotales);

            Paragraph letras = new Paragraph("SON: " + valor(totales.getTotalEnLetras()), FONT_NORMAL);
            doc.add(Chunk.NEWLINE);
            doc.add(letras);

            // --- Medios de pago ---
            for (MedioPagoDTO medio : comprobante.getMediosPago()) {
                Paragraph linea = new Paragraph("Medio de pago: " + valor(medio.getDescripcion())
                        + " (" + medio.getCodigo() + ") - " + importe(medio.getMonto())
                        + (medio.getReferencia() == null || medio.getReferencia().isBlank()
                        ? "" : " - Ref. " + medio.getReferencia()), FONT_NORMAL);
                doc.add(linea);
            }

            // --- Leyendas ---
            for (LeyendaDTO leyenda : comprobante.getLeyendas()) {
                if (leyenda.getCodigo() == null || leyenda.getCodigo().isBlank()) {
                    Paragraph linea = new Paragraph(leyenda.getTexto(), FONT_SMALL);
                    linea.setAlignment(Element.ALIGN_CENTER);
                    doc.add(linea);
                } else {
                    Paragraph linea = new Paragraph(leyenda.getTexto(), FONT_SMALL);
                    doc.add(linea);
                }
            }

            doc.add(Chunk.NEWLINE);

            // --- Codigo QR y Valor Resumen (parte inferior) ---
            if (comprobante.getQrPngBase64() != null && !comprobante.getQrPngBase64().isBlank()) {
                PdfPTable bloqueQr = new PdfPTable(2);
                bloqueQr.setWidthPercentage(100);
                bloqueQr.setWidths(new float[]{40, 60});
                bloqueQr.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                PdfPCell celdaQr = new PdfPCell();
                celdaQr.setBorder(Rectangle.NO_BORDER);
                celdaQr.setHorizontalAlignment(Element.ALIGN_CENTER);
                // Zona de silencio minima de 1 mm alrededor del codigo.
                celdaQr.setPadding(3);
                Image qr = leerImagen(comprobante.getQrPngBase64());
                qr.scaleAbsolute(LADO_QR_PT, LADO_QR_PT);
                celdaQr.addElement(qr);
                bloqueQr.addCell(celdaQr);

                PdfPCell celdaResumen = new PdfPCell();
                celdaResumen.setBorder(Rectangle.NO_BORDER);
                celdaResumen.addElement(new Paragraph("Valor Resumen (cÃ³digo hash):", FONT_BOLD_SMALL));
                celdaResumen.addElement(new Paragraph(valor(comprobante.getHashComprobante()), FONT_SMALL));
                if (comprobante.getValorResumen() != null && !comprobante.getValorResumen().isBlank()) {
                    celdaResumen.addElement(new Paragraph("Valor Resumen (base 64):", FONT_BOLD_SMALL));
                    celdaResumen.addElement(new Paragraph(comprobante.getValorResumen(), FONT_SMALL));
                }
                bloqueQr.addCell(celdaResumen);

                doc.add(bloqueQr);
            }
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        tabla.addCell(PdfUtil.celdaSinBorde(etiqueta, fuente));
        tabla.addCell(PdfUtil.celdaSinBorde(valor, fuente));
    }

    private static Image leerImagen(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        // La imagen se crea desde los bytes del PNG: iText 5 genera un PDF con
        // operadores de estado desbalanceados cuando el Image se construye
        // desde un BufferedImage y se incrusta dentro de una tabla.
        try {
            return Image.getInstance(bytes);
        } catch (BadElementException e) {
            throw new IOException("No se pudo incrustar el codigo QR en la representacion impresa", e);
        }
    }

    private static String importe(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String cantidad(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).stripTrailingZeros().toPlainString();
    }

    private static boolean esMayorCero(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
