package com.example.proyec_herramientas.config;

// Principal que el JwtAuthenticationFilter deja en el SecurityContext.
// idTienda es null para CLIENTE (no esta atado a una sola tienda).
public class UsuarioAutenticado {

    private final String id;
    private final String rol;
    private final Integer idTienda;

    public UsuarioAutenticado(String id, String rol, Integer idTienda) {
        this.id = id;
        this.rol = rol;
        this.idTienda = idTienda;
    }

    public String getId() {
        return id;
    }

    public String getRol() {
        return rol;
    }

    public Integer getIdTienda() {
        return idTienda;
    }
}
