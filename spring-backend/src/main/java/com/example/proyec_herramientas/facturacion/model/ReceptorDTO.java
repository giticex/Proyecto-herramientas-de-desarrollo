package com.example.proyec_herramientas.facturacion.model;

/**
 * Receptor del comprobante (cliente al que se le emite).
 *
 * <p>Tipos de documento del catálogo de documentos de identidad de SUNAT relevantes:</p>
 * <ul>
 *   <li>0 = Sin documento</li>
 *   <li>1 = DNI</li>
 *   <li>4 = Carnet de Extranjeria</li>
 *   <li>6 = RUC</li>
 *   <li>7 = Pasaporte</li>
 *   <li>A = CedulaDiplomatica de Identidad</li>
 * </ul>
 */
public class ReceptorDTO {

    /** RUC. */
    public static final String TIPO_RUC = "6";
    /** DNI. */
    public static final String TIPO_DNI = "1";

    private String tipoDocumento;
    private String numeroDocumento;
    private String razonSocial;
    private String correo;
    private DomicilioDTO domicilioFiscal;

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public DomicilioDTO getDomicilioFiscal() {
        return domicilioFiscal;
    }

    public void setDomicilioFiscal(DomicilioDTO domicilioFiscal) {
        this.domicilioFiscal = domicilioFiscal;
    }
}
