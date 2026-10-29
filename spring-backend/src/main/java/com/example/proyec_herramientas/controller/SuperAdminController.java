package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;
import com.example.proyec_herramientas.service.SuperAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consola global del SUPER_ADMIN. SecurityConfig restringe todo este prefijo
 * al rol SUPER_ADMIN; la empresa se elige por parametro porque esta cuenta no
 * pertenece a ninguna tienda.
 */
@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/resumen")
    public SuperAdminResumenDTO resumen() {
        return superAdminService.resumen();
    }

    @GetMapping("/empresas")
    public List<EmpresaResumenDTO> empresas() {
        return superAdminService.empresas();
    }

    @GetMapping("/empresas/{idTienda}/dashboard")
    public SuperAdminEmpresaDTO dashboard(@PathVariable int idTienda) {
        return superAdminService.dashboard(idTienda);
    }

    @GetMapping("/empresas/{idTienda}/productos")
    public List<ProductoDTO> productos(@PathVariable int idTienda) {
        return superAdminService.productos(idTienda);
    }

    @GetMapping("/empresas/{idTienda}/comprobantes")
    public PaginaVentasDTO comprobantes(@PathVariable int idTienda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return superAdminService.comprobantes(idTienda, q, page, size);
    }
}
