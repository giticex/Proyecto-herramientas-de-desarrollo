package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<Venta> registrar(@Valid @RequestBody CarritoRequestDTO carrito,
            Authentication authentication) {
        // El ADMIN registra ventas de cualquiera; el CLIENTE solo de su cuenta.
        if (esCliente(authentication) && !perteneceAlComprador(authentication, carrito.getIdCliente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No puedes registrar una compra a nombre de otro cliente");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarVenta(carrito));
    }

    private boolean esCliente(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof SesionPrincipal principal
                && "CLIENTE".equals(principal.rol());
    }

    private boolean perteneceAlComprador(Authentication authentication, String idCliente) {
        SesionPrincipal principal = (SesionPrincipal) authentication.getPrincipal();
        return principal.idCliente() != null && principal.idCliente().equals(idCliente);
    }
}
