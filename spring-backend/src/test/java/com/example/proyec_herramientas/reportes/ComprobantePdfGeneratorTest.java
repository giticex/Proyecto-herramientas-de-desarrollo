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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La representacion impresa (A5) debe generarse sin errores de sintaxis PDF
 * e incluir el codigo QR y el valor resumen.
 */
class ComprobantePdfGeneratorTest {

    @Test
    void generaPdfDeLaRepresentacionImpresa() throws Exception {
        ComprobanteRespuestaDTO comprobante = comprobanteDeEjemplo();

        byte[] pdf = ComprobantePdfGenerator.generarPdfBytes(comprobante);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000, "El PDF generado debe tener contenido");
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"),
                "El archivo debe ser un PDF valido");
    }

    private ComprobanteRespuestaDTO comprobanteDeEjemplo() {
        EmisorDTO emisor = new EmisorDTO();
        emisor.setRuc("20512345678");
        emisor.setRazonSocial("INNOVATION STORE S.A.C.");
        emisor.setNombreComercial("TECHZONE");
        emisor.setDomicilioFiscal("Av. Javier Prado Este 4200");
        emisor.setUbigeo("150131");
        emisor.setDistrito("Santiago de Surco");
        emisor.setProvincia("Lima");
        emisor.setDepartamento("Lima");

        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento("1");
        receptor.setNumeroDocumento("20604567891");
        receptor.setRazonSocial("CLIENTE EMPRESARIAL S.A.C.");
        DomicilioDTO domicilio = new DomicilioDTO();
        domicilio.setDireccion("Av. Arequipa 1234, Dpto 302");
        domicilio.setDistrito("Lima");
        receptor.setDomicilioFiscal(domicilio);

        DetalleComprobanteDTO detalle = new DetalleComprobanteDTO();
        detalle.setNumero(1);
        detalle.setIdProducto(1);
        detalle.setCodigo("P001");
        detalle.setDescripcion("Laptop Lenovo");
        detalle.setCantidad(new BigDecimal("2"));
        detalle.setUnidadMedida("NIU");
        detalle.setValorUnitario(new BigDecimal("2118.644068"));
        detalle.setPrecioUnitario(new BigDecimal("2500.00"));
        detalle.setAfectacionIgv("01");
        detalle.setBase(new BigDecimal("4237.29"));
        detalle.setIgv(new BigDecimal("762.71"));
        detalle.setTotalItem(new BigDecimal("5000.00"));

        TotalesDTO totales = new TotalesDTO();
        totales.setOpGravada(new BigDecimal("4237.29"));
        totales.setOpInafecta(new BigDecimal("60.00"));
        totales.setIgv(new BigDecimal("762.71"));
        totales.setTotalVenta(new BigDecimal("5060.00"));
        totales.setMontoTotalPagar(new BigDecimal("5060.00"));
        totales.setTotalEnLetras("CINCO MIL SESENTA CON CERO/100");

        MedioPagoDTO medio = new MedioPagoDTO();
        medio.setCodigo("006");
        medio.setDescripcion("Tarjeta de credito");
        medio.setMonto(new BigDecimal("5060.00"));

        ComprobanteRespuestaDTO comprobante = new ComprobanteRespuestaDTO();
        comprobante.setTipoComprobante("01");
        comprobante.setTipoComprobanteDescripcion("Factura");
        comprobante.setNumeroDocumento("F001-000010");
        comprobante.setSerie("F001");
        comprobante.setCorrelativo(10);
        comprobante.setFechaEmision("2026-10-22");
        comprobante.setHoraEmision("10:15:30");
        comprobante.setMoneda("PEN");
        comprobante.setEmisor(emisor);
        comprobante.setReceptor(receptor);
        comprobante.setTotales(totales);
        comprobante.setDetalles(List.of(detalle));
        comprobante.setMediosPago(List.of(medio));
        comprobante.setLeyendas(List.of(
                new LeyendaDTO("1000", "SON: CINCO MIL SESENTA CON CERO/100"),
                new LeyendaDTO(null, "Representacion impresa de INNOVATION STORE S.A.C.")));
        comprobante.setHashComprobante(
                "a8483b1a1881773a15df475573b60d99336bcc5b72830724aee6c5c5df00f8e5");
        comprobante.setValorResumen("qEg7GhiBdzoV30dVc7YNmTNrzFtygwckrubFxd8A+OU=");
        comprobante.setCodigoQr("20512345678|01|F001|000010|762.71|5060.00|2026-10-22|1|20604567891|"
                + "qEg7GhiBdzoV30dVc7YNmTNrzFtygwckrubFxd8A+OU=");
        comprobante.setQrPngBase64(new QrCodeService().generarPngBase64(comprobante.getCodigoQr()));
        return comprobante;
    }
}
