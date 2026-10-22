package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, String> {

    List<Venta> findByIdTipoDocumentoOrderByFechaEmisionDesc(String idTipoDocumento);

    List<Venta> findByIdClienteOrderByFechaEmisionDesc(String idCliente);

    List<Venta> findAllByOrderByFechaEmisionDesc();

    // Suma de los totales de las ventas de la tienda en el rango [inicio, fin).
    // La venta no guarda id_tienda: la pertenencia se resuelve por los productos
    // de sus detalles. Se usa EXISTS para no duplicar la venta si tiene varias
    // lineas de la misma tienda.
    @Query("""
            SELECT COALESCE(SUM(v.totalVenta), 0)
            FROM Venta v
            WHERE v.fechaEmision >= :inicio AND v.fechaEmision < :fin
              AND EXISTS (
                  SELECT 1
                  FROM DetalleVenta d, Producto p
                  WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
              )
            """)
    BigDecimal sumarTotalPorTiendaEntre(@Param("inicio") Date inicio,
            @Param("fin") Date fin,
            @Param("idTienda") int idTienda);
}
