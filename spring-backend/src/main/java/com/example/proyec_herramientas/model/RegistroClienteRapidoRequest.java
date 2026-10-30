package com.example.proyec_herramientas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Alta rapida de un comprador desde el punto de venta: basta con el documento
 * y el nombre o razon social; la direccion es opcional y se imprime en el
 * comprobante. La tienda sale del token, nunca del cuerpo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistroClienteRapidoRequest {

    private String documento;
    private String nombre;
    private String direccion;
    private String telefono;

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
