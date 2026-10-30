package com.example.proyec_herramientas.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Turno de caja de un vendedor (tabla movimiento_caja).
 *
 * <p>La apertura fija el fondo inicial con el que arranca el cajon y el cierre
 * congela el efectivo esperado por el sistema, el efectivo contado a mano y la
 * diferencia. El resumen por metodo de pago no se guarda: se recalcula de las
 * ventas del rango [apertura, cierre], que nunca se modifican.</p>
 *
 * <p>Solo puede haber un turno ABIERTO por usuario y tienda; la validacion se
 * hace en CajaServiceImpl dentro de la transaccion de apertura.</p>
 */
@Entity
@Table(name = "movimiento_caja")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovimientoCaja {

    public static final String ESTADO_ABIERTA = "ABIERTA";
    public static final String ESTADO_CERRADA = "CERRADA";

    @Id
    @Column(name = "id_caja", length = 36, nullable = false)
    private String idCaja;

    @Column(name = "id_tienda", nullable = false)
    private Integer idTienda;

    @Column(name = "usuario", length = 50, nullable = false)
    private String usuario;

    @Column(name = "nombre_vendedor", length = 120, nullable = false)
    private String nombreVendedor = "";

    @Column(name = "estado", length = 12, nullable = false)
    private String estado = ESTADO_ABIERTA;

    @Column(name = "monto_apertura", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    @Column(name = "fecha_apertura", nullable = false)
    private Date fechaApertura = new Date();

    /** Efectivo esperado (apertura + ventas en efectivo del turno). */
    @Column(name = "monto_sistema", precision = 12, scale = 2)
    private BigDecimal montoSistema;

    /** Efectivo contado fisicamente al cerrar. */
    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    /** contado - esperado: negativo es faltante, positivo sobrante. */
    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "observacion", length = 200, nullable = false)
    private String observacion = "";

    @Column(name = "fecha_cierre")
    private Date fechaCierre;

    public MovimientoCaja() {
    }

    public boolean estaAbierta() {
        return ESTADO_ABIERTA.equals(estado);
    }

    public String getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(String idCaja) {
        this.idCaja = idCaja;
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public void setNombreVendedor(String nombreVendedor) {
        this.nombreVendedor = nombreVendedor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }

    public Date getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(Date fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public BigDecimal getMontoSistema() {
        return montoSistema;
    }

    public void setMontoSistema(BigDecimal montoSistema) {
        this.montoSistema = montoSistema;
    }

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(BigDecimal efectivoContado) {
        this.efectivoContado = efectivoContado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
}
