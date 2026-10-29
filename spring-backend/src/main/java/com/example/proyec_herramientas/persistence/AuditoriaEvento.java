package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

// Bitacora global de la plataforma. Guarda el nombre de la tienda ademas del
// id porque es un registro historico: debe seguir siendo legible aunque la
// empresa se renombre o se elimine.
@Entity
@Table(name = "auditoria_evento")
public class AuditoriaEvento {

    public static final String TIPO_TIENDA_CREADA = "TIENDA_CREADA";
    public static final String TIPO_TIENDA_SUSPENDIDA = "TIENDA_SUSPENDIDA";
    public static final String TIPO_TIENDA_ACTIVADA = "TIENDA_ACTIVADA";
    public static final String TIPO_PLAN_ACTUALIZADO = "PLAN_ACTUALIZADO";
    public static final String TIPO_CONFIGURACION_ACTUALIZADA = "CONFIGURACION_ACTUALIZADA";
    public static final String TIPO_ADMIN_PASSWORD_RESTABLECIDO = "ADMIN_PASSWORD_RESTABLECIDO";
    public static final String TIPO_USUARIO_CREADO = "USUARIO_CREADO";
    public static final String TIPO_USUARIO_ACTUALIZADO = "USUARIO_ACTUALIZADO";
    public static final String TIPO_USUARIO_SUSPENDIDO = "USUARIO_SUSPENDIDO";
    public static final String TIPO_USUARIO_ACTIVADO = "USUARIO_ACTIVADO";
    public static final String TIPO_USUARIO_ELIMINADO = "USUARIO_ELIMINADO";
    public static final String TIPO_USUARIO_PASSWORD_RESTABLECIDO = "USUARIO_PASSWORD_RESTABLECIDO";

    public static final String SEVERIDAD_INFO = "INFO";
    public static final String SEVERIDAD_ALERTA = "ALERTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "fecha", nullable = false)
    private Date fecha = new Date();

    @Column(name = "tipo", length = 40, nullable = false)
    private String tipo;

    @Column(name = "severidad", length = 15, nullable = false)
    private String severidad = SEVERIDAD_INFO;

    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;

    @Column(name = "id_tienda")
    private Integer idTienda;

    @Column(name = "tienda_nombre", length = 100)
    private String tiendaNombre;

    @Column(name = "actor", length = 100)
    private String actor;

    public AuditoriaEvento() {
    }

    public AuditoriaEvento(String tipo, String severidad, String descripcion,
            Integer idTienda, String tiendaNombre, String actor) {
        this.tipo = tipo;
        this.severidad = severidad;
        this.descripcion = descripcion;
        this.idTienda = idTienda;
        this.tiendaNombre = tiendaNombre;
        this.actor = actor;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }

    public String getTiendaNombre() {
        return tiendaNombre;
    }

    public void setTiendaNombre(String tiendaNombre) {
        this.tiendaNombre = tiendaNombre;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
