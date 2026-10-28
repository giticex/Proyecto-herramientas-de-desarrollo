package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.DevolucionRequestDTO;
import com.example.proyec_herramientas.persistence.Devolucion;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.service.DevolucionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    // Registra la devolucion de productos de una venta previa.
    @PostMapping
    public ResponseEntity<Devolucion> registrar(@Valid @RequestBody DevolucionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devolucionService.registrarDevolucion(request, authentication));
    }

    // Venta con sus detalles y lo ya devuelto por linea, para armar la
    // devolucion en el POS. La pertenencia la valida DevolucionService.
    @GetMapping("/ventas/{idVenta}")
    public Venta ventaParaDevolucion(@PathVariable String idVenta, Authentication authentication) {
        return devolucionService.obtenerVentaParaDevolucion(idVenta, authentication);
    }
}
