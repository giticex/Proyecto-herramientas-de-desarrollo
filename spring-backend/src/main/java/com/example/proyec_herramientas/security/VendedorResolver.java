package com.example.proyec_herramientas.security;

import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Resuelve el vendedor de una venta a partir del token. Solo el personal de la
 * tienda (ADMIN y VENDEDOR) se atribuye: la autocompra de un CLIENTE no tiene
 * vendedor. La cuenta se valida contra la tienda del token para que un token
 * manipulado no atribuya ventas a personal de otra empresa.
 */
@Service
public class VendedorResolver {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;

    public VendedorResolver(UsuarioRepository usuarioRepository, PersonaRepository personaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
    }

    public Vendedor resolver(SesionPrincipal sesion) {
        if (sesion == null || sesion.idTienda() == null) {
            return null;
        }
        if (!Rol.ADMIN.equals(sesion.rol()) && !Rol.VENDEDOR.equals(sesion.rol())) {
            return null;
        }
        String cuenta = sesion.usuario();
        if (cuenta == null || cuenta.isBlank()) {
            return null;
        }
        return usuarioRepository.findByUsuario(cuenta)
                .filter(usuario -> Objects.equals(usuario.getIdTienda(), sesion.idTienda()))
                .map(usuario -> new Vendedor(usuario.getUsuario(), nombreDe(usuario)))
                .orElse(null);
    }

    private String nombreDe(Usuario usuario) {
        return personaRepository.findById(usuario.getIdPersona())
                .map(this::nombreCompleto)
                .filter(nombre -> !nombre.isBlank())
                .orElse(usuario.getUsuario());
    }

    private String nombreCompleto(Persona persona) {
        String nombre = persona.getNombre() == null ? "" : persona.getNombre().trim();
        String apellido = persona.getApellido() == null ? "" : persona.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }
}
