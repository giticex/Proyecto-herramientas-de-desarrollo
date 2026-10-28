package com.example.proyec_herramientas.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;

/**
 * Item (linea) de un comprobante de pago electronico (tabla detalle_venta).
 *
 * <p>Diferencia clave entre valor unitario y precio unitario:</p>
 * <ul>
 *   <li><b>Valor unitario</b>: importe de una unidad SIN IGV. Es la base
 *       imponible que SUNAT usa para calcular el IGV del item.</li>
 *   <li><b>Precio unitario</b>: importe de una unidad CON IGV. Es el precio
 *       de venta que ve el cliente en el catalogo.</li>
 * </ul>
 */
@Entity
@Table(name = "detalle_venta")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetalleVenta {

    /** Afectacion del IGV (catalogo 07 SUNAT). */
    public static final String AFECTACION_GRAVADA = "01";
    public static final String AFECTACION_INAFECTA = "02";
    public static final String AFECTACION_EXONERADA = "03";
    public static final String AFECTACION_GRATUITA = "04";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @Column(name = "id_producto", nullable = false)
    private int idProducto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    /** Precio de venta unitario CON IGV. */
    @Column(name = "precio_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    /** Monto total del item: base imponible + IGV del item. */
    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Valor unitario SIN IGV (base imponible unitaria). */
    @Column(name = "valor_unitario", precision = 12, scale = 6, nullable = false)
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    /** IGV del item. */
    @Column(name = "igv_item", precision = 12, scale = 2, nullable = false)
    private BigDecimal igvItem = BigDecimal.ZERO;

    /** Descuento del item. */
    @Column(name = "descuento", precision = 12, scale = 2, nullable = false)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** Codigo del item segun catalogo del emisor. */
    @Column(name = "codigo_item", length = 50, nullable = false)
    private String codigoItem = "";

    /** Descripcion del item (nombre del producto). */
    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion = "";

    /** Unidad de medida (catalogo 03 SUNAT): NIU, UN, ZZ, KG... */
    @Column(name = "unidad_medida", length = 5, nullable = false)
    private String unidadMedida = "NIU";

    /** Afectacion del IGV (catalogo 07 SUNAT). */
    @Column(name = "afectacion_igv", length = 2, nullable = false)
    private String afectacionIgv = AFECTACION_GRAVADA;

    @Transient
    private String producto;

    // Unidades ya devueltas de esta linea (se rellena solo al consultar una
    // venta para devolucion; no se persiste).
    @Transient
    private int cantidadDevuelta = 0;

    public DetalleVenta() {
    }

    public DetalleVenta(int idProducto, String producto, int cantidad, BigDecimal precioUnitario) {
        this.idProducto = idProducto;
        this.producto = producto;
        this.descripcion = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(int cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getIgvItem() {
        return igvItem;
    }

    public void setIgvItem(BigDecimal igvItem) {
        this.igvItem = igvItem;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public String getCodigoItem() {
        return codigoItem;
    }

    public void setCodigoItem(String codigoItem) {
        this.codigoItem = codigoItem;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getAfectacionIgv() {
        return afectacionIgv;
    }

    public void setAfectacionIgv(String afectacionIgv) {
        this.afectacionIgv = afectacionIgv;
    }

    /** El item no paga IGV cuando es inafecto, exonerado o gratuito. */
    public boolean esGravado() {
        return AFECTACION_GRAVADA.equals(afectacionIgv);
    }
}
