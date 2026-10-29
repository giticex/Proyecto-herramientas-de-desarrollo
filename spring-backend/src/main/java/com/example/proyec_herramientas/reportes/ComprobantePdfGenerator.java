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
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;

/**
 * Representacion impresa A4 del comprobante de pago electronico.
 *
 * <p>La RSM 300-2019/SUNAT (Anexo I, numeral 6) exige que el codigo QR se
 * imprima en la parte inferior de la representacion, con un tamano maximo de
 * 6 cm por 6 cm (incluida la zona de silencio de 1 mm) y en color negro. El
 * Valor Resumen puede consignarse fuera del QR.</p>
 */
public class ComprobantePdfGenerator {

    /**
     * DejaVu Sans se incrusta con IDENTITY_H para que el PDF conserve tildes,
     * la 'n' con virgulilla y cualquier caracter UTF-8 sin depender de las
     * fuentes del sistema.
     */
    private static final BaseFont FUENTE_NORMAL = cargarFuente("/fonts/DejaVuSans.ttf");
    private static final BaseFont FUENTE_NEGRITA = cargarFuente("/fonts/DejaVuSans-Bold.ttf");

    private static final BaseColor GRIS_TEXTO = new BaseColor(0x5B, 0x67, 0x75);
    private static final BaseColor GRIS_LINEA = new BaseColor(0xE2, 0xE8, 0xF0);
    private static final BaseColor GRIS_SUAVE = new BaseColor(0x94, 0xA3, 0xB8);
    private static final BaseColor FONDO_SUAVE = new BaseColor(0xF6, 0xF8, 0xFB);
    private static final BaseColor FONDO_ZEBRA = new BaseColor(0xF9, 0xFB, 0xFD);
    private static final BaseColor TINTA = new BaseColor(0x0F, 0x17, 0x2A);

    /** Lado del codigo QR impreso: 3,6 cm (el maximo normativo es 6 cm). */
    private static final float LADO_QR_PT = 102f;

    public static byte[] generarPdfBytes(ComprobanteRespuestaDTO comprobante) throws Exception {
        return generarPdfBytes(comprobante, MarcaComprobante.porDefecto());
    }

    public static byte[] generarPdfBytes(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 56);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new PieDePagina(comprobante));

        doc.open();
        try {
            doc.add(cabecera(comprobante, marca));
            doc.add(separador(marca));
            doc.add(bloqueAdquirente(comprobante, marca));
            doc.add(tablaItems(comprobante, marca));
            doc.add(bloqueTotales(comprobante, marca));
            doc.add(bloquePagoYLetras(comprobante, marca));
            doc.add(bloqueQr(comprobante, marca));
            doc.add(despedida(marca));
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------
    // Cabecera: marca de la tienda + caja del comprobante
    // ------------------------------------------------------------------

    private static PdfPTable cabecera(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws DocumentException {
        EmisorDTO emisor = comprobante.getEmisor();
        PdfPTable cabecera = new PdfPTable(new float[]{62f, 38f});
        cabecera.setWidthPercentage(100);

        PdfPCell marcaCelda = new PdfPCell();
        marcaCelda.setBorder(Rectangle.NO_BORDER);
        marcaCelda.setBackgroundColor(marca.primario());
        marcaCelda.setPadding(18);
        marcaCelda.setMinimumHeight(104);

        Image logo = LogoComprobante.cargar(marca.logoUrl());
        if (logo != null) {
            escalarLogo(logo, 34f);
            logo.setAlignment(Element.ALIGN_LEFT);
            marcaCelda.addElement(logo);
        }

        String nombrePrincipal = texto(emisor.getNombreComercial(), emisor.getRazonSocial());
        Paragraph titulo = new Paragraph(nombrePrincipal, fuente(17, true, marca.textoPrimario()));
        titulo.setLeading(20);
        if (logo != null) {
            titulo.setSpacingBefore(8);
        }
        marcaCelda.addElement(titulo);

        if (!vacio(emisor.getNombreComercial()) && !emisor.getNombreComercial().equalsIgnoreCase(emisor.getRazonSocial())) {
            Paragraph razon = new Paragraph(emisor.getRazonSocial(), fuente(8.5f, false, marca.textoPrimario()));
            razon.setSpacingBefore(2);
            marcaCelda.addElement(razon);
        }

        Paragraph domicilio = new Paragraph(domicilioEmisor(emisor), fuente(7.8f, false, marca.textoPrimario()));
        domicilio.setSpacingBefore(8);
        marcaCelda.addElement(domicilio);

        String contacto = contactoEmisor(emisor);
        if (!contacto.isBlank()) {
            Paragraph linea = new Paragraph(contacto, fuente(7.8f, false, marca.textoPrimario()));
            linea.setSpacingBefore(2);
            marcaCelda.addElement(linea);
        }

        Paragraph electronico = new Paragraph("COMPROBANTE DE PAGO ELECTRÓNICO",
                fuente(6.6f, true, marca.textoPrimario()));
        electronico.setSpacingBefore(8);
        marcaCelda.addElement(electronico);

        PdfPCell documento = new PdfPCell();
        documento.setBorder(Rectangle.BOX);
        documento.setBorderColor(GRIS_LINEA);
        documento.setBorderWidth(0.8f);
        documento.setBackgroundColor(BaseColor.WHITE);
        documento.setPadding(14);
        documento.setMinimumHeight(104);

        Paragraph ruc = new Paragraph("R.U.C. " + nulo(emisor.getRuc()), fuente(10, true, TINTA));
        ruc.setAlignment(Element.ALIGN_RIGHT);
        documento.addElement(ruc);

        Chunk tipo = new Chunk("  " + tipoComprobante(comprobante) + " ELECTRÓNICA  ",
                fuente(8.6f, true, marca.textoPrimario()));
        tipo.setBackground(marca.primario());
        Paragraph tipoParrafo = new Paragraph(tipo);
        tipoParrafo.setAlignment(Element.ALIGN_RIGHT);
        tipoParrafo.setSpacingBefore(8);
        documento.addElement(tipoParrafo);

        Paragraph numero = new Paragraph(nulo(comprobante.getNumeroDocumento()),
                fuente(14.5f, true, marca.secundario()));
        numero.setAlignment(Element.ALIGN_RIGHT);
        numero.setSpacingBefore(8);
        documento.addElement(numero);

        Paragraph fecha = new Paragraph("Emitido: " + fecha(comprobante), fuente(7.8f, false, GRIS_TEXTO));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingBefore(3);
        documento.addElement(fecha);

        Paragraph moneda = new Paragraph("Moneda: " + nombreMoneda(comprobante.getMoneda()),
                fuente(7.8f, false, GRIS_TEXTO));
        moneda.setAlignment(Element.ALIGN_RIGHT);
        moneda.setSpacingBefore(1);
        documento.addElement(moneda);

        cabecera.addCell(marcaCelda);
        cabecera.addCell(documento);
        return cabecera;
    }

    private static PdfPTable separador(MarcaComprobante marca) {
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(marca.primario());
        celda.setBorderWidth(2f);
        celda.setPadding(0);
        celda.setMinimumHeight(2);
        linea.addCell(celda);
        linea.setSpacingBefore(10);
        return linea;
    }

    // ------------------------------------------------------------------
    // Adquirente
    // ------------------------------------------------------------------

    private static PdfPTable bloqueAdquirente(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws DocumentException {
        ReceptorDTO receptor = comprobante.getReceptor();
        DomicilioDTO domicilio = receptor == null ? null : receptor.getDomicilioFiscal();

        PdfPTable contenido = new PdfPTable(new float[]{64f, 36f});
        contenido.setWidthPercentage(100);

        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        datos.setPadding(0);
        datos.setPaddingRight(12);
        datos.addElement(new Paragraph("ADQUIRENTE", fuente(7, true, GRIS_SUAVE)));

        Paragraph nombre = new Paragraph(
                texto(receptor == null ? null : receptor.getRazonSocial(), "Clientes varios"),
                fuente(10.5f, true, marca.secundario()));
        nombre.setSpacingBefore(3);
        datos.addElement(nombre);

        Paragraph documento = new Paragraph(
                etiquetaDocumento(receptor) + ": " + texto(receptor == null ? null : receptor.getNumeroDocumento(), "—"),
                fuente(8.4f, false, TINTA));
        documento.setSpacingBefore(2);
        datos.addElement(documento);

        String direccion = domicilio == null ? "" : direccion(domicilio);
        if (!direccion.isBlank()) {
            Paragraph linea = new Paragraph("Dirección: " + direccion, fuente(8, false, GRIS_TEXTO));
            linea.setSpacingBefore(2);
            datos.addElement(linea);
        }

        if (receptor != null && !vacio(receptor.getCorreo())) {
            Paragraph correo = new Paragraph("Correo: " + receptor.getCorreo(), fuente(8, false, GRIS_TEXTO));
            correo.setSpacingBefore(2);
            datos.addElement(correo);
        }

        DomicilioDTO entrega = comprobante.getEntrega();
        if (entrega != null && !vacio(entrega.getDireccion())
                && (domicilio == null || !entrega.getDireccion().equalsIgnoreCase(domicilio.getDireccion()))) {
            Paragraph linea = new Paragraph("Entrega: " + direccion(entrega), fuente(8, false, GRIS_TEXTO));
            linea.setSpacingBefore(2);
            datos.addElement(linea);
        }

        PdfPCell condiciones = new PdfPCell();
        condiciones.setBorder(Rectangle.NO_BORDER);
        condiciones.setPadding(0);
        condiciones.setHorizontalAlignment(Element.ALIGN_RIGHT);

        condiciones.addElement(derecha("Condición: " + condicion(comprobante.getCondicionOperacion())));
        condiciones.addElement(derecha("Forma de pago: " + formaPago(comprobante.getFormaPago())));
        condiciones.addElement(derecha("Moneda: " + nombreMoneda(comprobante.getMoneda())));

        contenido.addCell(datos);
        contenido.addCell(condiciones);

        PdfPTable caja = new PdfPTable(1);
        caja.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(GRIS_LINEA);
        celda.setBorderWidth(0.8f);
        celda.setBackgroundColor(FONDO_SUAVE);
        celda.setPadding(12);
        caja.addCell(celda);

        PdfPTable exterior = new PdfPTable(1);
        exterior.setWidthPercentage(100);
        exterior.setSpacingBefore(12);
        exterior.addCell(cajaCell(caja));
        return exterior;
    }

    private static PdfPCell cajaCell(PdfPTable tabla) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        celda.addElement(tabla);
        return celda;
    }

    private static Paragraph derecha(String texto) {
        Paragraph parrafo = new Paragraph(texto, fuente(8, false, GRIS_TEXTO));
        parrafo.setAlignment(Element.ALIGN_RIGHT);
        parrafo.setSpacingBefore(2);
        return parrafo;
    }

    // ------------------------------------------------------------------
    // Detalle
    // ------------------------------------------------------------------

    private static PdfPTable tablaItems(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{0.55f, 0.8f, 0.85f, 5.1f, 1.45f, 1.45f, 1.2f, 1.65f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSplitRows(false);
        tabla.setSplitLate(false);
        tabla.setSpacingBefore(12);

        cabeceraItem(tabla, "N°", Element.ALIGN_CENTER, marca);
        cabeceraItem(tabla, "CANT.", Element.ALIGN_CENTER, marca);
        cabeceraItem(tabla, "U.M.", Element.ALIGN_CENTER, marca);
        cabeceraItem(tabla, "DESCRIPCIÓN", Element.ALIGN_LEFT, marca);
        cabeceraItem(tabla, "V. UNIT.", Element.ALIGN_RIGHT, marca);
        cabeceraItem(tabla, "P. UNIT.", Element.ALIGN_RIGHT, marca);
        cabeceraItem(tabla, "IGV", Element.ALIGN_RIGHT, marca);
        cabeceraItem(tabla, "IMPORTE", Element.ALIGN_RIGHT, marca);

        boolean zebra = false;
        for (DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
            BaseColor fondo = zebra ? FONDO_ZEBRA : BaseColor.WHITE;
            zebra = !zebra;

            celdaItem(String.valueOf(detalle.getNumero()), fuente(7.8f, false, TINTA),
                    Element.ALIGN_CENTER, fondo, tabla);
            celdaItem(cantidad(detalle.getCantidad()), fuente(7.8f, false, TINTA),
                    Element.ALIGN_CENTER, fondo, tabla);
            celdaItem(unidad(detalle.getUnidadMedida()), fuente(7.8f, false, GRIS_TEXTO),
                    Element.ALIGN_CENTER, fondo, tabla);

            Phrase descripcion = new Phrase(texto(detalle.getDescripcion(), ""), fuente(7.8f, false, TINTA));
            String afectacion = afectacion(detalle.getAfectacionIgv());
            if (!afectacion.isBlank()) {
                descripcion.add(new Chunk("  · " + afectacion, fuente(6.6f, false, GRIS_TEXTO)));
            }
            PdfPCell celdaDescripcion = new PdfPCell(descripcion);
            celdaDescripcion.setBorder(Rectangle.BOTTOM);
            celdaDescripcion.setBorderColor(GRIS_LINEA);
            celdaDescripcion.setBorderWidth(0.5f);
            celdaDescripcion.setBackgroundColor(fondo);
            celdaDescripcion.setPadding(5);
            celdaDescripcion.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celdaDescripcion);

            celdaItem(importe(detalle.getValorUnitario()), fuente(7.8f, false, TINTA),
                    Element.ALIGN_RIGHT, fondo, tabla);
            celdaItem(importe(detalle.getPrecioUnitario()), fuente(7.8f, false, TINTA),
                    Element.ALIGN_RIGHT, fondo, tabla);
            celdaItem(importe(detalle.getIgv()), fuente(7.8f, false, TINTA),
                    Element.ALIGN_RIGHT, fondo, tabla);
            celdaItem(importe(detalle.getTotalItem()), fuente(7.8f, true, TINTA),
                    Element.ALIGN_RIGHT, fondo, tabla);
        }

        if (comprobante.getDetalles().isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin detalle de productos", fuente(8, false, GRIS_TEXTO)));
            vacio.setColspan(8);
            vacio.setBorder(Rectangle.BOTTOM);
            vacio.setBorderColor(GRIS_LINEA);
            vacio.setPadding(8);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(vacio);
        }
        return tabla;
    }

    private static void cabeceraItem(PdfPTable tabla, String texto, int alineacion, MarcaComprobante marca) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente(7.2f, true, marca.textoPrimario())));
        celda.setBackgroundColor(marca.primario());
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingTop(6);
        celda.setPaddingBottom(6);
        celda.setPaddingLeft(3);
        celda.setPaddingRight(3);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private static void celdaItem(String texto, Font fuente, int alineacion, BaseColor fondo, PdfPTable tabla) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(GRIS_LINEA);
        celda.setBorderWidth(0.5f);
        celda.setBackgroundColor(fondo);
        celda.setPadding(5);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(celda);
    }

    // ------------------------------------------------------------------
    // Totales, pago y letras
    // ------------------------------------------------------------------

    private static PdfPTable bloqueTotales(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws DocumentException {
        TotalesDTO totales = comprobante.getTotales();
        PdfPTable tabla = new PdfPTable(new float[]{60f, 40f});
        tabla.setWidthPercentage(46);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(10);

        if (esMayorCero(totales.getOpGravada())) {
            filaTotal(tabla, "Op. Gravada", totales.getOpGravada(), comprobante.getMoneda(), false);
        }
        if (esMayorCero(totales.getOpInafecta())) {
            filaTotal(tabla, "Op. Inafecta", totales.getOpInafecta(), comprobante.getMoneda(), false);
        }
        if (esMayorCero(totales.getOpExonerada())) {
            filaTotal(tabla, "Op. Exonerada", totales.getOpExonerada(), comprobante.getMoneda(), false);
        }
        if (esMayorCero(totales.getOpGratuita())) {
            filaTotal(tabla, "Op. Gratuitas", totales.getOpGratuita(), comprobante.getMoneda(), false);
        }
        if (esMayorCero(totales.getTotalDescuentos())) {
            filaTotal(tabla, "Descuentos", totales.getTotalDescuentos().negate(), comprobante.getMoneda(), false);
        }
        if (esMayorCero(totales.getIgv())) {
            filaTotal(tabla, "I.G.V. (18%)", totales.getIgv(), comprobante.getMoneda(), false);
        }

        PdfPCell etiqueta = new PdfPCell(new Phrase("TOTAL A PAGAR", fuente(10.5f, true, marca.textoPrimario())));
        etiqueta.setBackgroundColor(marca.primario());
        etiqueta.setBorder(Rectangle.NO_BORDER);
        etiqueta.setPadding(8);
        etiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(etiqueta);

        PdfPCell valor = new PdfPCell(new Phrase(simbolo(comprobante.getMoneda()) + " "
                + importe(totales.getMontoTotalPagar()), fuente(10.5f, true, marca.textoPrimario())));
        valor.setBackgroundColor(marca.primario());
        valor.setBorder(Rectangle.NO_BORDER);
        valor.setPadding(8);
        valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(valor);
        return tabla;
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, BigDecimal valor, String moneda, boolean negrita) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuente(8.2f, false, GRIS_TEXTO)));
        celdaEtiqueta.setBorder(Rectangle.BOTTOM);
        celdaEtiqueta.setBorderColor(GRIS_LINEA);
        celdaEtiqueta.setBorderWidth(0.5f);
        celdaEtiqueta.setPadding(5);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(simbolo(moneda) + " " + importe(valor),
                fuente(8.2f, negrita, TINTA)));
        celdaValor.setBorder(Rectangle.BOTTOM);
        celdaValor.setBorderColor(GRIS_LINEA);
        celdaValor.setBorderWidth(0.5f);
        celdaValor.setPadding(5);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(celdaValor);
    }

    private static Paragraph bloquePagoYLetras(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca) {
        TotalesDTO totales = comprobante.getTotales();
        Paragraph parrafo = new Paragraph();
        parrafo.setSpacingBefore(10);

        Chunk etiqueta = new Chunk("SON: ", fuente(8.4f, true, marca.secundario()));
        parrafo.add(etiqueta);
        parrafo.add(new Chunk(texto(totales.getTotalEnLetras(), "—"), fuente(8.4f, false, TINTA)));

        for (MedioPagoDTO medio : comprobante.getMediosPago()) {
            parrafo.add(Chunk.NEWLINE);
            String referencia = vacio(medio.getReferencia()) ? "" : " · Ref. " + medio.getReferencia();
            parrafo.add(new Chunk("Pago: " + texto(medio.getDescripcion(), "Efectivo")
                    + " · " + simbolo(comprobante.getMoneda()) + " " + importe(medio.getMonto()) + referencia,
                    fuente(8.4f, false, GRIS_TEXTO)));
        }

        if (!vacio(comprobante.getObservacion())) {
            parrafo.add(Chunk.NEWLINE);
            parrafo.add(new Chunk("Observación: " + comprobante.getObservacion(),
                    fuente(8.4f, false, GRIS_TEXTO)));
        }
        return parrafo;
    }

    // ------------------------------------------------------------------
    // QR, valor resumen y leyendas
    // ------------------------------------------------------------------

    private static PdfPTable bloqueQr(ComprobanteRespuestaDTO comprobante, MarcaComprobante marca)
            throws DocumentException, IOException {
        PdfPTable contenido = new PdfPTable(new float[]{24f, 76f});
        contenido.setWidthPercentage(100);

        PdfPCell qrCelda = new PdfPCell();
        qrCelda.setBorder(Rectangle.NO_BORDER);
        qrCelda.setPadding(0);
        qrCelda.setPaddingRight(12);
        qrCelda.setVerticalAlignment(Element.ALIGN_TOP);
        if (!vacio(comprobante.getQrPngBase64())) {
            // Imagen y leyenda en una tabla anidada: asi la altura de la fila
            // incluye el pie del QR y nada sobresale del marco.
            PdfPTable bloque = new PdfPTable(1);
            bloque.setWidthPercentage(100);

            PdfPCell imagen = new PdfPCell();
            imagen.setBorder(Rectangle.NO_BORDER);
            imagen.setPadding(0);
            imagen.setHorizontalAlignment(Element.ALIGN_CENTER);
            Image qr = leerImagen(comprobante.getQrPngBase64());
            qr.scaleAbsolute(LADO_QR_PT, LADO_QR_PT);
            qr.setAlignment(Element.ALIGN_CENTER);
            imagen.addElement(qr);
            bloque.addCell(imagen);

            PdfPCell pie = new PdfPCell(new Phrase("Código QR", fuente(6.6f, false, GRIS_SUAVE)));
            pie.setBorder(Rectangle.NO_BORDER);
            pie.setPadding(0);
            pie.setPaddingTop(3);
            pie.setHorizontalAlignment(Element.ALIGN_CENTER);
            bloque.addCell(pie);

            qrCelda.addElement(bloque);
        } else {
            Paragraph sinQr = new Paragraph("QR no disponible\npara este comprobante",
                    fuente(7, false, GRIS_TEXTO));
            sinQr.setAlignment(Element.ALIGN_CENTER);
            qrCelda.addElement(sinQr);
        }

        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        datos.setPadding(0);

        datos.addElement(new Paragraph("CÓDIGO HASH (VALOR RESUMEN)", fuente(6.8f, true, marca.secundario())));
        Paragraph hash = new Paragraph(partirHash(comprobante.getHashComprobante()), fuente(6.6f, false, GRIS_TEXTO));
        hash.setSpacingBefore(2);
        datos.addElement(hash);

        if (!vacio(comprobante.getValorResumen())) {
            Paragraph tituloResumen = new Paragraph("VALOR RESUMEN (BASE 64)", fuente(6.8f, true, marca.secundario()));
            tituloResumen.setSpacingBefore(6);
            datos.addElement(tituloResumen);
            datos.addElement(new Paragraph(partirHash(comprobante.getValorResumen()), fuente(6.6f, false, GRIS_TEXTO)));
        }

        List<LeyendaDTO> leyendas = comprobante.getLeyendas();
        if (leyendas != null) {
            for (LeyendaDTO leyenda : leyendas) {
                if (leyenda == null || vacio(leyenda.getTexto()) || "1000".equals(leyenda.getCodigo())) {
                    continue;
                }
                Paragraph linea = new Paragraph(leyenda.getTexto(), fuente(6.8f, false, GRIS_TEXTO));
                linea.setSpacingBefore(5);
                datos.addElement(linea);
            }
        }

        Paragraph nota = new Paragraph("Representación impresa del comprobante electrónico. "
                + "El código QR y el Valor Resumen permiten verificar su autenticidad ante SUNAT.",
                fuente(6.6f, false, GRIS_SUAVE));
        nota.setSpacingBefore(6);
        datos.addElement(nota);

        contenido.addCell(qrCelda);
        contenido.addCell(datos);

        PdfPTable marco = new PdfPTable(1);
        marco.setWidthPercentage(100);
        marco.setSpacingBefore(16);
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(GRIS_LINEA);
        celda.setBorderWidth(0.8f);
        celda.setBackgroundColor(FONDO_SUAVE);
        celda.setPadding(12);
        marco.addCell(celda);
        return marco;
    }

    private static Paragraph despedida(MarcaComprobante marca) {
        Paragraph parrafo = new Paragraph("¡Gracias por su compra!", fuente(9.5f, true, marca.primario()));
        parrafo.setAlignment(Element.ALIGN_CENTER);
        parrafo.setSpacingBefore(10);
        return parrafo;
    }

    // ------------------------------------------------------------------
    // Pie de pagina
    // ------------------------------------------------------------------

    private static final class PieDePagina extends PdfPageEventHelper {
        private final ComprobanteRespuestaDTO comprobante;

        private PieDePagina(ComprobanteRespuestaDTO comprobante) {
            this.comprobante = comprobante;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte lienzo = writer.getDirectContent();
            Rectangle pagina = document.getPageSize();

            lienzo.saveState();
            lienzo.setColorStroke(GRIS_LINEA);
            lienzo.setLineWidth(0.6f);
            lienzo.moveTo(document.leftMargin(), 44);
            lienzo.lineTo(pagina.getWidth() - document.rightMargin(), 44);
            lienzo.stroke();
            lienzo.restoreState();

            Font fuente = fuente(6.6f, false, GRIS_SUAVE);
            EmisorDTO emisor = comprobante.getEmisor();
            String izquierda = texto(emisor.getRazonSocial(), "") + " · R.U.C. "
                    + nulo(emisor.getRuc()) + " · Comprobante de pago electrónico";
            ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT, new Phrase(izquierda, fuente),
                    document.leftMargin(), 30, 0);
            ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), fuente),
                    pagina.getWidth() - document.rightMargin(), 30, 0);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static BaseFont cargarFuente(String ruta) {
        try (InputStream entrada = ComprobantePdfGenerator.class.getResourceAsStream(ruta)) {
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

    /** Ajusta el logo a la cabecera conservando su proporcion. */
    private static void escalarLogo(Image logo, float altoMaximo) {
        float anchoOriginal = logo.getWidth();
        float altoOriginal = logo.getHeight();
        float escala = Math.min(altoMaximo / altoOriginal, 150f / anchoOriginal);
        logo.scaleAbsolute(anchoOriginal * escala, altoOriginal * escala);
    }

    private static Image leerImagen(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        try {
            return Image.getInstance(bytes);
        } catch (Exception e) {
            throw new IOException("No se pudo incrustar el codigo QR en la representacion impresa", e);
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

    private static String nombreMoneda(String moneda) {
        if ("USD".equalsIgnoreCase(moneda)) {
            return "Dólares (USD)";
        }
        return "PEN".equalsIgnoreCase(moneda) || vacio(moneda) ? "Soles (PEN)" : moneda;
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
        if (!vacio(emisor.getDepartamento())) {
            texto.append(" - ").append(emisor.getDepartamento());
        }
        return texto.toString();
    }

    private static String contactoEmisor(EmisorDTO emisor) {
        StringBuilder texto = new StringBuilder();
        if (!vacio(emisor.getTelefono())) {
            texto.append("Tel. ").append(emisor.getTelefono());
        }
        if (!vacio(emisor.getEmail())) {
            texto.append(texto.isEmpty() ? "" : "  ·  ").append(emisor.getEmail());
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

    private static String unidad(String codigo) {
        if (codigo == null || codigo.isBlank() || "NIU".equalsIgnoreCase(codigo)) {
            return "UND";
        }
        if ("ZZ".equalsIgnoreCase(codigo)) {
            return "SERV";
        }
        return codigo.toUpperCase();
    }

    private static String condicion(String codigo) {
        return "02".equals(codigo) ? "Crédito" : "Contado";
    }

    private static String formaPago(String codigo) {
        return "02".equals(codigo) ? "Crédito" : "Contado";
    }

    /** Inserta espacios para que el hash de 64 caracteres pueda partirse. */
    private static String partirHash(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.trim();
        if (limpio.length() <= 44) {
            return limpio;
        }
        return limpio.replaceAll("(.{44})", "$1 ");
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
