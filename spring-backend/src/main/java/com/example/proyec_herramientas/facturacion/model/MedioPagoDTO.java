package com.example.proyec_herramientas.facturacion.model;

import java.math.BigDecimal;

/**
 * Medio de pago del comprobante (catalogo 09 de SUNAT).
 *
 * <p>Los codigos mas usados:</p>
 * <ul>
 *   <li>001 Efectivo</li>
 *   <li>002 Deposito en cuenta</li>
 *   <li>003 Transferencia de credito</li>
 *   <li>005 Ticket</li>
 *   <li>006 Tarjeta de credito</li>
 *   <li>009 Yape / Plin</li>
 *   <li>011 Billetera digital</li>
 * </ul>
 */
public class MedioPagoDTO {

    private String codigo;
    private String descripcion;
    private BigDecimal monto;
    private String referencia;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
