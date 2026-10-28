package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.RolDTO;
import com.example.proyec_herramientas.service.RolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catalogo de roles del sistema (ADMIN, CLIENTE). Es publico a proposito:
// el frontend lo necesita para mostrar los nombres de los roles.
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public List<RolDTO> listar() {
        return rolService.listar();
    }
}
