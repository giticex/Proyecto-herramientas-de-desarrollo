package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.CanjeCreditoDTO;
import com.example.proyec_herramientas.model.ReembolsoCreditoDTO;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.service.CreditoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creditos")
public class CreditoController {

    private final CreditoService creditoService;

    public CreditoController(CreditoService creditoService) {
        this.creditoService = creditoService;
    }

    @GetMapping
    public List<Credito> listar() {
        return creditoService.listar();
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
