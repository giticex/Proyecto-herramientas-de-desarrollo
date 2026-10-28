package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.facturacion.service.ComprobanteService;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Endpoints de los comprobantes de pago electronicos: emision, consulta,
 * representacion impresa (PDF), XML UBL 2.1 y codigo QR.
 */
@RestController
@RequestMapping("/api/comprobantes")
public class ComprobanteController {

    private final ComprobanteService comprobanteService;
    private final AccesoService accesoService;

    public ComprobanteController(ComprobanteService comprobanteService, AccesoService accesoService) {
        this.comprobanteService = comprobanteService;
        this.accesoService = accesoService;
    }

    @PostMapping
    public ResponseEntity<ComprobanteRespuestaDTO> emitir(@RequestBody ComprobanteRequestDTO request,
            Authentication authentication) {
        accesoService.prepararEmision(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comprobanteService.emitir(request));
    }

    @GetMapping("/{idVenta}")
    public ResponseEntity<ComprobanteRespuestaDTO> obtener(@PathVariable String idVenta,
            Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        return ResponseEntity.ok(comprobanteService.obtenerPorId(idVenta));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<ComprobanteRespuestaDTO>> listarPorCliente(@PathVariable String idCliente,
            Authentication authentication) {
        accesoService.validarCliente(idCliente, authentication);
        SesionPrincipal sesion = accesoService.principal(authentication);
        if (accesoService.esCliente(sesion)) {
            return ResponseEntity.ok(comprobanteService.listarPorCliente(idCliente));
        }
        return ResponseEntity.ok(comprobanteService.listarPorClienteDeTienda(idCliente,
                accesoService.tiendaObligatoria(sesion)));
    }

    /** Representacion impresa del comprobante (QR y valor resumen incluidos). */
    @GetMapping(value = "/{idVenta}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String idVenta, Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        ComprobanteRespuestaDTO comprobante = comprobanteService.obtenerPorId(idVenta);
        byte[] pdf = comprobanteService.generarPdf(idVenta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + comprobante.getNumeroDocumento() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Ticket de 80 mm para impresora termica (mismo comprobante, formato compacto). */
    @GetMapping(value = "/{idVenta}/ticket", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> ticket(@PathVariable String idVenta, Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        ComprobanteRespuestaDTO comprobante = comprobanteService.obtenerPorId(idVenta);
        byte[] pdf = comprobanteService.generarTicketPdf(idVenta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"ticket-" + comprobante.getNumeroDocumento() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** XML UBL 2.1 con el nombre normativo del archivo. */
    @GetMapping(value = "/{idVenta}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> xml(@PathVariable String idVenta, Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        ComprobanteRespuestaDTO comprobante = comprobanteService.obtenerPorId(idVenta);
        byte[] xml = comprobanteService.generarXml(idVenta).getBytes(StandardCharsets.UTF_8);
        String nombre = comprobante.getNombreArchivoXml() == null
                ? comprobante.getNumeroDocumento() + ".XML" : comprobante.getNombreArchivoXml();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    /** Codigo QR del comprobante en PNG. */
    @GetMapping(value = "/{idVenta}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String idVenta, Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        ComprobanteRespuestaDTO comprobante = comprobanteService.obtenerPorId(idVenta);
        if (comprobante.getQrPngBase64() == null || comprobante.getQrPngBase64().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        byte[] png = Base64.getDecoder().decode(comprobante.getQrPngBase64());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
