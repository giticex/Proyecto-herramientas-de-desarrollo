package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "producto")
public class Producto {

    /** Unidad de medida por defecto para bienes (catalogo 03 SUNAT). */
    public static final String UNIDAD_NIU = "NIU";

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "stock", nullable = false)
    private int stock;

    /** Precio de venta unitario CON IGV. */
    @Column(name = "precio", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "categoria", length = 80, nullable = false)
    private String categoria;

    /** Codigo del item en el catalogo del emisor (se envia al CDR de SUNAT). */
    @Column(name = "codigo", length = 50, nullable = false)
    private String codigo = "";

    /** Unidad de medida del item (catalogo 03 SUNAT). */
    @Column(name = "unidad_medida", length = 5, nullable = false)
    private String unidadMedida = UNIDAD_NIU;

    /** Afectacion del IGV del item (catalogo 07 SUNAT). */
    @Column(name = "afectacion_igv", length = 2, nullable = false)
    private String afectacionIgv = DetalleVenta.AFECTACION_GRAVADA;

    // Atributos libres por rubro (ej. talla, color) que usa el catalogo para filtros dinamicos.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos", columnDefinition = "json")
    private Map<String, String> atributos = new HashMap<>();

    public Producto() {
    }

    public Producto(Integer idProducto, String nombre, int stock, BigDecimal precio, String categoria) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.categoria = categoria;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

    public Map<String, String> getAtributos() {
        return atributos;
    }

    public void setAtributos(Map<String, String> atributos) {
        this.atributos = atributos != null ? atributos : new HashMap<>();
    }
}
