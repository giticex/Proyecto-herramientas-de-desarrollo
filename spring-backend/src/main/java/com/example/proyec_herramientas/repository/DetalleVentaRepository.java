package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.model.ProductoVendidoDTO;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {

    // Ranking de los productos mas vendidos de la tienda (todas las ventas).
    // El detalle no conoce la tienda directamente: se resuelve por el producto,
    // que es el que tiene id_tienda.
    @Query("""
            SELECT new com.example.proyec_herramientas.model.ProductoVendidoDTO(
                p.idProducto, p.nombre, SUM(d.cantidad), SUM(d.subtotal))
            FROM DetalleVenta d, Producto p
            WHERE p.idProducto = d.idProducto AND p.idTienda = :idTienda
            GROUP BY p.idProducto, p.nombre
            ORDER BY SUM(d.cantidad) DESC, SUM(d.subtotal) DESC
            """)
    List<ProductoVendidoDTO> topProductosPorTienda(@Param("idTienda") int idTienda, Pageable pageable);
}
