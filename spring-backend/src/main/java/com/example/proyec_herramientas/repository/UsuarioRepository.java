package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsuario(String usuario);

    Optional<Usuario> findByIdPersona(String idPersona);
}
