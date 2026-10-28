package com.example.proyec_herramientas.model;

import jakarta.validation.constraints.NotBlank;

// Credenciales de POST /auth/login. "usuario" admite el nombre de cuenta del
// panel (admin) o el correo del comprador (luis@gmail.com).
public class LoginRequestDTO {

    @NotBlank
    private String usuario;

    @NotBlank
    private String clave;

    // Opcional: "CLIENTE" valida solo la tabla de compradores y "PERSONAL"
    // solo las cuentas del panel. Vacio mantiene el orden historico.
    private String tipo;

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String usuario, String clave) {
        this.usuario = usuario;
        this.clave = clave;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
