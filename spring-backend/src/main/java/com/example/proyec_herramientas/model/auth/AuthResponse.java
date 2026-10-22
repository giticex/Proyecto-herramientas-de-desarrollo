package com.example.proyec_herramientas.model.auth;

import com.example.proyec_herramientas.model.TiendaDTO;

public class AuthResponse {

    private String token;
    private UsuarioAuthDTO usuario;
    // null si el rol es CLIENTE.
    private TiendaDTO tienda;

    public AuthResponse() {
    }

    public AuthResponse(String token, UsuarioAuthDTO usuario, TiendaDTO tienda) {
        this.token = token;
        this.usuario = usuario;
        this.tienda = tienda;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UsuarioAuthDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioAuthDTO usuario) {
        this.usuario = usuario;
    }

    public TiendaDTO getTienda() {
        return tienda;
    }

    public void setTienda(TiendaDTO tienda) {
        this.tienda = tienda;
    }
}
