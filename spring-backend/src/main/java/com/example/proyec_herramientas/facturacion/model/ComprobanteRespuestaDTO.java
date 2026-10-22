package com.example.proyec_herramientas.facturacion.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprobante de pago electronico devuelto al frontend: datos de cabecera,
 * items, totales, leyendas y los elementos de verificacion (hash y QR) que
 * exige SUNAT en la representacion impresa.
 */
public class ComprobanteRespuestaDTO {

    private String idVenta;
    private String tipoComprobante;
    private String tipoComprobanteDescripcion;
    private String serie;
    private int correlativo;
    private String numeroDocumento;
    private String fechaEmision;
    private String horaEmision;
    private String moneda;
    private String condicionOperacion;
    private String formaPago;
    private String nombreComercio;

    private EmisorDTO emisor;
    private ReceptorDTO receptor;
    private DomicilioDTO entrega;

    private List<DetalleComprobanteDTO> detalles = new ArrayList<>();
    private TotalesDTO totales = new TotalesDTO();
    private List<LeyendaDTO> leyendas = new ArrayList<>();
    private List<MedioPagoDTO> mediosPago = new ArrayList<>();

    private String hashComprobante;
    private String valorResumen;
    private String codigoQr;
    private String qrPngBase64;
    private String estado;
    private String estadoSunat;
    private String observacion;
    private String nombreArchivoXml;

    public String getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(String idVenta) {
        this.idVenta = idVenta;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getTipoComprobanteDescripcion() {
        return tipoComprobanteDescripcion;
    }

    public void setTipoComprobanteDescripcion(String tipoComprobanteDescripcion) {
        this.tipoComprobanteDescripcion = tipoComprobanteDescripcion;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public int getCorrelativo() {
        return correlativo;
    }

    public void setCorrelativo(int correlativo) {
        this.correlativo = correlativo;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getHoraEmision() {
        return horaEmision;
    }

    public void setHoraEmision(String horaEmision) {
        this.horaEmision = horaEmision;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getCondicionOperacion() {
        return condicionOperacion;
    }

    public void setCondicionOperacion(String condicionOperacion) {
        this.condicionOperacion = condicionOperacion;
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public String getNombreComercio() {
        return nombreComercio;
    }

    public void setNombreComercio(String nombreComercio) {
        this.nombreComercio = nombreComercio;
    }

    public EmisorDTO getEmisor() {
        return emisor;
    }

    public void setEmisor(EmisorDTO emisor) {
        this.emisor = emisor;
    }

    public ReceptorDTO getReceptor() {
        return receptor;
    }

    public void setReceptor(ReceptorDTO receptor) {
        this.receptor = receptor;
    }

    public DomicilioDTO getEntrega() {
        return entrega;
    }

    public void setEntrega(DomicilioDTO entrega) {
        this.entrega = entrega;
    }

    public List<DetalleComprobanteDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleComprobanteDTO> detalles) {
        this.detalles = detalles;
    }

    public TotalesDTO getTotales() {
        return totales;
    }

    public void setTotales(TotalesDTO totales) {
        this.totales = totales;
    }

    public List<LeyendaDTO> getLeyendas() {
        return leyendas;
    }

    public void setLeyendas(List<LeyendaDTO> leyendas) {
        this.leyendas = leyendas;
    }

    public List<MedioPagoDTO> getMediosPago() {
        return mediosPago;
    }

    public void setMediosPago(List<MedioPagoDTO> mediosPago) {
        this.mediosPago = mediosPago;
    }

    public String getHashComprobante() {
        return hashComprobante;
    }

    public void setHashComprobante(String hashComprobante) {
        this.hashComprobante = hashComprobante;
    }

    /** Valor Resumen: cadena en base 64 del hash SHA-256 del XML. */
    public String getValorResumen() {
        return valorResumen;
    }

    public void setValorResumen(String valorResumen) {
        this.valorResumen = valorResumen;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getQrPngBase64() {
        return qrPngBase64;
    }

    public void setQrPngBase64(String qrPngBase64) {
        this.qrPngBase64 = qrPngBase64;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoSunat() {
        return estadoSunat;
    }

    public void setEstadoSunat(String estadoSunat) {
        this.estadoSunat = estadoSunat;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getNombreArchivoXml() {
        return nombreArchivoXml;
    }

    public void setNombreArchivoXml(String nombreArchivoXml) {
        this.nombreArchivoXml = nombreArchivoXml;
    }
}
