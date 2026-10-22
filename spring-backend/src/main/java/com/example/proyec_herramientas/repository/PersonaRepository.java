package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, String> {

    @Query("select p.idPersona from Persona p, Cliente c where c.idCliente = :idCliente and c.correo = p.correo")
    Optional<String> findIdPersonaByClienteId(@Param("idCliente") String idCliente);

    Optional<Persona> findByCorreo(String correo);
}
