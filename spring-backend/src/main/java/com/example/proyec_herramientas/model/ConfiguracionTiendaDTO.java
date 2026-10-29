package com.example.proyec_herramientas.model;

/**
 * Datos comerciales y de facturacion que el ADMIN de una tienda puede editar.
 * `nombre` y `slug` son informativos: la identidad de la tienda no cambia
 * desde este endpoint.
 */
public class ConfiguracionTiendaDTO {

    private String nombre;
    private String slug;
    private String razonSocial;
    private String ruc;
    private String direccionFiscal;
    private String telefono;
    private String logoUrl;
    private String serieBoleta;
    private String serieFactura;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccionFiscal() {
        return direccionFiscal;
    }

    public void setDireccionFiscal(String direccionFiscal) {
        this.direccionFiscal = direccionFiscal;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getSerieBoleta() {
        return serieBoleta;
    }

    public void setSerieBoleta(String serieBoleta) {
        this.serieBoleta = serieBoleta;
    }

    public String getSerieFactura() {
        return serieFactura;
    }

    public void setSerieFactura(String serieFactura) {
        this.serieFactura = serieFactura;
    }
}
