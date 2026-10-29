package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.AuditoriaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    // tipo/texto llegan vacios cuando no se filtra: comparar contra '' evita
    // los problemas de tipado de un parametro NULL dentro de "is null".
    @Query("""
            select e from AuditoriaEvento e
            where (:tipo = '' or e.tipo = :tipo)
              and (:texto = ''
                   or lower(e.descripcion) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(e.actor, '')) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(e.tiendaNombre, '')) like lower(concat('%', :texto, '%')))
            """)
    Page<AuditoriaEvento> buscar(@Param("tipo") String tipo, @Param("texto") String texto,
            Pageable pageable);
}
