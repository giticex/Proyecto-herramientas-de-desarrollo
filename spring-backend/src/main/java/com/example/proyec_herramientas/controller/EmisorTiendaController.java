package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.facturacion.model.ConfiguracionEmisor;
import com.example.proyec_herramientas.facturacion.service.EmisorTiendaService;
import com.example.proyec_herramientas.security.AmbitoVentas;
import com.example.proyec_herramientas.security.SesionPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturacion/emisor")
public class EmisorTiendaController {
    private final EmisorTiendaService emisores;
    private final AmbitoVentas ambito;

    public EmisorTiendaController(EmisorTiendaService emisores, AmbitoVentas ambito) {
        this.emisores = emisores;
        this.ambito = ambito;
    }

    @GetMapping
    public ResponseEntity<ConfiguracionEmisor> obtener(@AuthenticationPrincipal SesionPrincipal sesion) {
        return emisores.obtener(ambito.tiendaAdministrada(sesion))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ConfiguracionEmisor guardar(@AuthenticationPrincipal SesionPrincipal sesion,
            @Valid @RequestBody ConfiguracionEmisor configuracion) {
        return emisores.guardar(ambito.tiendaAdministrada(sesion), configuracion);
    }
}
