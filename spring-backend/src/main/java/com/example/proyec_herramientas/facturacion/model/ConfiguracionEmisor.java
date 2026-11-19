package com.example.proyec_herramientas.facturacion.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfiguracionEmisor(
        @NotNull @Valid EmisorDTO emisor,
        @NotBlank @Pattern(regexp = "B[A-Z0-9]{3}") String serieBoleta,
        @NotBlank @Pattern(regexp = "F[A-Z0-9]{3}") String serieFactura) {

    public String serie(String tipoComprobante) {
        return ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante) ? serieFactura : serieBoleta;
    }
}
