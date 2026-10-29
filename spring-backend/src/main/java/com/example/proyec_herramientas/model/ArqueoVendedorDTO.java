package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/** Total facturado por una cuenta del panel (o por las ventas sin atribuir). */
public class ArqueoVendedorDTO {

    private String usuario;
    private String nombre;
    private BigDecimal total = BigDecimal.ZERO;
    private long comprobantes;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
