package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoDTO> listar(
            @RequestParam(required = false) String filtro,
            Authentication authentication) {

        int idTiendaActual = idTienda(authentication);

        if (filtro != null && !filtro.trim().isEmpty()) {
            return productoService.filtrarPorNombreOCodigo(idTiendaActual, filtro.trim());
        }

        return productoService.listar(idTiendaActual);
    }

    @GetMapping("/{id}")
    public ProductoDTO obtener(@PathVariable int id, Authentication authentication) {
        ProductoDTO producto = productoService.obtenerPorId(id, idTienda(authentication));
        if (producto == null) {
            throw new RecursoNoEncontradoException("Producto " + id + " no existe");
        }
        return producto;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<ProductoDTO> crear(@RequestBody ProductoDTO producto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(producto, idTienda(authentication)));
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable int id, @RequestBody ProductoDTO producto,
                                  Authentication authentication) {
        ProductoDTO actualizado = productoService.actualizar(id, producto, idTienda(authentication));
        if (actualizado == null) {
            throw new RecursoNoEncontradoException("Producto " + id + " no existe");
        }
        return actualizado;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id, Authentication authentication) {
        int idTienda = idTienda(authentication);
        if (productoService.obtenerPorId(id, idTienda) == null) {
            throw new RecursoNoEncontradoException("Producto " + id + " no existe");
        }
        productoService.eliminar(id, idTienda);
        return ResponseEntity.noContent().build();
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