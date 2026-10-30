package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.AperturaCajaRequest;
import com.example.proyec_herramientas.model.CajaEstadoDTO;
import com.example.proyec_herramientas.model.CierreCajaRequest;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.CajaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Caja del POS. La tienda y el vendedor salen siempre del token: el cuerpo de
 * la solicitud solo trae el fondo inicial o el efectivo contado.
 */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final CajaService cajaService;
    private final AccesoService accesoService;

    public CajaController(CajaService cajaService, AccesoService accesoService) {
        this.cajaService = cajaService;
        this.accesoService = accesoService;
    }

    @GetMapping
    public CajaEstadoDTO estado(Authentication authentication) {
        return cajaService.estado(principal(authentication));
    }

    @PostMapping("/apertura")
    public CajaEstadoDTO abrir(@Valid @RequestBody AperturaCajaRequest request,
            Authentication authentication) {
        return cajaService.abrir(principal(authentication), request);
    }

    @PostMapping("/cierre")
    public CajaEstadoDTO cerrar(@Valid @RequestBody CierreCajaRequest request,
            Authentication authentication) {
        return cajaService.cerrar(principal(authentication), request);
    }

    private SesionPrincipal principal(Authentication authentication) {
        return accesoService.principal(authentication);
    }
}
