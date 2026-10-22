package com.example.proyec_herramientas.facturacion.model;

import java.math.BigDecimal;

/**
 * Linea de detalle ya calculada por el backend, lista para pintar en la
 * representacion impresa y para el XML UBL 2.1.
 */
public class DetalleComprobanteDTO {

    private int numero;
    private Integer idProducto;
    private String codigo;
    private String descripcion;
    private BigDecimal cantidad = BigDecimal.ZERO;
    private String unidadMedida;
    /** Valor unitario sin IGV. */
    private BigDecimal valorUnitario = BigDecimal.ZERO;
    /** Precio unitario con IGV. */
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    private BigDecimal descuento = BigDecimal.ZERO;
    private String afectacionIgv;
    /** Base imponible de la linea (sin IGV). */
    private BigDecimal base = BigDecimal.ZERO;
    /** IGV de la linea. */
    private BigDecimal igv = BigDecimal.ZERO;
    /** Monto total de la linea (base + IGV). */
    private BigDecimal totalItem = BigDecimal.ZERO;

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public String getAfectacionIgv() {
        return afectacionIgv;
    }

    public void setAfectacionIgv(String afectacionIgv) {
        this.afectacionIgv = afectacionIgv;
    }

    public BigDecimal getBase() {
        return base;
    }

    public void setBase(BigDecimal base) {
        this.base = base;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotalItem() {
        return totalItem;
    }

    public void setTotalItem(BigDecimal totalItem) {
        this.totalItem = totalItem;
    }
}
