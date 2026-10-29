package com.example.proyec_herramientas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Corte de caja y arqueo de la tienda para un dia o un mes: totales por metodo
 * de pago, por vendedor y por turno, mas el detalle de comprobantes que los
 * respaldan.
 */
public class ArqueoDTO {

    private String tiendaNombre;
    private String tiendaRuc;
    private String periodo;
    private String desde;
    private String hasta;
    private String generadoEn;
    private BigDecimal totalVentas = BigDecimal.ZERO;
    private long comprobantes;
    private BigDecimal ticketPromedio = BigDecimal.ZERO;
    private List<ArqueoGrupoDTO> metodosPago = new ArrayList<>();
    private List<ArqueoVendedorDTO> vendedores = new ArrayList<>();
    private List<ArqueoTurnoDTO> turnos = new ArrayList<>();
    private List<ArqueoComprobanteDTO> detalle = new ArrayList<>();

    public String getTiendaNombre() {
        return tiendaNombre;
    }

    public void setTiendaNombre(String tiendaNombre) {
        this.tiendaNombre = tiendaNombre;
    }

    public String getTiendaRuc() {
        return tiendaRuc;
    }

    public void setTiendaRuc(String tiendaRuc) {
        this.tiendaRuc = tiendaRuc;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public String getGeneradoEn() {
        return generadoEn;
    }

    public void setGeneradoEn(String generadoEn) {
        this.generadoEn = generadoEn;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public long getComprobantes() {
        return comprobantes;
    }

    public void setComprobantes(long comprobantes) {
        this.comprobantes = comprobantes;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public List<ArqueoGrupoDTO> getMetodosPago() {
        return metodosPago;
    }

    public void setMetodosPago(List<ArqueoGrupoDTO> metodosPago) {
        this.metodosPago = metodosPago;
    }

    public List<ArqueoVendedorDTO> getVendedores() {
        return vendedores;
    }

    public void setVendedores(List<ArqueoVendedorDTO> vendedores) {
        this.vendedores = vendedores;
    }

    public List<ArqueoTurnoDTO> getTurnos() {
        return turnos;
    }

    public void setTurnos(List<ArqueoTurnoDTO> turnos) {
        this.turnos = turnos;
    }

    public List<ArqueoComprobanteDTO> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<ArqueoComprobanteDTO> detalle) {
        this.detalle = detalle;
    }
}
