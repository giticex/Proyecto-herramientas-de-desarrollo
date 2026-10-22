package com.example.proyec_herramientas.facturacion.model;

import java.math.BigDecimal;

/**
 * Totales del comprobante (LegalMonetaryTotal de UBL 2.1).
 *
 * <p>Estructura exigida por SUNAT:</p>
 * <ul>
 *   <li><b>opGravada</b>: operaciones gravadas (base imponible, sin IGV)</li>
 *   <li><b>opInafecta</b>: operaciones inafectas</li>
 *   <li><b>opExonerada</b>: operaciones exoneradas</li>
 *   <li><b>opGratuita</b>: operaciones gratuitas (valor referencial)</li>
 *   <li><b>igv</b>: total del IGV</li>
 *   <li><b>totalVenta</b>: monto total de la venta (base + IGV, incluye
 *       operaciones gratuitas)</li>
 *   <li><b>montoTotalPagar</b>: monto a pagar (total venta - descuentos +
 *       otros cargos)</li>
 * </ul>
 */
public class TotalesDTO {

    private BigDecimal opGravada = BigDecimal.ZERO;
    private BigDecimal opInafecta = BigDecimal.ZERO;
    private BigDecimal opExonerada = BigDecimal.ZERO;
    private BigDecimal opGratuita = BigDecimal.ZERO;
    private BigDecimal igv = BigDecimal.ZERO;
    private BigDecimal totalDescuentos = BigDecimal.ZERO;
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;
    private BigDecimal totalVenta = BigDecimal.ZERO;
    private BigDecimal montoTotalPagar = BigDecimal.ZERO;
    private BigDecimal montoTotalCredito = BigDecimal.ZERO;
    private String totalEnLetras;

    public BigDecimal getOpGravada() {
        return opGravada;
    }

    public void setOpGravada(BigDecimal opGravada) {
        this.opGravada = opGravada;
    }

    public BigDecimal getOpInafecta() {
        return opInafecta;
    }

    public void setOpInafecta(BigDecimal opInafecta) {
        this.opInafecta = opInafecta;
    }

    public BigDecimal getOpExonerada() {
        return opExonerada;
    }

    public void setOpExonerada(BigDecimal opExonerada) {
        this.opExonerada = opExonerada;
    }

    public BigDecimal getOpGratuita() {
        return opGratuita;
    }

    public void setOpGratuita(BigDecimal opGratuita) {
        this.opGratuita = opGratuita;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getTotalOtrosCargos() {
        return totalOtrosCargos;
    }

    public void setTotalOtrosCargos(BigDecimal totalOtrosCargos) {
        this.totalOtrosCargos = totalOtrosCargos;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta;
    }

    public BigDecimal getMontoTotalPagar() {
        return montoTotalPagar;
    }

    public void setMontoTotalPagar(BigDecimal montoTotalPagar) {
        this.montoTotalPagar = montoTotalPagar;
    }

    public BigDecimal getMontoTotalCredito() {
        return montoTotalCredito;
    }

    public void setMontoTotalCredito(BigDecimal montoTotalCredito) {
        this.montoTotalCredito = montoTotalCredito;
    }

    public String getTotalEnLetras() {
        return totalEnLetras;
    }

    public void setTotalEnLetras(String totalEnLetras) {
        this.totalEnLetras = totalEnLetras;
    }
}
