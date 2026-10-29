package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaRepository extends JpaRepository<Tienda, Integer> {

    Optional<Tienda> findBySlug(String slug);

    List<Tienda> findAllByOrderByNombreAsc();
}
