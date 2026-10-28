package com.example.proyec_herramientas.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Credito a favor del cliente generado por una devolucion (tabla credito).
 *
 * <p>El comprobante original de la venta NUNCA se modifica: la devolucion
 * devuelve el stock y este credito registra el saldo que la tienda le debe al
 * cliente. El codigo es secuencial (BOL-CR-XXXXXX) y se imprime para canjear
 * en el POS o reembolsar en caja.</p>
 *
 * <p>Estados: Vigente (tiene saldo usable), Usado (saldo consumido hasta
 * S/ 0.00) y Reembolsado (la tienda devolvio el dinero en caja).</p>
 */
@Entity
@Table(name = "credito")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Credito {

    public static final String ESTADO_VIGENTE = "Vigente";
    public static final String ESTADO_USADO = "Usado";
    public static final String ESTADO_REEMBOLSADO = "Reembolsado";

    public static final String PREFIJO_CODIGO = "BOL-CR-";

    @Id
    @Column(name = "id_credito", length = 36, nullable = false)
    private String idCredito;

    /** Codigo secuencial unico que canjea el cliente (BOL-CR-XXXXXX). */
    @Column(name = "codigo", length = 20, nullable = false, unique = true)
    private String codigo;

    /** Venta sobre la que se hizo la devolucion (referencia, no FK dura). */
    @Column(name = "id_venta_origen", length = 36, nullable = false)
    private String idVentaOrigen;

    @Column(name = "numero_documento_origen", length = 36, nullable = false)
    private String numeroDocumentoOrigen;

    @Column(name = "cliente_nombre", length = 200, nullable = false)
    private String clienteNombre = "";

    @Column(name = "dni_cliente", length = 15, nullable = false)
    private String dniCliente = "";

    @Column(name = "monto_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(name = "monto_disponible", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoDisponible = BigDecimal.ZERO;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado = ESTADO_VIGENTE;

    @Column(name = "fecha", nullable = false)
    private Date fecha = new Date();

    public Credito() {
    }

    public String getIdCredito() {
        return idCredito;
    }

    public void setIdCredito(String idCredito) {
        this.idCredito = idCredito;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getIdVentaOrigen() {
        return idVentaOrigen;
    }

    public void setIdVentaOrigen(String idVentaOrigen) {
        this.idVentaOrigen = idVentaOrigen;
    }

    public String getNumeroDocumentoOrigen() {
        return numeroDocumentoOrigen;
    }

    public void setNumeroDocumentoOrigen(String numeroDocumentoOrigen) {
        this.numeroDocumentoOrigen = numeroDocumentoOrigen;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getDniCliente() {
        return dniCliente;
    }

    public void setDniCliente(String dniCliente) {
        this.dniCliente = dniCliente;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getMontoDisponible() {
        return montoDisponible;
    }

    public void setMontoDisponible(BigDecimal montoDisponible) {
        this.montoDisponible = montoDisponible;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public boolean esVigente() {
        return ESTADO_VIGENTE.equals(estado);
    }
}
