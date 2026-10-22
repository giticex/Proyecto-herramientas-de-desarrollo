package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, String> {

    Optional<Parametro> findByIdParametro(String idParametro);
}
