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
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Ticket de venta para impresora termica de 80 mm.
 *
 * <p>El PDF tiene exactamente el ancho del papel (80 mm = 226,77 pt) y una
 * altura que se ajusta al contenido: primero se mide el bloque completo con
 * una {@link ColumnText} en modo simulacion y despues se genera la pagina con
 * esa altura, de modo que la ticketera no desperdicia papel ni corta el
 * comprobante a la mitad. Si el contenido supera el maximo de una hoja PDF se
 * reparte en varias paginas.</p>
 *
 * <p>La estructura sigue la representacion impresa que exige la RSM
 * 300-2019/SUNAT, en formato compacto: emisor, comprobante, adquirente,
 * detalle, totales, importe en letras, medio de pago, QR, codigo hash y
 * leyendas.</p>
 */
public final class ComprobanteTicketPdfGenerator {

    /** Ancho del papel: 80 mm. */
    private static final float ANCHO = 226.77f;
    /** Margen lateral de ~4 mm: deja la zona imprimible de las ticketeras. */
    private static final float MARGEN_LATERAL = 11.5f;
    private static final float MARGEN_SUPERIOR = 14f;
    private static final float MARGEN_INFERIOR = 18f;

    /** Altura usada para medir el contenido antes de crear la pagina. */
    private static final float ALTO_MEDICION = 10000f;
    /** Limite de altura de una hoja PDF (200 pulgadas). */
    private static final float ALTO_MAXIMO = 14000f;
    private static final float ALTO_MINIMO = 260f;
    private static final int MAXIMO_PAGINAS = 20;

    /** Lado del codigo QR impreso: 3,4 cm (el maximo normativo es 6 cm). */
    private static final float LADO_QR = 96f;

    private static final BaseColor TINTA = new BaseColor(0x11, 0x18, 0x27);
    private static final BaseColor GRIS = new BaseColor(0x4B, 0x55, 0x63);
    private static final BaseColor GRIS_CLARO = new BaseColor(0x9C, 0xA3, 0xAF);
    private static final BaseColor LINEA = new BaseColor(0xD1, 0xD5, 0xDB);

    private static final BaseFont FUENTE_NORMAL = cargarFuente("/fonts/DejaVuSans.ttf");
    private static final BaseFont FUENTE_NEGRITA = cargarFuente("/fonts/DejaVuSans-Bold.ttf");

    private ComprobanteTicketPdfGenerator() {
    }

    public static byte[] generarPdfBytes(ComprobanteRespuestaDTO comprobante) throws Exception {
        return generarPdfBytes(comprobante, MarcaComprobante.porDefecto());
    }

    public static byte[] generarPdfBytes(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws Exception {
        float altura = medir(comprobante, marca);
        float altoPagina = Math.min(ALTO_MAXIMO,
                Math.max(ALTO_MINIMO, altura + MARGEN_SUPERIOR + MARGEN_INFERIOR + 10f));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(new Rectangle(ANCHO, altoPagina),
                MARGEN_LATERAL, MARGEN_LATERAL, MARGEN_SUPERIOR, MARGEN_INFERIOR);
        PdfWriter escritor = PdfWriter.getInstance(doc, out);
        doc.open();

        ColumnText columna = new ColumnText(escritor.getDirectContent());
        for (Element elemento : contenido(comprobante, marca)) {
            columna.addElement(elemento);
        }

        int estado = ColumnText.NO_MORE_COLUMN;
        int pagina = 0;
        while (estado == ColumnText.NO_MORE_COLUMN && pagina < MAXIMO_PAGINAS) {
            if (pagina > 0) {
                doc.newPage();
            }
            columna.setSimpleColumn(MARGEN_LATERAL, MARGEN_INFERIOR,
                    ANCHO - MARGEN_LATERAL, altoPagina - MARGEN_SUPERIOR);
            estado = columna.go();
            pagina++;
        }
        doc.close();
        return out.toByteArray();
    }

    /**
     * Mide el contenido con una columna de altura practicamente ilimitada: la
     * diferencia entre el borde superior y la ultima linea escrita es la altura
     * real que necesita la pagina. El documento de medicion no se cierra porque
     * su salida se descarta (no tiene paginas que escribir).
     */
    private static float medir(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) throws Exception {
        Document doc = new Document(new Rectangle(ANCHO, ALTO_MEDICION), 0, 0, 0, 0);
        PdfWriter escritor = PdfWriter.getInstance(doc, new ByteArrayOutputStream());
        doc.open();

        ColumnText columna = new ColumnText(escritor.getDirectContent());
        columna.setSimpleColumn(MARGEN_LATERAL, 0, ANCHO - MARGEN_LATERAL, ALTO_MEDICION);
        for (Element elemento : contenido(comprobante, marca)) {
            columna.addElement(elemento);
        }
        columna.go(true);
        return Math.max(0f, ALTO_MEDICION - columna.getYLine());
    }

    // ------------------------------------------------------------------
    // Contenido del ticket
    // ------------------------------------------------------------------

    private static List<Element> contenido(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws Exception {
        List<Element> contenido = new ArrayList<>();
        contenido.add(cabeceraEmisor(comprobante, marca));
        contenido.add(tituloComprobante(comprobante, marca));
        contenido.add(separador());
        contenido.add(bloqueAdquirente(comprobante));
        contenido.add(separador());
        contenido.add(tablaItems(comprobante));
        contenido.add(separador());
        contenido.add(bloqueTotales(comprobante, marca));
        contenido.add(bloquePagoYLetras(comprobante));
        contenido.add(separador());
        contenido.add(bloqueQr(comprobante));
        contenido.add(bloqueLegal(comprobante, marca));
        contenido.add(despedida(marca));
        return contenido;
    }

    private static PdfPTable cabeceraEmisor(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) {
        EmisorDTO emisor = comprobante.getEmisor();

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);

        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setBackgroundColor(marca.primarioSuave());
        celda.setPaddingTop(9);
        celda.setPaddingBottom(8);
        celda.setPaddingLeft(6);
        celda.setPaddingRight(6);

        Image logo = LogoComprobante.cargar(marca.logoUrl());
        if (logo != null) {
            escalarLogo(logo, 30f);
            logo.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(logo);
        }

        Paragraph titulo = centrado(texto(emisor.getNombreComercial(), emisor.getRazonSocial()),
                fuente(12.5f, true, marca.secundario()), logo == null ? 0 : 4);
        celda.addElement(titulo);

        if (!vacio(emisor.getNombreComercial())
                && !emisor.getNombreComercial().equalsIgnoreCase(emisor.getRazonSocial())) {
            Paragraph razon = centrado(emisor.getRazonSocial(), fuente(6.4f, false, GRIS), 1);
            celda.addElement(razon);
        }

        celda.addElement(centrado("R.U.C. " + nulo(emisor.getRuc()),
                fuente(7.8f, true, marca.primario()), 4));

        String domicilio = domicilioEmisor(emisor);
        if (!domicilio.isBlank()) {
            celda.addElement(centrado(domicilio, fuente(6.3f, false, GRIS), 2));
        }

        String contacto = contactoEmisor(emisor);
        if (!contacto.isBlank()) {
            celda.addElement(centrado(contacto, fuente(6.3f, false, GRIS), 1));
        }

        tabla.addCell(celda);
        return tabla;
    }

    private static PdfPTable tituloComprobante(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(7);

        PdfPCell banda = new PdfPCell(centrado(tipoComprobante(comprobante) + " ELECTRÓNICA",
                fuente(7.4f, true, marca.textoPrimario()), 0));
        banda.setBorder(Rectangle.NO_BORDER);
        banda.setBackgroundColor(marca.primario());
        banda.setPaddingTop(4);
        banda.setPaddingBottom(4);
        tabla.addCell(banda);

        PdfPTable detalle = new PdfPTable(1);
        detalle.setWidthPercentage(100);

        PdfPCell numero = new PdfPCell();
        numero.setBorder(Rectangle.NO_BORDER);
        numero.setPaddingTop(6);
        numero.setPaddingBottom(0);
        numero.addElement(centrado(nulo(comprobante.getNumeroDocumento()), fuente(13.5f, true, TINTA), 0));
        numero.addElement(centrado("Emitido: " + fecha(comprobante), fuente(6.6f, false, GRIS), 2));
        detalle.addCell(numero);

        PdfPCell celda = new PdfPCell(detalle);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        tabla.addCell(celda);
        return tabla;
    }

    private static PdfPTable separador() {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);

        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(LINEA);
        celda.setBorderWidth(0.7f);
        celda.setPadding(0);
        celda.setMinimumHeight(1);
        tabla.addCell(celda);
        return tabla;
    }

    private static Paragraph bloqueAdquirente(ComprobanteRespuestaDTO comprobante) {
        ReceptorDTO receptor = comprobante.getReceptor();
        DomicilioDTO domicilio = receptor == null ? null : receptor.getDomicilioFiscal();

        Paragraph bloque = new Paragraph();
        bloque.setLeading(9);
        bloque.add(etiqueta("CLIENTE"));
        bloque.add(Chunk.NEWLINE);
        bloque.add(new Chunk(texto(receptor == null ? null : receptor.getRazonSocial(), "Clientes varios"),
                fuente(8.4f, true, TINTA)));
        bloque.add(Chunk.NEWLINE);
        bloque.add(new Chunk(etiquetaDocumento(receptor) + ": "
                + texto(receptor == null ? null : receptor.getNumeroDocumento(), "—"),
                fuente(7.1f, false, GRIS)));

        String direccion = domicilio == null ? "" : direccion(domicilio);
        if (!direccion.isBlank()) {
            bloque.add(Chunk.NEWLINE);
            bloque.add(new Chunk("Dirección: " + direccion, fuente(6.6f, false, GRIS)));
        }

        if (receptor != null && !vacio(receptor.getCorreo())) {
            bloque.add(Chunk.NEWLINE);
            bloque.add(new Chunk("Correo: " + receptor.getCorreo(), fuente(6.6f, false, GRIS)));
        }

        DomicilioDTO entrega = comprobante.getEntrega();
        if (entrega != null && !vacio(entrega.getDireccion())
                && (domicilio == null || !entrega.getDireccion().equalsIgnoreCase(domicilio.getDireccion()))) {
            bloque.add(Chunk.NEWLINE);
            bloque.add(new Chunk("Entrega: " + direccion(entrega), fuente(6.6f, false, GRIS)));
        }
        return bloque;
    }

    private static PdfPTable tablaItems(ComprobanteRespuestaDTO comprobante) {
        PdfPTable tabla = new PdfPTable(new float[]{0.8f, 3.9f, 1.35f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);

        cabeceraItem(tabla, "CANT", Element.ALIGN_CENTER);
        cabeceraItem(tabla, "DESCRIPCIÓN", Element.ALIGN_LEFT);
        cabeceraItem(tabla, "IMPORTE", Element.ALIGN_RIGHT);

        String moneda = comprobante.getMoneda();
        for (DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
            PdfPCell cantidad = celdaItem(cantidad(detalle.getCantidad()), fuente(7f, false, TINTA),
                    Element.ALIGN_CENTER);

            Paragraph descripcion = new Paragraph();
            descripcion.setLeading(9);
            descripcion.add(new Chunk(texto(detalle.getDescripcion(), "Producto"),
                    fuente(7.2f, false, TINTA)));
            String afectacion = afectacion(detalle.getAfectacionIgv());
            if (!afectacion.isBlank()) {
                descripcion.add(new Chunk("  (" + afectacion + ")", fuente(6.1f, false, GRIS_CLARO)));
            }
            descripcion.add(Chunk.NEWLINE);
            descripcion.add(new Chunk(cantidad(detalle.getCantidad()) + " × " + simbolo(moneda)
                    + " " + importe(detalle.getPrecioUnitario()), fuente(6.1f, false, GRIS)));

            PdfPCell descripcionCelda = new PdfPCell(descripcion);
            descripcionCelda.setBorder(Rectangle.BOTTOM);
            descripcionCelda.setBorderColor(LINEA);
            descripcionCelda.setBorderWidth(0.5f);
            descripcionCelda.setPaddingTop(3);
            descripcionCelda.setPaddingBottom(3);
            descripcionCelda.setPaddingLeft(2);
            descripcionCelda.setPaddingRight(2);
            descripcionCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell importe = celdaItem(importe(detalle.getTotalItem()), fuente(7.3f, true, TINTA),
                    Element.ALIGN_RIGHT);

            tabla.addCell(cantidad);
            tabla.addCell(descripcionCelda);
            tabla.addCell(importe);
        }

        if (comprobante.getDetalles().isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin detalle de productos", fuente(7f, false, GRIS)));
            vacio.setColspan(3);
            vacio.setBorder(Rectangle.BOTTOM);
            vacio.setBorderColor(LINEA);
            vacio.setPadding(4);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacio);
        }
        return tabla;
    }

    private static void cabeceraItem(PdfPTable tabla, String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente(6.3f, true, TINTA)));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(TINTA);
        celda.setBorderWidth(0.8f);
        celda.setPaddingTop(3);
        celda.setPaddingBottom(2);
        celda.setPaddingLeft(2);
        celda.setPaddingRight(2);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private static PdfPCell celdaItem(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(LINEA);
        celda.setBorderWidth(0.5f);
        celda.setPaddingTop(3);
        celda.setPaddingBottom(3);
        celda.setPaddingLeft(2);
        celda.setPaddingRight(2);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private static PdfPTable bloqueTotales(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) {
        TotalesDTO totales = comprobante.getTotales();
        String moneda = comprobante.getMoneda();

        PdfPTable tabla = new PdfPTable(new float[]{62f, 38f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(2);

        if (esMayorCero(totales.getOpGravada())) {
            filaTotal(tabla, "Op. Gravada", totales.getOpGravada(), moneda);
        }
        if (esMayorCero(totales.getOpInafecta())) {
            filaTotal(tabla, "Op. Inafecta", totales.getOpInafecta(), moneda);
        }
        if (esMayorCero(totales.getOpExonerada())) {
            filaTotal(tabla, "Op. Exonerada", totales.getOpExonerada(), moneda);
        }
        if (esMayorCero(totales.getOpGratuita())) {
            filaTotal(tabla, "Op. Gratuitas", totales.getOpGratuita(), moneda);
        }
        if (esMayorCero(totales.getTotalDescuentos())) {
            filaTotal(tabla, "Descuentos", totales.getTotalDescuentos().negate(), moneda);
        }
        if (esMayorCero(totales.getIgv())) {
            filaTotal(tabla, "I.G.V. (18%)", totales.getIgv(), moneda);
        }

        PdfPCell etiqueta = new PdfPCell(new Phrase("TOTAL A PAGAR", fuente(9.5f, true, marca.secundario())));
        etiqueta.setBorder(Rectangle.TOP);
        etiqueta.setBorderColor(TINTA);
        etiqueta.setBorderWidth(1.1f);
        etiqueta.setPaddingTop(5);
        etiqueta.setPaddingBottom(3);
        etiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(etiqueta);

        PdfPCell valor = new PdfPCell(new Phrase(simbolo(moneda) + " " + importe(totales.getMontoTotalPagar()),
                fuente(11.5f, true, marca.secundario())));
        valor.setBorder(Rectangle.TOP);
        valor.setBorderColor(TINTA);
        valor.setBorderWidth(1.1f);
        valor.setPaddingTop(5);
        valor.setPaddingBottom(3);
        valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(valor);
        return tabla;
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, BigDecimal valor, String moneda) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuente(7f, false, GRIS)));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPaddingTop(2);
        celdaEtiqueta.setPaddingBottom(2);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(simbolo(moneda) + " " + importe(valor),
                fuente(7f, false, TINTA)));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPaddingTop(2);
        celdaValor.setPaddingBottom(2);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(celdaValor);
    }

    private static Paragraph bloquePagoYLetras(ComprobanteRespuestaDTO comprobante) {
        Paragraph bloque = new Paragraph();
        bloque.setLeading(9);
        bloque.setSpacingBefore(6);

        bloque.add(new Chunk("SON: ", fuente(6.5f, true, TINTA)));
        bloque.add(new Chunk(texto(comprobante.getTotales().getTotalEnLetras(), "—"),
                fuente(6.5f, false, GRIS)));

        for (MedioPagoDTO medio : comprobante.getMediosPago()) {
            bloque.add(Chunk.NEWLINE);
            String referencia = vacio(medio.getReferencia()) ? "" : " · Ref. " + medio.getReferencia();
            bloque.add(new Chunk("PAGO: " + texto(medio.getDescripcion(), "Efectivo") + " · "
                    + simbolo(comprobante.getMoneda()) + " " + importe(medio.getMonto()) + referencia,
                    fuente(6.5f, false, GRIS)));
        }

        if (!vacio(comprobante.getObservacion())) {
            bloque.add(Chunk.NEWLINE);
            bloque.add(new Chunk("Observación: " + comprobante.getObservacion(),
                    fuente(6.5f, false, GRIS)));
        }
        return bloque;
    }

    private static PdfPTable bloqueQr(ComprobanteRespuestaDTO comprobante) throws IOException {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(2);

        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);

        if (!vacio(comprobante.getQrPngBase64())) {
            // Imagen y leyenda en una tabla anidada para que el pie quede
            // pegado al QR y la altura se calcule de forma exacta.
            PdfPTable qrBloque = new PdfPTable(1);
            qrBloque.setWidthPercentage(100);

            PdfPCell imagen = new PdfPCell();
            imagen.setBorder(Rectangle.NO_BORDER);
            imagen.setPadding(0);
            imagen.setHorizontalAlignment(Element.ALIGN_CENTER);
            Image qr = leerImagen(comprobante.getQrPngBase64());
            qr.scaleAbsolute(LADO_QR, LADO_QR);
            qr.setAlignment(Element.ALIGN_CENTER);
            imagen.addElement(qr);
            qrBloque.addCell(imagen);

            PdfPCell pie = new PdfPCell(centrado("Escanea para verificar tu comprobante en SUNAT",
                    fuente(5.9f, false, GRIS), 0));
            pie.setBorder(Rectangle.NO_BORDER);
            pie.setPadding(0);
            pie.setPaddingTop(3);
            pie.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrBloque.addCell(pie);

            celda.addElement(qrBloque);
        } else {
            celda.addElement(centrado("QR no disponible para este comprobante",
                    fuente(6.5f, false, GRIS), 0));
        }

        if (!vacio(comprobante.getHashComprobante())) {
            Paragraph hash = new Paragraph();
            hash.setLeading(7.5f);
            hash.setSpacingBefore(4);
            hash.add(new Chunk("CÓDIGO HASH: ", fuente(5.6f, true, GRIS)));
            hash.add(new Chunk(partir(comprobante.getHashComprobante(), 38), fuente(5.4f, false, GRIS_CLARO)));
            celda.addElement(hash);
        }

        if (!vacio(comprobante.getValorResumen())) {
            Paragraph resumen = new Paragraph();
            resumen.setLeading(7.5f);
            resumen.add(new Chunk("VALOR RESUMEN: ", fuente(5.6f, true, GRIS)));
            resumen.add(new Chunk(partir(comprobante.getValorResumen(), 38), fuente(5.4f, false, GRIS_CLARO)));
            celda.addElement(resumen);
        }

        tabla.addCell(celda);
        return tabla;
    }

    private static Paragraph bloqueLegal(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) {
        Paragraph legal = new Paragraph();
        legal.setLeading(8);
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(5);

        List<LeyendaDTO> leyendas = comprobante.getLeyendas();
        if (leyendas != null) {
            for (LeyendaDTO leyenda : leyendas) {
                if (leyenda == null || vacio(leyenda.getTexto()) || "1000".equals(leyenda.getCodigo())) {
                    continue;
                }
                legal.add(new Chunk(leyenda.getTexto(), fuente(5.7f, false, GRIS)));
                legal.add(Chunk.NEWLINE);
            }
        }

        legal.add(new Chunk("Comprobante emitido electrónicamente. Verifique su autenticidad "
                + "escaneando el código QR.", fuente(5.5f, false, GRIS_CLARO)));
        return legal;
    }

    private static Paragraph despedida(MarcaComprobante marca) {
        Paragraph parrafo = new Paragraph("¡Gracias por su compra!", fuente(9f, true, marca.primario()));
        parrafo.setAlignment(Element.ALIGN_CENTER);
        parrafo.setSpacingBefore(7);
        return parrafo;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Paragraph centrado(String texto, Font fuente, float espacioAntes) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        parrafo.setLeading(fuente.getSize() + 3f);
        parrafo.setSpacingBefore(espacioAntes);
        return parrafo;
    }

    private static Chunk etiqueta(String texto) {
        return new Chunk(texto, fuente(6f, true, GRIS_CLARO));
    }

    private static BaseFont cargarFuente(String ruta) {
        try (InputStream entrada = ComprobanteTicketPdfGenerator.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontro la fuente " + ruta + " en el classpath");
            }
            return BaseFont.createFont(ruta, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, entrada.readAllBytes(), null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la fuente Unicode " + ruta, e);
        }
    }

    private static Font fuente(float tamano, boolean negrita, BaseColor color) {
        return new Font(negrita ? FUENTE_NEGRITA : FUENTE_NORMAL, tamano, Font.NORMAL, color);
    }

    /** Ajusta el logo al ancho util del ticket conservando su proporcion. */
    private static void escalarLogo(Image logo, float altoMaximo) {
        float anchoOriginal = logo.getWidth();
        float altoOriginal = logo.getHeight();
        float escala = Math.min(altoMaximo / altoOriginal, 120f / anchoOriginal);
        logo.scaleAbsolute(anchoOriginal * escala, altoOriginal * escala);
    }

    private static Image leerImagen(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        try {
            return Image.getInstance(bytes);
        } catch (Exception e) {
            throw new IOException("No se pudo incrustar el codigo QR en el ticket", e);
        }
    }

    private static String importe(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String cantidad(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).stripTrailingZeros().toPlainString();
    }

    private static boolean esMayorCero(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String simbolo(String moneda) {
        if ("USD".equalsIgnoreCase(moneda)) {
            return "US$";
        }
        return "PEN".equalsIgnoreCase(moneda) || vacio(moneda) ? "S/" : moneda;
    }

    private static String tipoComprobante(ComprobanteRespuestaDTO comprobante) {
        String descripcion = comprobante.getTipoComprobanteDescripcion();
        if (!vacio(descripcion)) {
            return descripcion.toUpperCase();
        }
        return "01".equals(comprobante.getTipoComprobante()) ? "FACTURA" : "BOLETA DE VENTA";
    }

    private static String fecha(ComprobanteRespuestaDTO comprobante) {
        String fecha = texto(comprobante.getFechaEmision(), "");
        if (fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
            fecha = fecha.substring(8, 10) + "/" + fecha.substring(5, 7) + "/" + fecha.substring(0, 4);
        }
        String hora = texto(comprobante.getHoraEmision(), "");
        return hora.isBlank() ? fecha : fecha + " " + hora;
    }

    private static String domicilioEmisor(EmisorDTO emisor) {
        StringBuilder texto = new StringBuilder(texto(emisor.getDomicilioFiscal(), ""));
        if (!vacio(emisor.getDistrito())) {
            texto.append(texto.isEmpty() ? "" : " - ").append(emisor.getDistrito());
        }
        if (!vacio(emisor.getProvincia())) {
            texto.append(", ").append(emisor.getProvincia());
        }
        return texto.toString();
    }

    private static String contactoEmisor(EmisorDTO emisor) {
        StringBuilder texto = new StringBuilder();
        if (!vacio(emisor.getTelefono())) {
            texto.append("Tel. ").append(emisor.getTelefono());
        }
        if (!vacio(emisor.getEmail())) {
            texto.append(texto.isEmpty() ? "" : " · ").append(emisor.getEmail());
        }
        return texto.toString();
    }

    private static String direccion(DomicilioDTO domicilio) {
        StringBuilder texto = new StringBuilder(texto(domicilio.getDireccion(), ""));
        if (!vacio(domicilio.getDistrito())) {
            texto.append(texto.isEmpty() ? "" : " - ").append(domicilio.getDistrito());
        }
        if (!vacio(domicilio.getProvincia())) {
            texto.append(", ").append(domicilio.getProvincia());
        }
        return texto.toString();
    }

    private static String etiquetaDocumento(ReceptorDTO receptor) {
        String tipo = receptor == null ? null : receptor.getTipoDocumento();
        if ("6".equals(tipo)) {
            return "RUC";
        }
        if ("1".equals(tipo)) {
            return "DNI";
        }
        if ("4".equals(tipo)) {
            return "Carné de extranjería";
        }
        if ("7".equals(tipo)) {
            return "Pasaporte";
        }
        return "Documento";
    }

    private static String afectacion(String codigo) {
        if ("02".equals(codigo)) {
            return "Inafecta";
        }
        if ("03".equals(codigo)) {
            return "Exonerada";
        }
        if ("04".equals(codigo)) {
            return "Gratuita";
        }
        return "";
    }

    /** Corta una cadena larga (hash) para que pueda envolverse en el ticket. */
    private static String partir(String valor, int tamano) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.trim();
        if (limpio.length() <= tamano) {
            return limpio;
        }
        return limpio.replaceAll("(.{" + tamano + "})", "$1 ");
    }

    private static String texto(String valor, String defecto) {
        return vacio(valor) ? defecto : valor;
    }

    private static String nulo(String valor) {
        return vacio(valor) ? "—" : valor;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
