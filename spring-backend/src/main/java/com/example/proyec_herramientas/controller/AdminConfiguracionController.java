package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ConfiguracionTiendaDTO;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ConfiguracionTiendaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Configuracion comercial y de facturacion de la tienda del ADMIN autenticado.
 * No recibe idTienda: sale siempre del token, nunca del cuerpo ni de la ruta.
 */
@RestController
@RequestMapping("/api/admin/configuracion")
public class AdminConfiguracionController {

    private final ConfiguracionTiendaService configuracionService;

    public AdminConfiguracionController(ConfiguracionTiendaService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public ConfiguracionTiendaDTO obtener(Authentication authentication) {
        return configuracionService.obtener(idTienda(authentication));
    }

    @PutMapping
    public ConfiguracionTiendaDTO actualizar(@RequestBody ConfiguracionTiendaDTO configuracion,
                                             Authentication authentication) {
        return configuracionService.actualizar(idTienda(authentication), configuracion);
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
