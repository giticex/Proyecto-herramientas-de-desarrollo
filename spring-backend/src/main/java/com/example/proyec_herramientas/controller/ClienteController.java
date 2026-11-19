package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.security.AmbitoVentas;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ClienteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final AmbitoVentas ambito;

    public ClienteController(ClienteService clienteService, AmbitoVentas ambito) {
        this.clienteService = clienteService;
        this.ambito = ambito;
    }

    @GetMapping
    public List<ClienteDTO> listar(@AuthenticationPrincipal SesionPrincipal sesion) {
        return clienteService.listar(ambito.tiendaAdministrada(sesion));
    }
}