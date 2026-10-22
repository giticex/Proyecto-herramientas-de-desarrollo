package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Parametro de configuracion del sistema (tabla parametro).
 * Guarda los datos del emisor, las series y la tasa de IGV, para que
 * puedan editarse sin tocar el codigo.
 */
@Entity
@Table(name = "parametro")
public class Parametro {

    @Id
    @Column(name = "id_parametro", length = 30, nullable = false)
    private String idParametro;

    @Column(name = "valor", length = 50, nullable = false)
    private String valor;

    @Column(name = "descripcion", length = 100, nullable = false)
    private String descripcion;

    public Parametro() {
    }

    public Parametro(String idParametro, String valor, String descripcion) {
        this.idParametro = idParametro;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public String getIdParametro() {
        return idParametro;
    }

    public void setIdParametro(String idParametro) {
        this.idParametro = idParametro;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
