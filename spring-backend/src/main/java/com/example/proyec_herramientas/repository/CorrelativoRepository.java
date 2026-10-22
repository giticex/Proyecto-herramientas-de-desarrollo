package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Correlativo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CorrelativoRepository extends JpaRepository<Correlativo, String> {

    /**
     * Bloquea la fila de la serie para que dos ventas simultaneas no obtengan
     * el mismo correlativo. El bloqueo se mantiene hasta el commit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Correlativo c where c.serie = :serie")
    Optional<Correlativo> findBySerieForUpdate(@Param("serie") String serie);
}
