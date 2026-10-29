package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PersonalTiendaDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.AdminPersonalService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de personal del panel de tienda. SecurityConfig restringe el
 * prefijo al rol ADMIN y la empresa sale siempre del token: nunca se acepta
 * un idTienda en la peticion, asi que un administrador no puede operar sobre
 * el personal de otra empresa.
 */
@RestController
@RequestMapping("/api/admin/personal")
public class AdminPersonalController {

    private final AdminPersonalService adminPersonalService;
    private final AccesoService accesoService;

    public AdminPersonalController(AdminPersonalService adminPersonalService,
            AccesoService accesoService) {
        this.adminPersonalService = adminPersonalService;
        this.accesoService = accesoService;
    }

    @GetMapping
    public PersonalTiendaDTO listar(Authentication authentication) {
        return adminPersonalService.listar(tiendaDelAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioStaffDTO crear(@RequestBody CrearUsuarioStaffRequest request,
            Authentication authentication) {
        return adminPersonalService.crear(tiendaDelAdmin(authentication), request);
    }

    @PutMapping("/{idUsuario}")
    public UsuarioStaffDTO actualizar(@PathVariable int idUsuario,
            @RequestBody ActualizarUsuarioStaffRequest request, Authentication authentication) {
        return adminPersonalService.actualizar(tiendaDelAdmin(authentication), idUsuario, request);
    }

    @PutMapping("/{idUsuario}/estado")
    public UsuarioStaffDTO cambiarEstado(@PathVariable int idUsuario,
            @RequestBody EstadoUsuarioStaffRequest request, Authentication authentication) {
        return adminPersonalService.cambiarEstado(tiendaDelAdmin(authentication), idUsuario,
                request);
    }

    private int tiendaDelAdmin(Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        if (!Rol.ADMIN.equals(sesion.rol())) {
            throw new AccessDeniedException(
                    "Solo el administrador de la tienda gestiona su personal");
        }
        return accesoService.tiendaObligatoria(sesion);
    }
}
