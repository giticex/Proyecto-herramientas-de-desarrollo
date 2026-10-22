package com.example.proyec_herramientas.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroTiendaRequest {

    @NotBlank(message = "es obligatorio")
    private String nombre;

    @NotBlank(message = "es obligatorio")
    private String rubro;

    @NotBlank(message = "es obligatorio")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "debe usar minúsculas y guiones")
    private String slug;

    @NotBlank(message = "es obligatorio")
    @Email(message = "debe ser un correo válido")
    private String correo;

    @NotBlank(message = "es obligatoria")
    @Size(min = 8, message = "debe tener al menos 8 caracteres")
    private String contrasena;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
