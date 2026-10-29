package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Una persona con ventas emitidas no puede borrarse: el comprobante
    // historico la referencia.
    long countByIdPersona(String idPersona);

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

    // Total historico y cantidad de comprobantes de la tienda. La consola del
    // SUPER_ADMIN los usa para el estado de cada empresa.
    @Query("""
            SELECT COALESCE(SUM(v.totalVenta), 0)
            FROM Venta v
            WHERE EXISTS (
                SELECT 1
                FROM DetalleVenta d, Producto p
                WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
            )
            """)
    BigDecimal sumarTotalPorTienda(@Param("idTienda") int idTienda);

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE EXISTS (
                SELECT 1
                FROM DetalleVenta d, Producto p
                WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
            )
            """)
    long contarVentasPorTienda(@Param("idTienda") int idTienda);

    @Query("""
            SELECT MAX(v.fechaEmision)
            FROM Venta v
            WHERE EXISTS (
                SELECT 1
                FROM DetalleVenta d, Producto p
                WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
            )
            """)
    Date ultimaVentaPorTienda(@Param("idTienda") int idTienda);

    // Totales de toda la plataforma (sin acotar por tienda): alimentan los KPI
    // globales de la consola del SUPER_ADMIN.
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v WHERE v.fechaEmision >= :inicio AND v.fechaEmision < :fin")
    BigDecimal sumarTotalGlobalEntre(@Param("inicio") Date inicio, @Param("fin") Date fin);

    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v")
    BigDecimal sumarTotalGlobal();

    @Query("SELECT COUNT(v) FROM Venta v")
    long contarVentasGlobal();

    // Historial paginado de la tienda. Igual que en el dashboard, la venta se
    // asocia a la tienda por los productos de sus detalles. El filtro busca por
    // numero de comprobante, documento del cliente o razon social.
    @Query("""
            SELECT v
            FROM Venta v
            WHERE EXISTS (
                SELECT 1
                FROM DetalleVenta d, Producto p
                WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
            )
              AND (:q = ''
                   OR LOWER(v.numeroDocumento) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR v.receptorNumDocumento LIKE CONCAT('%', :q, '%')
                   OR LOWER(v.receptorRazonSocial) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY v.fechaEmision DESC
            """)
    Page<Venta> buscarPorTienda(@Param("idTienda") int idTienda,
            @Param("q") String q,
            Pageable pageable);

    // Cantidad de lineas de la venta que pertenecen a productos de la tienda.
    // Sirve para decidir si un ADMIN/VENDEDOR puede tocar el comprobante.
    @Query("""
            SELECT COUNT(d)
            FROM DetalleVenta d
            WHERE d.venta.idVenta = :idVenta
              AND EXISTS (
                  SELECT 1 FROM Producto p
                  WHERE p.idProducto = d.idProducto AND p.idTienda = :idTienda
              )
            """)
    long contarDetallesDeTienda(@Param("idVenta") String idVenta, @Param("idTienda") int idTienda);

    // Siguiente correlativo de la serie. Se calcula sobre el maximo ya emitido,
    // no sobre el total de ventas: si una serie arranca en otro numero o hay
    // huecos, contar filas reutiliza un numero ya usado.
    @Query("SELECT COALESCE(MAX(v.correlativo), 0) FROM Venta v WHERE v.serie = :serie")
    int maxCorrelativoPorSerie(@Param("serie") String serie);

    // Historial de la tienda (opcionalmente filtrado por cliente). La venta se
    // asocia a la tienda por los productos de sus detalles.
    @Query("""
            SELECT v
            FROM Venta v
            WHERE EXISTS (
                SELECT 1
                FROM DetalleVenta d, Producto p
                WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
            )
              AND (:idCliente = '' OR v.idCliente = :idCliente)
            ORDER BY v.fechaEmision DESC
            """)
    List<Venta> historialPorTienda(@Param("idTienda") int idTienda,
            @Param("idCliente") String idCliente);

    // Boletas (o cualquier tipo de documento) de la tienda.
    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.idTipoDocumento = :idTipoDocumento
              AND EXISTS (
                  SELECT 1
                  FROM DetalleVenta d, Producto p
                  WHERE d.venta = v AND p.idProducto = d.idProducto AND p.idTienda = :idTienda
              )
            ORDER BY v.fechaEmision DESC
            """)
    List<Venta> boletasPorTienda(@Param("idTipoDocumento") String idTipoDocumento,
            @Param("idTienda") int idTienda);
}
