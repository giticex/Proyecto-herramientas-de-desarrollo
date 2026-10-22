package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "precio", nullable = false)
    private double precio;

    @Column(name = "categoria", length = 80, nullable = false)
    private String categoria;

    // Atributos libres por rubro (ej. talla, color) que usa el catalogo para filtros dinamicos.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos", columnDefinition = "json")
    private Map<String, String> atributos = new HashMap<>();

    // La tienda dueña del producto. Nunca viaja en ProductoDTO: sale siempre
    // del token del ADMIN autenticado.
    @Column(name = "id_tienda", nullable = false)
    private Integer idTienda;

    public Producto() {
    }

    public Producto(Integer idProducto, String nombre, int stock, double precio, String categoria, Integer idTienda) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.categoria = categoria;
        this.idTienda = idTienda;
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

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Map<String, String> getAtributos() {
        return atributos;
    }

    public void setAtributos(Map<String, String> atributos) {
        this.atributos = atributos != null ? atributos : new HashMap<>();
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }
}
