package com.example.proyec_herramientas.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Fondo inicial en efectivo con el que el vendedor abre su turno de caja. */
public class AperturaCajaRequest {

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "admite hasta 2 decimales")
    private BigDecimal montoApertura;

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }
}
