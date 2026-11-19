package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Correlativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrelativoRepository extends JpaRepository<Correlativo, String> {
    @Modifying
    @Query(value = """
            INSERT INTO correlativo_emisor (ruc_emisor, id_tipo_documento, serie, ultimo_numero)
            VALUES (:ruc, :tipo, :serie, :numeroInicial)
            ON DUPLICATE KEY UPDATE ultimo_numero = ultimo_numero + 1
            """, nativeQuery = true)
    void reservar(@Param("ruc") String ruc, @Param("tipo") String tipo,
            @Param("serie") String serie, @Param("numeroInicial") int numeroInicial);

    @Query(value = """
            SELECT ultimo_numero FROM correlativo_emisor
            WHERE ruc_emisor = :ruc AND id_tipo_documento = :tipo AND serie = :serie
            """, nativeQuery = true)
    int ultimoNumero(@Param("ruc") String ruc, @Param("tipo") String tipo, @Param("serie") String serie);
}
