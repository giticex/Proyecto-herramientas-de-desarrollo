package com.example.proyec_herramientas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// idCliente NO va aqui: lo pone el controller a partir del token del CLIENTE
// autenticado, nunca del cuerpo de la peticion. ignoreUnknown tolera que el
// frontend todavia mande idCliente mientras se actualiza.
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarritoRequestDTO {

    @NotNull
    private String tipoDocumento;

    private String medioPago;

    // Solo lo usan el ADMIN y el VENDEDOR para vender a nombre de un cliente
    // (mostrador). Para un CLIENTE se ignora: su idCliente sale del token.
    private String idCliente;

    // Documento del adquirente que imprime el comprobante. En factura es el
    // RUC (11 digitos) y en boleta el DNI (8 digitos) si el comprador lo da.
    private String documentoCliente;

    private String razonSocial;

    // Codigo BOL-CR-XXXXXX del credito a favor que se aplica al total. Si el
    // saldo no cubre todo, el resto se cobra con el medio de pago elegido.
    private String creditoCodigo;

    @NotNull
    @NotEmpty
    private List<Item> items;

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

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getDocumentoCliente() {
        return documentoCliente;
    }

    public void setDocumentoCliente(String documentoCliente) {
        this.documentoCliente = documentoCliente;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getCreditoCodigo() {
        return creditoCodigo;
    }

    public void setCreditoCodigo(String creditoCodigo) {
        this.creditoCodigo = creditoCodigo;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        @NotNull
        private Integer idProducto;

        @NotNull
        @Min(1)
        private Integer cantidad;

        public Integer getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Integer idProducto) {
            this.idProducto = idProducto;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}
