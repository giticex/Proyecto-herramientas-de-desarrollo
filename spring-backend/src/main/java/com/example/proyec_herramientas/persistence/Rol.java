package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Catalogo de roles del sistema. Es la unica fuente de verdad: el backend
// nunca deduce el rol de una cuenta, siempre lo lee de esta tabla.
@Entity
@Table(name = "rol")
public class Rol {

    public static final String ADMIN = "ADMIN";
    public static final String CLIENTE = "CLIENTE";

    @Id
    @Column(name = "id_rol", length = 15, nullable = false)
    private String idRol;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 100, nullable = false)
    private String descripcion;

    public Rol() {
    }

    public Rol(String idRol, String nombre, String descripcion) {
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
