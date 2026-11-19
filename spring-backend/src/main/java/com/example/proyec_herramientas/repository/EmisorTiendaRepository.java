package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.EmisorTienda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmisorTiendaRepository extends JpaRepository<EmisorTienda, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmisorTienda e WHERE e.idTienda = :idTienda")
    Optional<EmisorTienda> bloquear(@Param("idTienda") int idTienda);
}
