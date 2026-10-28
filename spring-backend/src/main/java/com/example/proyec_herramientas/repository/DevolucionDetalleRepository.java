package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.DevolucionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DevolucionDetalleRepository extends JpaRepository<DevolucionDetalle, Integer> {

    // Unidades ya devueltas por producto en una venta (suma de todas sus
    // devoluciones). Cada fila es [idProducto, sumaDeCantidades].
    @Query("""
            SELECT d.idProducto, SUM(d.cantidad)
            FROM DevolucionDetalle d
            WHERE d.devolucion.idVenta = :idVenta
            GROUP BY d.idProducto
            """)
    List<Object[]> devueltosPorVenta(@Param("idVenta") String idVenta);
}
