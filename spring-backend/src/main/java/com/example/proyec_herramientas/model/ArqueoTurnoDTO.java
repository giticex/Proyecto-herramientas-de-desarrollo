package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/** Ingresos de una franja horaria del dia. */
public class ArqueoTurnoDTO {

    private String turno;
    private String rango;
    private BigDecimal total = BigDecimal.ZERO;
    private long comprobantes;

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getRango() {
        return rango;
    }

    public void setRango(String rango) {
        this.rango = rango;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public long getComprobantes() {
        return comprobantes;
    }

    public void setComprobantes(long comprobantes) {
        this.comprobantes = comprobantes;
    }
}
