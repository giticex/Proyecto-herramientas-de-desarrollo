package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByIdTienda(int idTienda);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Producto p WHERE p.idTienda = :tienda AND p.idProducto IN :productos
            ORDER BY p.idProducto
            """)
    List<Producto> bloquearParaVenta(@Param("tienda") int idTienda, @Param("productos") Collection<Integer> ids);
}
