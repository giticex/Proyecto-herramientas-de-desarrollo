package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.CanjeCreditoDTO;
import com.example.proyec_herramientas.model.ReembolsoCreditoDTO;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.persistence.Devolucion;
import com.example.proyec_herramientas.persistence.Venta;

import java.math.BigDecimal;
import java.util.List;

public interface CreditoService {

    List<Credito> listar();

    Credito buscarPorCodigo(String codigo);

    // Genera el crédito a favor de una devolución (estado Vigente). Se ejecuta
    // dentro de la transacción de la devolución para que ambos se guardan o
    // ninguno.
    Credito generarParaDevolucion(Devolucion devolucion, Venta venta);

    // Canje manual desde la gestión de créditos.
    Credito canjear(CanjeCreditoDTO request);

    Credito reembolsar(ReembolsoCreditoDTO request);

    // Aplica el crédito al total de una venta: consume el menor entre el saldo
    // disponible y el total, y devuelve cuánto se usó y cuánto quedó.
    ResultadoUsoCredito aplicarEnVenta(String codigo, BigDecimal totalVenta);

    record ResultadoUsoCredito(String codigo, BigDecimal usado, BigDecimal saldoRestante) {
    }
}
