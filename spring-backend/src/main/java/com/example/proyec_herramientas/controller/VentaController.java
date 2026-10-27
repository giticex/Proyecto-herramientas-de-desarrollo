package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // El CLIENTE compra siempre a su propio nombre: el idCliente sale del token
    // y se ignora el que venga en el cuerpo. El ADMIN y el VENDEDOR si pueden
    // indicarlo (venta de mostrador); si no lo envian, VentaService usa el
    // cliente de mostrador configurado.
    @PostMapping
    public ResponseEntity<Venta> registrar(@Valid @RequestBody CarritoRequestDTO carrito,
            Authentication authentication) {
        SesionPrincipal principal = principal(authentication);
        String idCliente;
        if (Rol.CLIENTE.equals(principal.rol())) {
            idCliente = principal.idCliente();
            if (idCliente == null || idCliente.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se pudo determinar el cliente de la venta");
            }
        } else {
            idCliente = carrito.getIdCliente();
            if (idCliente != null && idCliente.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se pudo determinar el cliente de la venta");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarVenta(idCliente, carrito));
    }

    // GET /ventas -> historial paginado de la tienda del ADMIN autenticado.
    // El idTienda sale del token; el filtro busca por comprobante o documento.
    @GetMapping
    public PaginaVentasDTO historial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q,
            Authentication authentication) {
        return ventaService.listarVentas(idTienda(authentication), q, page, size);
    }

    // GET /ventas/mias -> historial de compras del CLIENTE que llamo.
    @GetMapping("/mias")
    public List<Venta> mias(Authentication authentication) {
        SesionPrincipal principal = principal(authentication);
        if (principal.idCliente() == null || principal.idCliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene un cliente asociado");
        }
        return ventaService.obtenerHistorial(principal.idCliente());
    }

    private int idTienda(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SesionPrincipal principal
                && principal.idTienda() != null) {
            return principal.idTienda();
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta no tiene una tienda asignada");
    }

    private SesionPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SesionPrincipal sesion) {
            return sesion;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no iniciada");
    }
}
