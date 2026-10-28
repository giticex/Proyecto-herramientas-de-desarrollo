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

import java.math.BigDecimal;

/**
 * Linea devuelta de una devolucion (tabla devolucion_detalle).
 *
 * <p>Guarda el precio unitario (con IGV) y la descripcion congelados del
 * detalle de la venta original.</p>
 */
@Entity
@Table(name = "devolucion_detalle")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevolucionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_devolucion", nullable = false)
    private Devolucion devolucion;

    @Column(name = "id_producto", nullable = false)
    private int idProducto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    /** Precio de venta unitario CON IGV (congelado del detalle original). */
    @Column(name = "precio_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    /** Monto total de la linea: precio unitario por cantidad. */
    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Descripcion del producto (congelada del detalle original). */
    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion = "";

    public DevolucionDetalle() {
    }

    public DevolucionDetalle(int idProducto, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this.idProducto = idProducto;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public void setDevolucion(Devolucion devolucion) {
        this.devolucion = devolucion;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
