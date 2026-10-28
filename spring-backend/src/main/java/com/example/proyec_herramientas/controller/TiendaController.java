package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.TiendaDTO;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ProductoService;
import com.example.proyec_herramientas.service.TiendaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tiendas")
public class TiendaController {

    private final TiendaService tiendaService;
    private final ProductoService productoService;

    public TiendaController(TiendaService tiendaService, ProductoService productoService) {
        this.tiendaService = tiendaService;
        this.productoService = productoService;
    }

    // 1. Debe ir primero la ruta fija. Con sesion devuelve la tienda del token;
    //    sin sesion (landing publica) cae al slug por defecto.
    @GetMapping("/mi-tienda")
    public TiendaDTO obtenerMiTienda(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SesionPrincipal principal
                && principal.idTienda() != null) {
            return tiendaService.obtenerPorIdTienda(principal.idTienda());
        }
        return tiendaService.obtenerPorSlug("mi-tienda");
    }

    // 2. Después la ruta dinámica con variable
    @GetMapping("/{slug}")
    public TiendaDTO obtener(@PathVariable String slug) {
        return tiendaService.obtenerPorSlug(slug);
    }

    @GetMapping("/{slug}/productos")
    public List<ProductoDTO> productos(@PathVariable String slug) {
        int idTienda = tiendaService.obtenerIdPorSlug(slug);
        return productoService.listar(idTienda);
    }

    @PutMapping("/{slug}")
    public TiendaDTO actualizar(@PathVariable String slug, @RequestBody TiendaDTO tienda,
                                Authentication authentication) {
        return tiendaService.actualizar(slug, tienda, idTienda(authentication));
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