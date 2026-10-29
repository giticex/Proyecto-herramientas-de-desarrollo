package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Listar por tienda
    List<Producto> findByIdTienda(Integer idTienda);

    long countByIdTienda(Integer idTienda);

    long countByIdTiendaAndStockLessThanEqual(Integer idTienda, int stock);

    Optional<Producto> findByCodigo(String codigo);

    // Bloquea la fila del producto mientras dura la transaccion de venta: dos
    // cajas que venden el ultimo stock a la vez deben serializarse, no pisarse.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Integer id);
}