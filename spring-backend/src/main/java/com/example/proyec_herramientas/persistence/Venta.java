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
 * Cabecera del comprobante de pago electronico (tabla venta).
 *
 * <p>Los datos del receptor y los totales se guardan "congelados" en el
 * comprobante: aunque el cliente cambie su direccion o el catalogo cambie de
 * precio, el comprobante emitido debe seguir mostrando lo que facturo.</p>
 */
@Entity
@Table(name = "venta")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Venta {

    @Id
    @Column(name = "id_venta", length = 36, nullable = false)
    private String idVenta;

    @Column(name = "id_cliente", length = 20, nullable = false)
    private String idCliente;

    @Column(name = "id_persona", length = 25, nullable = false)
    private String idPersona;

    @Column(name = "id_tipo_documento", length = 25, nullable = false)
    private String idTipoDocumento;

    @Column(name = "numero_documento", length = 36, nullable = false)
    private String numeroDocumento;

    @Column(name = "id_medio_pago", length = 10, nullable = false)
    private String idMedioPago;

    @Column(name = "fecha_emision", nullable = false)
    private Date fechaEmision;

    // --- Identificacion del comprobante (SUNAT) ---
    @Column(name = "serie", length = 4, nullable = false)
    private String serie;

    @Column(name = "correlativo", nullable = false)
    private int correlativo;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @Column(name = "condicion_operacion", length = 2, nullable = false)
    private String condicionOperacion;

    @Column(name = "forma_pago", length = 2, nullable = false)
    private String formaPago;

    // --- Totales del comprobante ---
    @Column(name = "total_venta", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalVenta = BigDecimal.ZERO;

    @Column(name = "total_gravada", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalGravada = BigDecimal.ZERO;

    @Column(name = "total_inafecta", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalInafecta = BigDecimal.ZERO;

    @Column(name = "total_exonerada", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalExonerada = BigDecimal.ZERO;

    @Column(name = "total_gratuita", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalGratuita = BigDecimal.ZERO;

    @Column(name = "total_igv", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalIgv = BigDecimal.ZERO;

    @Column(name = "total_descuentos", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "monto_total_credito", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotalCredito = BigDecimal.ZERO;

    // --- Receptor (se congela al emitir) ---
    @Column(name = "receptor_tipo_documento", length = 1, nullable = false)
    private String receptorTipoDocumento;

    @Column(name = "receptor_num_documento", length = 15, nullable = false)
    private String receptorNumDocumento;

    @Column(name = "receptor_razon_social", length = 200, nullable = false)
    private String receptorRazonSocial;

    @Column(name = "receptor_direccion", length = 200, nullable = false)
    private String receptorDireccion;

    @Column(name = "receptor_ubigeo", length = 6, nullable = false)
    private String receptorUbigeo;

    @Column(name = "receptor_distrito", length = 80, nullable = false)
    private String receptorDistrito;

    @Column(name = "receptor_provincia", length = 80, nullable = false)
    private String receptorProvincia;

    @Column(name = "receptor_departamento", length = 80, nullable = false)
    private String receptorDepartamento;

    @Column(name = "receptor_pais", length = 2, nullable = false)
    private String receptorPais;

    // --- Entrega (puede diferir del domicilio fiscal) ---
    @Column(name = "entrega_direccion", length = 200, nullable = false)
    private String entregaDireccion;

    @Column(name = "entrega_ubigeo", length = 6, nullable = false)
    private String entregaUbigeo;

    @Column(name = "email_contacto", length = 100, nullable = false)
    private String emailContacto;

    // --- Respuesta de SUNAT / verificacion ---
    @Column(name = "hash_comprobante", length = 64)
    private String hashComprobante;

    @Column(name = "codigo_qr", length = 500)
    private String codigoQr;

    @Column(name = "xml_name", length = 120)
    private String xmlName;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "estado_sunat", length = 20)
    private String estadoSunat;

    @Column(name = "cdR_summary", length = 500)
    private String cdrSummary;

    @Column(name = "leyendas", columnDefinition = "TEXT")
    private String leyendas;

    @Column(name = "observacion", length = 500, nullable = false)
    private String observacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idDetalle")
    private List<DetalleVenta> detalles = new ArrayList<>();

    @Transient
    private String cliente;

    @Transient
    private String tipoDocumento;

    @Transient
    private String medioPago;

    // Resultado de aplicar un credito a favor en esta venta (no se persiste:
    // el saldo real vive en la tabla credito).
    @Transient
    private String creditoCodigo;

    @Transient
    private BigDecimal creditoMontoUsado;

    @Transient
    private BigDecimal creditoSaldoRestante;

    public Venta() {
    }

    public String getId() {
        return idVenta;
    }

    public void setId(String idVenta) {
        this.idVenta = idVenta;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(String idPersona) {
        this.idPersona = idPersona;
    }

    public String getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(String idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getIdMedioPago() {
        return idMedioPago;
    }

    public void setIdMedioPago(String idMedioPago) {
        this.idMedioPago = idMedioPago;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
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

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta;
    }

    public BigDecimal getTotalGravada() {
        return totalGravada;
    }

    public void setTotalGravada(BigDecimal totalGravada) {
        this.totalGravada = totalGravada;
    }

    public BigDecimal getTotalInafecta() {
        return totalInafecta;
    }

    public void setTotalInafecta(BigDecimal totalInafecta) {
        this.totalInafecta = totalInafecta;
    }

    public BigDecimal getTotalExonerada() {
        return totalExonerada;
    }

    public void setTotalExonerada(BigDecimal totalExonerada) {
        this.totalExonerada = totalExonerada;
    }

    public BigDecimal getTotalGratuita() {
        return totalGratuita;
    }

    public void setTotalGratuita(BigDecimal totalGratuita) {
        this.totalGratuita = totalGratuita;
    }

    public BigDecimal getTotalIgv() {
        return totalIgv;
    }

    public void setTotalIgv(BigDecimal totalIgv) {
        this.totalIgv = totalIgv;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getMontoTotalCredito() {
        return montoTotalCredito;
    }

    public void setMontoTotalCredito(BigDecimal montoTotalCredito) {
        this.montoTotalCredito = montoTotalCredito;
    }

    public String getReceptorTipoDocumento() {
        return receptorTipoDocumento;
    }

    public void setReceptorTipoDocumento(String receptorTipoDocumento) {
        this.receptorTipoDocumento = receptorTipoDocumento;
    }

    public String getReceptorNumDocumento() {
        return receptorNumDocumento;
    }

    public void setReceptorNumDocumento(String receptorNumDocumento) {
        this.receptorNumDocumento = receptorNumDocumento;
    }

    public String getReceptorRazonSocial() {
        return receptorRazonSocial;
    }

    public void setReceptorRazonSocial(String receptorRazonSocial) {
        this.receptorRazonSocial = receptorRazonSocial;
    }

    public String getReceptorDireccion() {
        return receptorDireccion;
    }

    public void setReceptorDireccion(String receptorDireccion) {
        this.receptorDireccion = receptorDireccion;
    }

    public String getReceptorUbigeo() {
        return receptorUbigeo;
    }

    public void setReceptorUbigeo(String receptorUbigeo) {
        this.receptorUbigeo = receptorUbigeo;
    }

    public String getReceptorDistrito() {
        return receptorDistrito;
    }

    public void setReceptorDistrito(String receptorDistrito) {
        this.receptorDistrito = receptorDistrito;
    }

    public String getReceptorProvincia() {
        return receptorProvincia;
    }

    public void setReceptorProvincia(String receptorProvincia) {
        this.receptorProvincia = receptorProvincia;
    }

    public String getReceptorDepartamento() {
        return receptorDepartamento;
    }

    public void setReceptorDepartamento(String receptorDepartamento) {
        this.receptorDepartamento = receptorDepartamento;
    }

    public String getReceptorPais() {
        return receptorPais;
    }

    public void setReceptorPais(String receptorPais) {
        this.receptorPais = receptorPais;
    }

    public String getEntregaDireccion() {
        return entregaDireccion;
    }

    public void setEntregaDireccion(String entregaDireccion) {
        this.entregaDireccion = entregaDireccion;
    }

    public String getEntregaUbigeo() {
        return entregaUbigeo;
    }

    public void setEntregaUbigeo(String entregaUbigeo) {
        this.entregaUbigeo = entregaUbigeo;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public String getHashComprobante() {
        return hashComprobante;
    }

    public void setHashComprobante(String hashComprobante) {
        this.hashComprobante = hashComprobante;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getXmlName() {
        return xmlName;
    }

    public void setXmlName(String xmlName) {
        this.xmlName = xmlName;
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

    public String getCdrSummary() {
        return cdrSummary;
    }

    public void setCdrSummary(String cdrSummary) {
        this.cdrSummary = cdrSummary;
    }

    public String getLeyendas() {
        return leyendas;
    }

    public void setLeyendas(String leyendas) {
        this.leyendas = leyendas;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getCreditoCodigo() {
        return creditoCodigo;
    }

    public void setCreditoCodigo(String creditoCodigo) {
        this.creditoCodigo = creditoCodigo;
    }

    public BigDecimal getCreditoMontoUsado() {
        return creditoMontoUsado;
    }

    public void setCreditoMontoUsado(BigDecimal creditoMontoUsado) {
        this.creditoMontoUsado = creditoMontoUsado;
    }

    public BigDecimal getCreditoSaldoRestante() {
        return creditoSaldoRestante;
    }

    public void setCreditoSaldoRestante(BigDecimal creditoSaldoRestante) {
        this.creditoSaldoRestante = creditoSaldoRestante;
    }

    /** Total del comprobante (monto total de la venta). */
    public BigDecimal getTotal() {
        if (detalles == null || detalles.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        detalles.add(detalle);
    }
}
