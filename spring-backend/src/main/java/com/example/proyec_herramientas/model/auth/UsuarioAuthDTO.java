package com.example.proyec_herramientas.model.auth;

public class UsuarioAuthDTO {

    private String id;
    private String correo;
    private String rol;

    public UsuarioAuthDTO() {
    }

    public UsuarioAuthDTO(String id, String correo, String rol) {
        this.id = id;
        this.correo = correo;
        this.rol = rol;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
