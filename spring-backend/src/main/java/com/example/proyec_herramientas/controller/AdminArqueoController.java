package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ArqueoDTO;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ArqueoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Corte de caja y arqueo de la tienda del ADMIN autenticado. No recibe
 * idTienda: sale siempre del token, nunca del cuerpo ni de la ruta.
 */
@RestController
@RequestMapping("/api/admin/arqueo")
public class AdminArqueoController {

    private final ArqueoService arqueoService;

    public AdminArqueoController(ArqueoService arqueoService) {
        this.arqueoService = arqueoService;
    }

    @GetMapping
    public ArqueoDTO obtener(
            @RequestParam(defaultValue = "diario") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {
        return arqueoService.generar(idTienda(authentication), periodo, fecha);
    }

    // Descarga del reporte para contabilidad: CSV estructurado o PDF A4.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(defaultValue = "diario") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "csv") String formato,
            Authentication authentication) {
        int idTienda = idTienda(authentication);
        String extension = normalizarFormato(formato);
        byte[] archivo = "pdf".equals(extension)
                ? arqueoService.exportarPdf(idTienda, periodo, fecha)
                : arqueoService.exportarCsv(idTienda, periodo, fecha);
        MediaType tipo = "pdf".equals(extension)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/csv; charset=UTF-8");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + arqueoService.nombreArchivo(periodo, fecha, extension) + "\"")
                .contentType(tipo)
                .body(archivo);
    }

    private String normalizarFormato(String formato) {
        String valor = formato == null ? "" : formato.trim().toLowerCase();
        if ("csv".equals(valor) || "pdf".equals(valor)) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El formato debe ser csv o pdf.");
    }

    private int idTienda(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SesionPrincipal principal
                && principal.idTienda() != null) {
            return principal.idTienda();
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta no tiene una tienda asignada");
    }
}
