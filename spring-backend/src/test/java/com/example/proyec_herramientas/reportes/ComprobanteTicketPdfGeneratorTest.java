package com.example.proyec_herramientas.reportes;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.facturacion.model.DetalleComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.DomicilioDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import com.example.proyec_herramientas.facturacion.model.LeyendaDTO;
import com.example.proyec_herramientas.facturacion.model.MedioPagoDTO;
import com.example.proyec_herramientas.facturacion.model.ReceptorDTO;
import com.example.proyec_herramientas.facturacion.model.TotalesDTO;
import com.example.proyec_herramientas.facturacion.service.QrCodeService;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El ticket termico debe tener el ancho exacto del papel (80 mm), una altura
 * ajustada al contenido y una sola pagina para una venta tipica.
 */
class ComprobanteTicketPdfGeneratorTest {

    private static final float ANCHO_80_MM = 226.77f;

    @Test
    void generaTicketDe80mmConUnaSolaPagina() throws Exception {
        ComprobanteRespuestaDTO comprobante = comprobanteDeEjemplo(2);

        byte[] pdf = ComprobanteTicketPdfGenerator.generarPdfBytes(comprobante);

        assertNotNull(pdf);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF-"),
                "El ticket debe ser un PDF valido");

        PdfReader lector = new PdfReader(pdf);
        try {
            assertEquals(1, lector.getNumberOfPages(), "Un ticket corto debe ocupar una sola pagina");
            Rectangle pagina = lector.getPageSize(1);
            assertEquals(ANCHO_80_MM, pagina.getWidth(), 0.5f, "El ancho debe ser el de 80 mm");
            assertTrue(pagina.getHeight() > 300f && pagina.getHeight() < 900f,
                    "La altura debe ajustarse al contenido, no a una hoja fija");
        } finally {
            lector.close();
        }
    }

    @Test
    void laAlturaCreceConElNumeroDeItems() throws Exception {
        PdfReader corto = new PdfReader(ComprobanteTicketPdfGenerator.generarPdfBytes(comprobanteDeEjemplo(1)));
        PdfReader largo = new PdfReader(ComprobanteTicketPdfGenerator.generarPdfBytes(comprobanteDeEjemplo(14)));
        try {
            assertTrue(largo.getPageSize(1).getHeight() > corto.getPageSize(1).getHeight() + 100f,
                    "Cada item debe alargar el ticket");
        } finally {
            corto.close();
            largo.close();
        }
    }

    /** Deja los PDF en target/pdfs para revisarlos a ojo durante el desarrollo. */
    @Test
    void dejaLosPdfParaRevisionVisual() throws Exception {
        MarcaComprobante marca = new MarcaComprobante("#0071e3", "#1d1d1f");
        Path carpeta = Path.of("target", "pdfs");
        Files.createDirectories(carpeta);

        ComprobanteRespuestaDTO comprobante = comprobanteDeEjemplo(2);
        Files.write(carpeta.resolve("comprobante-a4.pdf"),
                ComprobantePdfGenerator.generarPdfBytes(comprobante, marca));
        Files.write(carpeta.resolve("ticket-80mm.pdf"),
                ComprobanteTicketPdfGenerator.generarPdfBytes(comprobante, marca));
    }

    private ComprobanteRespuestaDTO comprobanteDeEjemplo(int items) {
        EmisorDTO emisor = new EmisorDTO();
        emisor.setRuc("20512345678");
        emisor.setRazonSocial("INNOVATION STORE S.A.C.");
        emisor.setNombreComercial("TECHZONE");
        emisor.setDomicilioFiscal("Av. Javier Prado Este 4200");
        emisor.setUbigeo("150131");
        emisor.setDistrito("Santiago de Surco");
        emisor.setProvincia("Lima");
        emisor.setDepartamento("Lima");
        emisor.setTelefono("(01) 555-1234");
        emisor.setEmail("ventas@techzone.pe");

        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento("1");
        receptor.setNumeroDocumento("44556677");
        receptor.setRazonSocial("MARIA FERNANDA QUISPE ROJAS");
        DomicilioDTO domicilio = new DomicilioDTO();
        domicilio.setDireccion("Av. Arequipa 1234, Dpto 302");
        domicilio.setDistrito("Lima");
        receptor.setDomicilioFiscal(domicilio);
        receptor.setCorreo("maria.quispe@correo.com");

        List<DetalleComprobanteDTO> detalles = new ArrayList<>();
        String[] nombres = {
                "Laptop Lenovo IdeaPad 3 15.6\" Core i5 8GB 512GB SSD",
                "Mouse inalámbrico Logitech M185",
                "Teclado mecánico Redragon Kumara K552",
                "Monitor Samsung 24\" Full HD",
                "Audífonos Sony WH-CH510",
                "Base refrigerante para laptop",
                "Memoria USB Kingston 64GB",
                "Cable HDMI 2.1 de 2 metros",
                "Webcam Logitech C920 Full HD",
                "Hub USB-C 7 en 1",
                "Disco externo Seagate 1TB",
                "Funda para laptop 15.6\"",
                "Mouse pad gamer XL",
                "Parlante Bluetooth JBL Go 3"
        };
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < items; i++) {
            BigDecimal precio = new BigDecimal(i == 0 ? "2500.00" : "149.90");
            BigDecimal valor = precio.divide(new BigDecimal("1.18"), 6, java.math.RoundingMode.HALF_UP);
            BigDecimal base = valor.multiply(BigDecimal.ONE).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal igv = precio.subtract(base);

            DetalleComprobanteDTO detalle = new DetalleComprobanteDTO();
            detalle.setNumero(i + 1);
            detalle.setIdProducto(i + 1);
            detalle.setCodigo("P" + (i + 1));
            detalle.setDescripcion(nombres[i % nombres.length]);
            detalle.setCantidad(BigDecimal.ONE);
            detalle.setUnidadMedida("NIU");
            detalle.setValorUnitario(valor);
            detalle.setPrecioUnitario(precio);
            detalle.setAfectacionIgv("01");
            detalle.setBase(base);
            detalle.setIgv(igv);
            detalle.setTotalItem(precio);
            detalles.add(detalle);
            total = total.add(precio);
        }

        BigDecimal baseTotal = total.divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP);
        TotalesDTO totales = new TotalesDTO();
        totales.setOpGravada(baseTotal);
        totales.setIgv(total.subtract(baseTotal));
        totales.setTotalVenta(total);
        totales.setMontoTotalPagar(total);
        totales.setTotalEnLetras("DOS MIL SEISCIENTOS CUARENTA Y NUEVE CON NOVENTA/100");

        MedioPagoDTO medio = new MedioPagoDTO();
        medio.setCodigo("006");
        medio.setDescripcion("Tarjeta de crédito");
        medio.setMonto(total);

        ComprobanteRespuestaDTO comprobante = new ComprobanteRespuestaDTO();
        comprobante.setTipoComprobante("03");
        comprobante.setTipoComprobanteDescripcion("Boleta de venta");
        comprobante.setNumeroDocumento("B001-000016");
        comprobante.setSerie("B001");
        comprobante.setCorrelativo(16);
        comprobante.setFechaEmision("2026-10-22");
        comprobante.setHoraEmision("10:15:30");
        comprobante.setMoneda("PEN");
        comprobante.setCondicionOperacion("01");
        comprobante.setFormaPago("01");
        comprobante.setEmisor(emisor);
        comprobante.setReceptor(receptor);
        comprobante.setTotales(totales);
        comprobante.setDetalles(detalles);
        comprobante.setMediosPago(List.of(medio));
        comprobante.setLeyendas(List.of(
                new LeyendaDTO("1000", "SON: DOS MIL SEISCIENTOS CUARENTA Y NUEVE CON NOVENTA/100"),
                new LeyendaDTO(null, "Representación impresa de INNOVATION STORE S.A.C."),
                new LeyendaDTO(null, "Boleta de venta electrónica")));
        comprobante.setHashComprobante(
                "a8483b1a1881773a15df475573b60d99336bcc5b72830724aee6c5c5df00f8e5");
        comprobante.setValorResumen("qEg7GhiBdzoV30dVc7YNmTNrzFtygwckrubFxd8A+OU=");
        comprobante.setCodigoQr("20512345678|03|B001|000016|403.71|2649.90|2026-10-22|1|44556677|"
                + "qEg7GhiBdzoV30dVc7YNmTNrzFtygwckrubFxd8A+OU=");
        comprobante.setQrPngBase64(new QrCodeService().generarPngBase64(comprobante.getCodigoQr()));
        return comprobante;
    }
}
