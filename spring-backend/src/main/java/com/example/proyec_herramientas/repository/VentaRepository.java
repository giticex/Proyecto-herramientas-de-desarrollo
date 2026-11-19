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

    List<Venta> findByIdTipoDocumentoAndIdTiendaOrderByFechaEmisionDesc(String idTipoDocumento, int idTienda);

    List<Venta> findByIdClienteOrderByFechaEmisionDesc(String idCliente);

    List<Venta> findByIdTiendaOrderByFechaEmisionDesc(int idTienda);
    List<Venta> findByIdClienteAndIdTiendaOrderByFechaEmisionDesc(String idCliente, int idTienda);

    boolean existsByIdVentaAndIdTienda(String idVenta, int idTienda);
    boolean existsByIdVentaAndIdCliente(String idVenta, String idCliente);

    @Query("""
            SELECT COALESCE(MAX(v.correlativo), 0) FROM Venta v
            WHERE v.rucEmisor IS NULL AND v.idTipoDocumento = :tipo AND v.serie = :serie
            """)
    int ultimoCorrelativoLegado(@Param("tipo") String tipo, @Param("serie") String serie);

    @Query("""
            SELECT COALESCE(SUM(v.totalVenta), 0)
            FROM Venta v
            WHERE v.fechaEmision >= :inicio AND v.fechaEmision < :fin
              AND v.idTienda = :idTienda
            """)
    BigDecimal sumarTotalPorTiendaEntre(@Param("inicio") Date inicio,
            @Param("fin") Date fin,
            @Param("idTienda") int idTienda);

    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.idTienda = :idTienda
              AND (:q = ''
                   OR LOWER(v.numeroDocumento) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR v.receptorNumDocumento LIKE CONCAT('%', :q, '%')
                   OR LOWER(v.receptorRazonSocial) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY v.fechaEmision DESC
            """)
    Page<Venta> buscarPorTienda(@Param("idTienda") int idTienda,
            @Param("q") String q,
            Pageable pageable);
}
