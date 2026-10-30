package com.example.proyec_herramientas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado del turno de caja del vendedor autenticado y su arqueo.
 *
 * <p>El resumen por metodo de pago y el efectivo esperado se calculan siempre
 * de las ventas del turno (o del dia cuando aun no hay turno), nunca se toman
 * del cuerpo de la solicitud.</p>
 */
public class CajaEstadoDTO {

    public static final String ESTADO_SIN_TURNO = "SIN_TURNO";
    public static final String PERIODO_TURNO = "TURNO";
    public static final String PERIODO_DIA = "DIA";

    private boolean turnoAbierto;
    private String estado = ESTADO_SIN_TURNO;
    private String idCaja;
    private String usuario;
    private String nombreVendedor = "";
    private BigDecimal montoApertura = BigDecimal.ZERO;
    private String fechaApertura;
    private String fechaCierre;
    private BigDecimal montoSistema;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;
    private String observacion = "";
    private String periodo = PERIODO_DIA;
    private String desde;
    private String hasta;
    private BigDecimal efectivoEsperado;
    private BigDecimal totalVentas = BigDecimal.ZERO;
    private long comprobantes;
    private List<CajaGrupoDTO> metodosPago = new ArrayList<>();

    public boolean isTurnoAbierto() {
        return turnoAbierto;
    }

    public void setTurnoAbierto(boolean turnoAbierto) {
        this.turnoAbierto = turnoAbierto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(String idCaja) {
        this.idCaja = idCaja;
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

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }

    public String getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(String fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(String fechaCierre) {
        this.fechaCierre = fechaCierre;
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

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public BigDecimal getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public void setEfectivoEsperado(BigDecimal efectivoEsperado) {
        this.efectivoEsperado = efectivoEsperado;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public long getComprobantes() {
        return comprobantes;
    }

    public void setComprobantes(long comprobantes) {
        this.comprobantes = comprobantes;
    }

    public List<CajaGrupoDTO> getMetodosPago() {
        return metodosPago;
    }

    public void setMetodosPago(List<CajaGrupoDTO> metodosPago) {
        this.metodosPago = metodosPago;
    }
}
