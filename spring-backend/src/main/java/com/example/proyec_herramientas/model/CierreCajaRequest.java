package com.example.proyec_herramientas.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Arqueo fisico del cajon con el que se cierra el turno. */
public class CierreCajaRequest {

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "admite hasta 2 decimales")
    private BigDecimal efectivoContado;

    @Size(max = 200, message = "no puede superar 200 caracteres")
    private String observacion;

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(BigDecimal efectivoContado) {
        this.efectivoContado = efectivoContado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
