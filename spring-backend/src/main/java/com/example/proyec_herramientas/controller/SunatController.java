package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.dto.DniResponseDTO;
import com.example.proyec_herramientas.dto.RucResponseDTO;
import com.example.proyec_herramientas.service.SunatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de DNI (RENIEC) y RUC (SUNAT) para el registro de usuarios. */
@RestController
@RequestMapping("/api/sunat")
public class SunatController {

    private final SunatService sunatService;

    public SunatController(SunatService sunatService) {
        this.sunatService = sunatService;
    }

    @GetMapping("/dni/{numero}")
    public DniResponseDTO dni(@PathVariable String numero) {
        return sunatService.consultarDni(numero);
    }

    @GetMapping("/ruc/{numero}")
    public RucResponseDTO ruc(@PathVariable String numero) {
        return sunatService.consultarRuc(numero);
    }
}
