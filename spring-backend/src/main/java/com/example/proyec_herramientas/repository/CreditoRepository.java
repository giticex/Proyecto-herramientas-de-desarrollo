package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Credito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditoRepository extends JpaRepository<Credito, String> {

    Optional<Credito> findByCodigo(String codigo);

    // Bloquea la fila para que dos canjes simultaneos del mismo credito no
    // sobregiren el saldo disponible.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credito c WHERE c.codigo = :codigo")
    Optional<Credito> findByCodigoForUpdate(@Param("codigo") String codigo);

    // El sufijo es fijo (6 digitos con ceros a la izquierda), asi el orden
    // lexicografico del codigo coincide con el orden numerico.
    Optional<Credito> findTopByCodigoStartingWithOrderByCodigoDesc(String prefijo);

    List<Credito> findAllByOrderByFechaDesc();
}
