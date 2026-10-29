package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ActualizarEstadoRequest;
import com.example.proyec_herramientas.model.ActualizarPlanRequest;
import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.AdminTiendaDTO;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PaginaAuditoriaDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.PlanTiendaDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.RestablecerPasswordRequest;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;
import com.example.proyec_herramientas.service.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PutMapping("/empresas/{idTienda}/estado")
    public EmpresaResumenDTO cambiarEstado(@PathVariable int idTienda,
            @RequestBody ActualizarEstadoRequest request) {
        return superAdminService.cambiarEstado(idTienda, request.estado());
    }

    @GetMapping("/empresas/{idTienda}/plan")
    public PlanTiendaDTO plan(@PathVariable int idTienda) {
        return superAdminService.plan(idTienda);
    }

    @PutMapping("/empresas/{idTienda}/plan")
    public PlanTiendaDTO actualizarPlan(@PathVariable int idTienda,
            @RequestBody ActualizarPlanRequest request) {
        return superAdminService.actualizarPlan(idTienda, request);
    }

    @GetMapping("/empresas/{idTienda}/admin")
    public AdminTiendaDTO admin(@PathVariable int idTienda) {
        return superAdminService.admin(idTienda);
    }

    @PutMapping("/empresas/{idTienda}/admin/password")
    public AdminTiendaDTO restablecerPasswordAdmin(@PathVariable int idTienda,
            @RequestBody RestablecerPasswordRequest request) {
        return superAdminService.restablecerPasswordAdmin(idTienda, request);
    }

    @GetMapping("/empresas/{idTienda}/usuarios")
    public List<UsuarioStaffDTO> usuarios(@PathVariable int idTienda) {
        return superAdminService.usuarios(idTienda);
    }

    @PostMapping("/empresas/{idTienda}/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioStaffDTO crearUsuario(@PathVariable int idTienda,
            @RequestBody CrearUsuarioStaffRequest request) {
        return superAdminService.crearUsuario(idTienda, request);
    }

    @PutMapping("/empresas/{idTienda}/usuarios/{idUsuario}")
    public UsuarioStaffDTO actualizarUsuario(@PathVariable int idTienda,
            @PathVariable int idUsuario,
            @RequestBody ActualizarUsuarioStaffRequest request) {
        return superAdminService.actualizarUsuario(idTienda, idUsuario, request);
    }

    @PutMapping("/empresas/{idTienda}/usuarios/{idUsuario}/estado")
    public UsuarioStaffDTO cambiarEstadoUsuario(@PathVariable int idTienda,
            @PathVariable int idUsuario,
            @RequestBody EstadoUsuarioStaffRequest request) {
        return superAdminService.cambiarEstadoUsuario(idTienda, idUsuario, request);
    }

    @DeleteMapping("/empresas/{idTienda}/usuarios/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarUsuario(@PathVariable int idTienda, @PathVariable int idUsuario) {
        superAdminService.eliminarUsuario(idTienda, idUsuario);
    }

    @GetMapping("/auditoria")
    public PaginaAuditoriaDTO auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String q) {
        return superAdminService.auditoria(tipo, q, page, size);
    }
}
