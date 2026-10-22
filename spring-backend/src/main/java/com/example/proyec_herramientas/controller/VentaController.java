package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.config.UsuarioAutenticado;
import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Solo CLIENTE. idCliente sale siempre del token, nunca del body.
@RestController
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<Venta> registrar(@Valid @RequestBody CarritoRequestDTO carrito,
            @AuthenticationPrincipal UsuarioAutenticado cliente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarVenta(cliente.getId(), carrito));
    }

    @GetMapping("/mias")
    public List<Venta> mias(@AuthenticationPrincipal UsuarioAutenticado cliente) {
        return ventaService.obtenerHistorial(cliente.getId());
    }
}
