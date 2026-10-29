package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/** Linea de comprobante del detalle del arqueo. */
public class ArqueoComprobanteDTO {

    private String idVenta;
    private String fechaHora;
    private String numeroDocumento;
    private String cliente;
    private String documentoCliente;
    private String medioPago;
    private String grupoPago;
    private String turno;
    private String vendedor;
    private String vendedorUsuario;
    private BigDecimal total = BigDecimal.ZERO;

    public String getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(String idVenta) {
        this.idVenta = idVenta;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDocumentoCliente() {
        return documentoCliente;
    }

    public void setDocumentoCliente(String documentoCliente) {
        this.documentoCliente = documentoCliente;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getGrupoPago() {
        return grupoPago;
    }

    public void setGrupoPago(String grupoPago) {
        this.grupoPago = grupoPago;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public String getVendedorUsuario() {
        return vendedorUsuario;
    }

    public void setVendedorUsuario(String vendedorUsuario) {
        this.vendedorUsuario = vendedorUsuario;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
