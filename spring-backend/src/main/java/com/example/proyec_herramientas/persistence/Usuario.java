package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Cuenta de acceso del personal de la tienda (ADMIN o VENDEDOR). Antes SCRUM-6
// la tabla "usuario" existia en MySQL pero no estaba mapeada: aqui se mapea
// junto con su rol, que vive en la base y no se deduce en el codigo.
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "usuario", length = 50, nullable = false)
    private String usuario;

    // SHA-256 en hexadecimal (64 caracteres) o BCrypt ("$2..."), nunca texto plano.
    @Column(name = "contrasena", length = 255, nullable = false)
    private String contrasena;

    @Column(name = "id_persona", length = 25, nullable = false)
    private String idPersona;

    // FK a la tabla rol. Sin valor por defecto a proposito: si el rol no se
    // indica, el insert falla y no se crea una cuenta sin permisos asignados.
    @Column(name = "id_rol", length = 15, nullable = false)
    private String idRol;

    // FK a la tienda que administra esta cuenta (multi-tienda).
    @Column(name = "id_tienda", nullable = false)
    private Integer idTienda;

    public Usuario() {
    }

    public Usuario(String usuario, String contrasena, String idPersona, String idRol) {
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.idPersona = idPersona;
        this.idRol = idRol;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

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

    public String getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(String idPersona) {
        this.idPersona = idPersona;
    }

    public String getIdRol() {
        return idRol;
    }

    public void setIdRol(String idRol) {
        this.idRol = idRol;
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }
}
