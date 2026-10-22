package com.example.proyec_herramientas.model;

import java.util.HashMap;
import java.util.Map;

public class ProductoDTO {

    private int idProducto;
    private String nombre;
    private int stock;
    private double precio;
    private String categoria;

    /** Codigo del item en el catalogo del emisor (se envia al CDR de SUNAT). */
    private String codigo = "";

    /** Unidad de medida (catalogo 03 SUNAT): NIU, UN, ZZ, KG... */
    private String unidadMedida = "NIU";

    /** Afectacion del IGV (catalogo 07 SUNAT): 01 gravada, 02 inafecta, 03 exonerada, 04 gratuita. */
    private String afectacionIgv = "01";

    // Atributos libres por rubro (ej. talla, color) que usa el catalogo para filtros dinamicos.
    private Map<String, String> atributos = new HashMap<>();

    /** Tienda duena del producto. Solo lectura: lo pone el backend desde el token. */
    private Integer idTienda;

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

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }
}