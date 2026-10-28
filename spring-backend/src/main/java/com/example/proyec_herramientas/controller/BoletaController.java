package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ReportePdfService;
import com.example.proyec_herramientas.service.VentaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de reportes del panel: siempre acotados a la tienda del personal
// autenticado (el idTienda sale del token, nunca de la URL).
@RestController
@RequestMapping("/api")
public class BoletaController {

    private static final MediaType PDF = MediaType.parseMediaType("application/pdf");

    private final VentaService ventaService;
    private final ReportePdfService reportePdfService;
    private final AccesoService accesoService;

    public BoletaController(VentaService ventaService, ReportePdfService reportePdfService,
            AccesoService accesoService) {
        this.ventaService = ventaService;
        this.reportePdfService = reportePdfService;
        this.accesoService = accesoService;
    }

    @GetMapping("/boletas")
    public List<Venta> boletas(Authentication authentication) {
        return ventaService.obtenerBoletasDeTienda(idTienda(authentication));
    }

    @GetMapping("/historial")
    public List<Venta> historial(@RequestParam(required = false) String clienteId,
            Authentication authentication) {
        return ventaService.obtenerHistorialDeTienda(idTienda(authentication), clienteId);
    }

    @GetMapping("/boleta/{id}/pdf")
    public ResponseEntity<byte[]> boletaPdf(@PathVariable String id, Authentication authentication) {
        accesoService.validarVenta(id, authentication);
        return pdf(id, "boleta", reportePdfService.generarBoletaPdf(id));
    }

    @GetMapping("/factura/{id}/pdf")
    public ResponseEntity<byte[]> facturaPdf(@PathVariable String id, Authentication authentication) {
        accesoService.validarVenta(id, authentication);
        return pdf(id, "factura", reportePdfService.generarFacturaPdf(id));
    }

    private int idTienda(Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        return accesoService.tiendaObligatoria(sesion);
    }

    private ResponseEntity<byte[]> pdf(String id, String tipo, byte[] bytes) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + tipo + "-" + id + ".pdf\"")
                .contentType(PDF)
                .body(bytes);
    }
}
