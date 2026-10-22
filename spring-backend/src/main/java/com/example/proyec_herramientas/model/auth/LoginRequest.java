package com.example.proyec_herramientas.model.auth;

import jakarta.validation.constraints.NotBlank;

// "usuario" acepta correo o nombre de usuario (ADMIN) o correo (CLIENTE).
public class LoginRequest {

    @NotBlank(message = "es obligatorio")
    private String usuario;

    @NotBlank(message = "es obligatoria")
    private String contrasena;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
