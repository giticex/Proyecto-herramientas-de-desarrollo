package com.example.proyec_herramientas.model;

import java.util.HashMap;
import java.util.Map;

public class ProductoDTO {

    private int idProducto;
    private String nombre;
    private int stock;
    private double precio;
    private String categoria;

    // Atributos libres por rubro (ej. talla, color) que usa el catalogo para filtros dinamicos.
    private Map<String, String> atributos = new HashMap<>();

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public ProductoDTO() {
    }

    public ProductoDTO(int idProducto, String nombre, double precio, int stock) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Map<String, String> getAtributos() {
        return atributos;
    }

    public void setAtributos(Map<String, String> atributos) {
        this.atributos = atributos != null ? atributos : new HashMap<>();
    }
}