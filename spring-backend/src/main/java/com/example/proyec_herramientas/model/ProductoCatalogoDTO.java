package com.example.proyec_herramientas.model;

import java.util.Map;

public record ProductoCatalogoDTO(
        int idProducto,
        String nombre,
        double precio,
        String categoria,
        String sku,
        Map<String, String> atributos,
        String unidadMedida,
        String afectacionIgv,
        boolean disponible
) {
    public static ProductoCatalogoDTO desde(ProductoDTO producto) {
        return new ProductoCatalogoDTO(
                producto.getIdProducto(), producto.getNombre(), producto.getPrecio(),
                producto.getCategoria(), producto.getSku(), producto.getAtributos(),
                producto.getUnidadMedida(), producto.getAfectacionIgv(), producto.getStock() > 0);
    }
}
