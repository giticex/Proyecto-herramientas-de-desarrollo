package com.example.proyec_herramientas.security;

import java.security.Principal;

// Datos de la sesion activa: quien es, que rol tiene y de que cliente es.
// Se guarda como principal del contexto de seguridad para que los
// controllers puedan consultar el rol y la pertenencia sin tocar la base.
public record SesionPrincipal(String usuario, String rol, String idCliente) implements Principal {

    @Override
    public String getName() {
        return usuario;
    }
}
