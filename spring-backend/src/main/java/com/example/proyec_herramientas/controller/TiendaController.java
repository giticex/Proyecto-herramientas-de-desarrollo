package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.TiendaDTO;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ProductoService;
import com.example.proyec_herramientas.service.TiendaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tiendas")
public class TiendaController {

    private final TiendaService tiendaService;
    private final ProductoService productoService;

    public TiendaController(TiendaService tiendaService, ProductoService productoService) {
        this.tiendaService = tiendaService;
        this.productoService = productoService;
    }

    // Publico: lo consume el catalogo de la tienda.
    @GetMapping("/{slug}")
    public TiendaDTO obtener(@PathVariable String slug) {
        return tiendaService.obtenerPorSlug(slug);
    }

    // Publico: catalogo de productos de esa tienda.
    @GetMapping("/{slug}/productos")
    public List<ProductoDTO> productos(@PathVariable String slug) {
        int idTienda = tiendaService.obtenerIdPorSlug(slug);
        return productoService.listar(idTienda);
    }

    // Solo el ADMIN dueño de esa tienda.
    @PutMapping("/{slug}")
    public TiendaDTO actualizar(@PathVariable String slug, @RequestBody TiendaDTO tienda,
            @AuthenticationPrincipal SesionPrincipal admin) {
        return tiendaService.actualizar(slug, tienda, admin.idTienda());
    }
}
