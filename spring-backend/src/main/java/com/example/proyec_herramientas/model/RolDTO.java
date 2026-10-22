package com.example.proyec_herramientas.model;

// Vista del catalogo de roles que devuelve GET /roles.
public class RolDTO {

    private String idRol;
    private String nombre;
    private String descripcion;

    public RolDTO() {
    }

    public RolDTO(String idRol, String nombre, String descripcion) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getIdRol() {
        return idRol;
    }

    public void setIdRol(String idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
