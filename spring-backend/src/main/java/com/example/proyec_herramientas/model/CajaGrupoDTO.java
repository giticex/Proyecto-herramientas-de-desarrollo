package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/** Ingresos del turno de caja agrupados por medio de pago. */
public class CajaGrupoDTO {

    private String grupo;
    private BigDecimal total = BigDecimal.ZERO;
    private long comprobantes;

    public CajaGrupoDTO() {
    }

    public CajaGrupoDTO(String grupo, BigDecimal total, long comprobantes) {
        this.grupo = grupo;
        this.total = total;
        this.comprobantes = comprobantes;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
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
