package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

    public static final String CONTRASENA_POR_DEFECTO =
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    @Id
    @Column(name = "id_cliente", length = 20, nullable = false)
    private String idCliente;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "apellido", length = 50, nullable = false)
    private String apellido;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "contrasena", length = 255, nullable = false)
    private String contrasena = CONTRASENA_POR_DEFECTO;

    @Column(name = "correo", length = 100)
    private String correo;

    // FK a la tabla rol. El comprador es CLIENTE por defecto: es el rol con
    // menos permisos, asi que una cuenta creada sin rol explicito no abre
    // puertas de mas.
    @Column(name = "id_rol", length = 15, nullable = false)
    private String idRol = Rol.CLIENTE;

    public Cliente() {
    }

    public Cliente(String idCliente, String nombre, String apellido, String telefono, String correo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.correo = correo;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getIdRol() {
        return idRol;
    }

    public void setIdRol(String idRol) {
        this.idRol = idRol;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
