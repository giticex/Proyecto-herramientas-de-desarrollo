package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // El login acepta "usuario" o el correo de la persona dueña de la cuenta.
    @Query("select u from Usuario u, Persona p where u.idPersona = p.idPersona "
            + "and (u.usuario = :valor or p.correo = :valor)")
    Optional<Usuario> buscarPorUsuarioOCorreo(@Param("valor") String valor);
}
