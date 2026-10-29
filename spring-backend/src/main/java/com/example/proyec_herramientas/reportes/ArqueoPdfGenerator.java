package com.example.proyec_herramientas.reportes;

import com.example.proyec_herramientas.model.ArqueoComprobanteDTO;
import com.example.proyec_herramientas.model.ArqueoDTO;
import com.example.proyec_herramientas.model.ArqueoGrupoDTO;
import com.example.proyec_herramientas.model.ArqueoTurnoDTO;
import com.example.proyec_herramientas.model.ArqueoVendedorDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
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
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Corte de caja y arqueo en A4 para contabilidad. Usa la misma marca de la
 * tienda que los comprobantes (colores y logo) y las fuentes Unicode
 * incrustadas, para que tildes y simbolos se impriman en cualquier equipo.
 */
public final class ArqueoPdfGenerator {

    private static final BaseFont FUENTE_NORMAL = cargarFuente("/fonts/DejaVuSans.ttf");
    private static final BaseFont FUENTE_NEGRITA = cargarFuente("/fonts/DejaVuSans-Bold.ttf");

    private static final BaseColor GRIS_TEXTO = new BaseColor(0x5B, 0x67, 0x75);
    private static final BaseColor GRIS_LINEA = new BaseColor(0xE2, 0xE8, 0xF0);
    private static final BaseColor FONDO_SUAVE = new BaseColor(0xF6, 0xF8, 0xFB);
    private static final BaseColor TINTA = new BaseColor(0x0F, 0x17, 0x2A);

    private ArqueoPdfGenerator() {
    }

    public static byte[] generarPdfBytes(ArqueoDTO arqueo, MarcaComprobante marca) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 52);
        PdfWriter escritor = PdfWriter.getInstance(documento, salida);
        escritor.setPageEvent(new PieDePagina(arqueo.getTiendaNombre()));

        documento.open();
        try {
            documento.add(cabecera(arqueo, marca));
            documento.add(resumen(arqueo, marca));
            documento.add(tituloSeccion("Ingresos por método de pago", marca));
            documento.add(tablaMetodos(arqueo, marca));
            documento.add(tituloSeccion("Total facturado por vendedor", marca));
            documento.add(tablaVendedores(arqueo, marca));
            documento.add(tituloSeccion("Total facturado por turno", marca));
            documento.add(tablaTurnos(arqueo, marca));
            documento.add(tituloSeccion("Detalle de comprobantes", marca));
            documento.add(tablaDetalle(arqueo, marca));
            documento.add(nota(marca));
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------
    // Cabecera y resumen
    // ------------------------------------------------------------------

    private static PdfPTable cabecera(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = new PdfPTable(new float[]{64f, 36f});
        tabla.setWidthPercentage(100);

        PdfPCell izquierda = new PdfPCell();
        izquierda.setBorder(Rectangle.NO_BORDER);
        izquierda.setBackgroundColor(marca.primario());
        izquierda.setPadding(16);
        izquierda.setMinimumHeight(96);

        Image logo = LogoComprobante.cargar(marca.logoUrl());
        if (logo != null) {
            escalarLogo(logo);
            izquierda.addElement(logo);
        }

        Paragraph tienda = new Paragraph(texto(arqueo.getTiendaNombre(), "Mi tienda"),
                fuente(14, true, marca.textoPrimario()));
        tienda.setLeading(17);
        if (logo != null) {
            tienda.setSpacingBefore(7);
        }
        izquierda.addElement(tienda);

        Paragraph titulo = new Paragraph("CORTE DE CAJA Y ARQUEO", fuente(9, true, marca.textoPrimario()));
        titulo.setSpacingBefore(6);
        izquierda.addElement(titulo);

        Paragraph periodo = new Paragraph(periodoTexto(arqueo), fuente(9, false, marca.textoPrimario()));
        periodo.setSpacingBefore(3);
        izquierda.addElement(periodo);
        tabla.addCell(izquierda);

        PdfPCell derecha = new PdfPCell();
        derecha.setBorder(Rectangle.BOX);
        derecha.setBorderColor(GRIS_LINEA);
        derecha.setBorderWidth(0.8f);
        derecha.setPadding(14);
        derecha.setMinimumHeight(96);
        derecha.setBackgroundColor(BaseColor.WHITE);

        Paragraph tipo = new Paragraph("REPORTE INTERNO DE CAJA", fuente(8, true, GRIS_TEXTO));
        tipo.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(tipo);

        Paragraph ruc = new Paragraph("R.U.C. " + texto(arqueo.getTiendaRuc(), "—"), fuente(10, true, TINTA));
        ruc.setAlignment(Element.ALIGN_RIGHT);
        ruc.setSpacingBefore(6);
        derecha.addElement(ruc);

        Paragraph rango = new Paragraph("Del " + arqueo.getDesde() + " al " + arqueo.getHasta(),
                fuente(8.5f, false, GRIS_TEXTO));
        rango.setAlignment(Element.ALIGN_RIGHT);
        rango.setSpacingBefore(6);
        derecha.addElement(rango);

        Paragraph generado = new Paragraph("Generado el " + arqueo.getGeneradoEn(),
                fuente(8, false, GRIS_TEXTO));
        generado.setAlignment(Element.ALIGN_RIGHT);
        generado.setSpacingBefore(4);
        derecha.addElement(generado);
        tabla.addCell(derecha);
        return tabla;
    }

    private static PdfPTable resumen(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = new PdfPTable(new float[]{34f, 33f, 33f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);
        tabla.addCell(kpi("Total vendido", dinero(arqueo.getTotalVentas()), marca));
        tabla.addCell(kpi("Comprobantes", String.valueOf(arqueo.getComprobantes()), marca));
        tabla.addCell(kpi("Ticket promedio", dinero(arqueo.getTicketPromedio()), marca));
        return tabla;
    }

    private static PdfPCell kpi(String etiqueta, String valor, MarcaComprobante marca) {
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(FONDO_SUAVE);
        celda.setBorderColor(GRIS_LINEA);
        celda.setBorderWidth(0.6f);
        celda.setPadding(12);
        Paragraph titulo = new Paragraph(etiqueta, fuente(8, false, GRIS_TEXTO));
        celda.addElement(titulo);
        Paragraph dato = new Paragraph(valor, fuente(14, true, marca.secundario()));
        dato.setSpacingBefore(3);
        celda.addElement(dato);
        return celda;
    }

    // ------------------------------------------------------------------
    // Secciones
    // ------------------------------------------------------------------

    private static Paragraph tituloSeccion(String titulo, MarcaComprobante marca) {
        Paragraph parrafo = new Paragraph(titulo, fuente(11, true, marca.secundario()));
        parrafo.setSpacingBefore(16);
        parrafo.setSpacingAfter(6);
        return parrafo;
    }

    private static PdfPTable tablaMetodos(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = tablaBase(new float[]{28f, 34f, 12f, 10f, 16f});
        cabecera(tabla, marca, "Grupo", "Medios incluidos", "N.º", "%", "Total");
        for (ArqueoGrupoDTO grupo : arqueo.getMetodosPago()) {
            celdaTexto(tabla, grupo.getGrupo(), true);
            celdaTexto(tabla, texto(String.join(", ", grupo.getMetodos()), "—"), false);
            celdaNumero(tabla, String.valueOf(grupo.getComprobantes()));
            celdaNumero(tabla, dinero(grupo.getPorcentaje()) + " %");
            celdaNumero(tabla, dinero(grupo.getTotal()));
        }
        return tabla;
    }

    private static PdfPTable tablaVendedores(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = tablaBase(new float[]{40f, 24f, 14f, 22f});
        cabecera(tabla, marca, "Vendedor", "Usuario", "Comprobantes", "Total");
        if (arqueo.getVendedores().isEmpty()) {
            celdaMensaje(tabla, "Sin vendedores registrados.", 4);
        }
        for (ArqueoVendedorDTO vendedor : arqueo.getVendedores()) {
            celdaTexto(tabla, texto(vendedor.getNombre(), "—"), true);
            celdaTexto(tabla, texto(vendedor.getUsuario(), "—"), false);
            celdaNumero(tabla, String.valueOf(vendedor.getComprobantes()));
            celdaNumero(tabla, dinero(vendedor.getTotal()));
        }
        return tabla;
    }

    private static PdfPTable tablaTurnos(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = tablaBase(new float[]{26f, 34f, 16f, 24f});
        cabecera(tabla, marca, "Turno", "Horario", "Comprobantes", "Total");
        for (ArqueoTurnoDTO turno : arqueo.getTurnos()) {
            celdaTexto(tabla, turno.getTurno(), true);
            celdaTexto(tabla, texto(turno.getRango(), "—"), false);
            celdaNumero(tabla, String.valueOf(turno.getComprobantes()));
            celdaNumero(tabla, dinero(turno.getTotal()));
        }
        return tabla;
    }

    private static PdfPTable tablaDetalle(ArqueoDTO arqueo, MarcaComprobante marca) {
        PdfPTable tabla = tablaBase(new float[]{14f, 16f, 24f, 16f, 18f, 12f});
        cabecera(tabla, marca, "Fecha y hora", "Comprobante", "Cliente", "Medio", "Vendedor", "Total");
        if (arqueo.getDetalle().isEmpty()) {
            celdaMensaje(tabla, "No hay comprobantes en el periodo.", 6);
            return tabla;
        }
        for (ArqueoComprobanteDTO comprobante : arqueo.getDetalle()) {
            celdaTexto(tabla, comprobante.getFechaHora(), false);
            celdaTexto(tabla, texto(comprobante.getNumeroDocumento(), "—"), true);
            celdaTexto(tabla, texto(comprobante.getCliente(), "—"), false);
            celdaTexto(tabla, texto(comprobante.getMedioPago(), "—"), false);
            celdaTexto(tabla, texto(comprobante.getVendedor(), "—"), false);
            celdaNumero(tabla, dinero(comprobante.getTotal()));
        }
        return tabla;
    }

    private static Paragraph nota(MarcaComprobante marca) {
        Paragraph parrafo = new Paragraph(
                "Los importes por método de pago incluyen las ventas al crédito y los pagos mixtos;"
                        + " el grupo Créditos no representa efectivo cobrado en caja.",
                fuente(8, false, GRIS_TEXTO));
        parrafo.setSpacingBefore(12);
        parrafo.setLeading(10.5f);
        return parrafo;
    }

    // ------------------------------------------------------------------
    // Tablas
    // ------------------------------------------------------------------

    private static PdfPTable tablaBase(float[] anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        return tabla;
    }

    private static void cabecera(PdfPTable tabla, MarcaComprobante marca, String... titulos) {
        for (String titulo : titulos) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, fuente(7.8f, true, TINTA)));
            celda.setBackgroundColor(marca.primarioSuave());
            celda.setBorderColor(GRIS_LINEA);
            celda.setPadding(6);
            celda.setHorizontalAlignment(Element.ALIGN_LEFT);
            tabla.addCell(celda);
        }
    }

    private static void celdaTexto(PdfPTable tabla, String valor, boolean negrita) {
        PdfPCell celda = new PdfPCell(new Phrase(texto(valor, "—"), fuente(8.2f, negrita, TINTA)));
        celda.setBorderColor(GRIS_LINEA);
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private static void celdaNumero(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, fuente(8.2f, false, TINTA)));
        celda.setBorderColor(GRIS_LINEA);
        celda.setPadding(5);
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(celda);
    }

    private static void celdaMensaje(PdfPTable tabla, String mensaje, int columnas) {
        PdfPCell celda = new PdfPCell(new Phrase(mensaje, fuente(8.2f, false, GRIS_TEXTO)));
        celda.setColspan(columnas);
        celda.setBorderColor(GRIS_LINEA);
        celda.setPadding(8);
        tabla.addCell(celda);
    }

    // ------------------------------------------------------------------
    // Pie de pagina
    // ------------------------------------------------------------------

    private static final class PieDePagina extends PdfPageEventHelper {

        private final String tienda;

        private PieDePagina(String tienda) {
            this.tienda = texto(tienda, "Mi tienda");
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            PdfContentByte lienzo = escritor.getDirectContent();
            ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                    new Phrase("Corte de caja · " + tienda, fuente(7.5f, false, GRIS_TEXTO)),
                    documento.leftMargin(), documento.bottomMargin() - 14, 0);
            ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                    new Phrase("Página " + escritor.getPageNumber(), fuente(7.5f, false, GRIS_TEXTO)),
                    documento.right(), documento.bottomMargin() - 14, 0);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String periodoTexto(ArqueoDTO arqueo) {
        String periodo = "MENSUAL".equals(arqueo.getPeriodo()) ? "Arqueo mensual" : "Arqueo diario";
        return periodo + " · " + arqueo.getDesde()
                + ("MENSUAL".equals(arqueo.getPeriodo()) ? " al " + arqueo.getHasta() : "");
    }

    private static String dinero(BigDecimal valor) {
        BigDecimal importe = (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
        return "S/ " + importe.toPlainString();
    }

    private static String texto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    private static BaseFont cargarFuente(String ruta) {
        try (InputStream entrada = ArqueoPdfGenerator.class.getResourceAsStream(ruta)) {
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

    private static void escalarLogo(Image logo) {
        float anchoOriginal = logo.getWidth();
        float altoOriginal = logo.getHeight();
        float escala = Math.min(28f / altoOriginal, 130f / anchoOriginal);
        logo.scaleAbsolute(anchoOriginal * escala, altoOriginal * escala);
    }
}
