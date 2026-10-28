package com.example.proyec_herramientas.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud de devolucion: a que venta aplica, por que motivo y que productos
 * (con sus cantidades) se devuelven.
 */
public record DevolucionRequestDTO(
        @NotBlank(message = "es obligatorio") String idVenta,
        @Size(max = 300, message = "no puede superar 300 caracteres") String motivo,
        @NotEmpty(message = "no puede estar vacío") @Valid List<Item> items) {

    public record Item(
            @NotNull(message = "es obligatorio") Integer idProducto,
            @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1") Integer cantidad) {
    }
}
