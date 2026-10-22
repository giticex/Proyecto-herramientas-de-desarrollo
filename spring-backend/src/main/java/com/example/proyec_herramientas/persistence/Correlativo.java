package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Correlativo vigente de una serie (tabla correlativo).
 * La numeracion de comprobantes DEBE ser correlativa y sin saltos: se
 * incrementa bloqueando la fila (SELECT ... FOR UPDATE) dentro de la misma
 * transaccion que persiste la venta.
 */
@Entity
@Table(name = "correlativo")
public class Correlativo {

    @Id
    @Column(name = "serie", length = 4, nullable = false)
    private String serie;

    @Column(name = "id_tipo_documento", length = 25, nullable = false)
    private String idTipoDocumento;

    @Column(name = "ultimo_numero", nullable = false)
    private int ultimoNumero;

    public Correlativo() {
    }

    public Correlativo(String serie, String idTipoDocumento, int ultimoNumero) {
        this.serie = serie;
        this.idTipoDocumento = idTipoDocumento;
        this.ultimoNumero = ultimoNumero;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(String idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }

    public int getUltimoNumero() {
        return ultimoNumero;
    }

    public void setUltimoNumero(int ultimoNumero) {
        this.ultimoNumero = ultimoNumero;
    }

    /** Reserva el siguiente correlativo de la serie y lo deja persistido. */
    public int siguienteNumero() {
        this.ultimoNumero = this.ultimoNumero + 1;
        return this.ultimoNumero;
    }
}
