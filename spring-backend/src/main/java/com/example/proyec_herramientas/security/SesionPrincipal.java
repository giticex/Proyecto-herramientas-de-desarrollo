package com.example.proyec_herramientas.security;

import java.security.Principal;

// Datos de la sesion activa: quien es, que rol tiene, de que cliente es y de
// que tienda es. Se guarda como principal del contexto de seguridad para que
// los controllers puedan consultar la pertenencia sin tocar la base.
//
// idTienda solo lo tienen las cuentas ADMIN y VENDEDOR (la tienda en la que
// trabajan); idCliente solo lo tienen las cuentas CLIENTE.
public record SesionPrincipal(String usuario, String rol, String idCliente, Integer idTienda)
        implements Principal {

    @Override
    public String getName() {
        return usuario;
    }
}
