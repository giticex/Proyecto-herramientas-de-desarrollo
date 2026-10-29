package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.CanjeCreditoDTO;
import com.example.proyec_herramientas.model.ReembolsoCreditoDTO;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.CreditoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/creditos")
public class CreditoController {

    private final CreditoService creditoService;
    private final AccesoService accesoService;

    public CreditoController(CreditoService creditoService, AccesoService accesoService) {
        this.creditoService = creditoService;
        this.accesoService = accesoService;
    }

    @GetMapping
    public List<Credito> listar() {
        return creditoService.listar();
    }

    // Créditos a favor del CLIENTE autenticado. El idCliente sale del token,
    // nunca de la URL, para que nadie pueda pedir los de otro cliente.
    @GetMapping("/mios")
    public List<Credito> mios(Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        if (sesion.idCliente() == null || sesion.idCliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene un cliente asociado");
        }
        return creditoService.listarPorCliente(sesion.idCliente());
    }

    // El POS valida el saldo antes de cobrar con crédito.
    @GetMapping("/codigo/{codigo}")
    public Credito porCodigo(@PathVariable String codigo) {
        return creditoService.buscarPorCodigo(codigo);
    }

    @PostMapping("/canjear")
    public Credito canjear(@Valid @RequestBody CanjeCreditoDTO request) {
        return creditoService.canjear(request);
    }

    @PostMapping("/reembolsar")
    public Credito reembolsar(@Valid @RequestBody ReembolsoCreditoDTO request) {
        return creditoService.reembolsar(request);
    }
}
