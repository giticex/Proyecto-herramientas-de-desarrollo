package com.example.proyec_herramientas.facturacion.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload que el frontend (React) envia al backend (Spring Boot) para emitir
 * un comprobante de pago electronico.
 *
 * <p>El backend NO confía en los importes del cliente: recalcula el valor
 * unitario, el IGV y los totales a partir del catalogo de productos. Lo que
 * si se toma del request es la identidad del receptor, la direccion de
 * entrega y los medios de pago.</p>
 */
public class ComprobanteRequestDTO {

    /** Factura. */
    public static final String TIPO_FACTURA = "01";
    /** Boleta de venta. */
    public static final String TIPO_BOLETA = "03";

    /** Contado. */
    public static final String CONDICION_CONTADO = "01";
    /** Credito. */
    public static final String CONDICION_CREDITO = "02";

    /** Forma de pago: Contado. */
    public static final String FORMA_PAGO_CONTADO = "01";
    /** Forma de pago: Credito. */
    public static final String FORMA_PAGO_CREDITO = "02";

    private String tipoComprobante = TIPO_BOLETA;
    private String idCliente;

    /** Serie a usar. Si se omite, se toma la serie del parametro del tipo. */
    private String serie;

    private String condicionOperacion = CONDICION_CONTADO;
    private String formaPago = FORMA_PAGO_CONTADO;
    private String moneda = "PEN";

    @Valid
    private ReceptorDTO receptor;

    /** Direccion de entrega (puede diferir del domicilio fiscal). */
    @Valid
    private DomicilioDTO entrega;

    @Valid
    private List<MedioPagoDTO> mediosPago = new ArrayList<>();

    @Valid
    @NotEmpty
    private List<ItemComprobanteDTO> items = new ArrayList<>();

    private String observacion;
    private String emailContacto;

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
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

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
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

    public List<MedioPagoDTO> getMediosPago() {
        return mediosPago;
    }

    public void setMediosPago(List<MedioPagoDTO> mediosPago) {
        this.mediosPago = mediosPago;
    }

    public List<ItemComprobanteDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemComprobanteDTO> items) {
        this.items = items;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }
}
