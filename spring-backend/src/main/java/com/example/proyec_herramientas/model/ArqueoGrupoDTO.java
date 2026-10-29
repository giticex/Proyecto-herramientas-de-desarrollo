package com.example.proyec_herramientas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Ingresos de un grupo de metodos de pago del arqueo. */
public class ArqueoGrupoDTO {

    private String grupo;
    private BigDecimal total = BigDecimal.ZERO;
    private long comprobantes;
    private BigDecimal porcentaje = BigDecimal.ZERO;
    private List<String> metodos = new ArrayList<>();

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

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public List<String> getMetodos() {
        return metodos;
    }

    public void setMetodos(List<String> metodos) {
        this.metodos = metodos;
    }
}
