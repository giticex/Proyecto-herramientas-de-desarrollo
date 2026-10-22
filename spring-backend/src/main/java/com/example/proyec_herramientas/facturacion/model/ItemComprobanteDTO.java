package com.example.proyec_herramientas.facturacion.model;

import java.math.BigDecimal;

/**
 * Item del comprobante segun la estructura de SUNAT UBL 2.1 (InvoiceLine).
 *
 * <p>Diferencia entre valor unitario y precio unitario:</p>
 * <ul>
 *   <li><b>valorUnitario</b>: importe de una unidad SIN IGV (base imponible
 *       unitaria). Es el valor que SUNAT usa para calcular el IGV del item.</li>
 *   <li><b>precioUnitario</b>: importe de una unidad CON IGV (precio de venta
 *       que se muestra en el catalogo y en la representacion impresa).</li>
 * </ul>
 *
 * <p>El backend recalcula estos importes a partir del catalogo de productos:
 * el precio que envia el frontend nunca es el precio que se factura.</p>
 */
public class ItemComprobanteDTO {

    /** Afectacion del IGV (catalogo 07 SUNAT). */
    public static final String AFECTACION_GRAVADA = "01";
    public static final String AFECTACION_INAFECTA = "02";
    public static final String AFECTACION_EXONERADA = "03";
    public static final String AFECTACION_GRATUITA = "04";

    private Integer idProducto;
    private String codigo;
    private String descripcion;
    private BigDecimal cantidad = BigDecimal.ONE;
    private String unidadMedida = "NIU";
    private BigDecimal valorUnitario;
    private BigDecimal precioUnitario;
    private String afectacionIgv = AFECTACION_GRAVADA;
    private BigDecimal descuento = BigDecimal.ZERO;

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

    public String getAfectacionIgv() {
        return afectacionIgv;
    }

    public void setAfectacionIgv(String afectacionIgv) {
        this.afectacionIgv = afectacionIgv;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }
}
