package com.example.proyec_herramientas.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Registro de una devolucion sobre una venta ya emitida (tabla devolucion).
 *
 * <p>Cada devolucion guarda sus propias lineas (DevolucionDetalle) con el
 * precio congelado del detalle original, asi el historico no cambia aunque el
 * catalogo se actualice. La suma de las cantidades devueltas por producto
 * nunca puede superar la cantidad vendida.</p>
 */
@Entity
@Table(name = "devolucion")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Devolucion {

    @Id
    @Column(name = "id_devolucion", length = 36, nullable = false)
    private String idDevolucion;

    @Column(name = "id_venta", length = 36, nullable = false)
    private String idVenta;

    @Column(name = "fecha", nullable = false)
    private Date fecha = new Date();

    @Column(name = "motivo", length = 300, nullable = false)
    private String motivo = "";

    @Column(name = "total", precision = 12, scale = 2, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    /** Usuario (ADMIN/VENDEDOR) que registro la devolucion. */
    @Column(name = "usuario", length = 50, nullable = false)
    private String usuario = "";

    /** Credito a favor generado por esta devolucion. */
    @Column(name = "id_credito", length = 36)
    private String idCredito;

    // Codigo del credito generado (se devuelve al POS; no se persiste).
    @Transient
    private String creditoCodigo;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idDetalle")
    private List<DevolucionDetalle> detalles = new ArrayList<>();

    public Devolucion() {
    }

    public String getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(String idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public String getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(String idVenta) {
        this.idVenta = idVenta;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIdCredito() {
        return idCredito;
    }

    public void setIdCredito(String idCredito) {
        this.idCredito = idCredito;
    }

    public String getCreditoCodigo() {
        return creditoCodigo;
    }

    public void setCreditoCodigo(String creditoCodigo) {
        this.creditoCodigo = creditoCodigo;
    }

    public List<DevolucionDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DevolucionDetalle> detalles) {
        this.detalles = detalles;
    }

    public void agregarDetalle(DevolucionDetalle detalle) {
        detalle.setDevolucion(this);
        detalles.add(detalle);
    }
}
