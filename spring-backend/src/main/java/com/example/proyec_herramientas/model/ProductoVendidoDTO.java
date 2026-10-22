package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

// Fila del ranking de productos vendidos que consume el dashboard.
// cantidad = unidades vendidas; total = importe vendido (subtotal de los items).
public record ProductoVendidoDTO(Integer idProducto, String nombre, Long cantidad, BigDecimal total) {
}
