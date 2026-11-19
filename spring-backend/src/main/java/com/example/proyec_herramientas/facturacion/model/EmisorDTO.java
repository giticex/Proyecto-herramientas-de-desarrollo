package com.example.proyec_herramientas.facturacion.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos fiscales del emisor que se conservan con el comprobante. */
public class EmisorDTO {

    @NotBlank @Pattern(regexp = "[0-9]{11}")
    private String ruc;
    @NotBlank @Size(max = 200)
    private String razonSocial;
    @NotBlank @Size(max = 120)
    private String nombreComercial;
    @NotBlank @Size(max = 200)
    private String domicilioFiscal;
    @NotBlank @Pattern(regexp = "[0-9]{6}")
    private String ubigeo;
    @NotBlank @Size(max = 80)
    private String distrito;
    @NotBlank @Size(max = 80)
    private String provincia;
    @NotBlank @Size(max = 80)
    private String departamento;
    @NotBlank @Pattern(regexp = "PE")
    private String pais = "PE";
    @Email @Size(max = 120)
    private String email;
    @Size(max = 30)
    private String telefono;

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDomicilioFiscal() {
        return domicilioFiscal;
    }

    public void setDomicilioFiscal(String domicilioFiscal) {
        this.domicilioFiscal = domicilioFiscal;
    }

    public String getUbigeo() {
        return ubigeo;
    }

    public void setUbigeo(String ubigeo) {
        this.ubigeo = ubigeo;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
