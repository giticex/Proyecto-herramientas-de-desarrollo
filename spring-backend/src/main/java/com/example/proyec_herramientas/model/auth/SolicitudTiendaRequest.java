package com.example.proyec_herramientas.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudTiendaRequest(
        @NotBlank @Size(max = 50) String nombres,
        @NotBlank @Size(max = 50) String apellidos,
        @NotBlank @Size(max = 100) String negocio,
        @NotBlank @Size(max = 80) String rubro,
        @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*") String slug,
        @NotBlank @Email @Size(max = 50) String correo,
        @NotBlank @Pattern(regexp = "[0-9]{8}") String dni,
        @NotBlank @Pattern(regexp = "9[0-9]{8}") String telefono,
        @NotBlank @Pattern(regexp = "9[0-9]{8}") String telefonoAlternativo,
        @NotBlank @Size(min = 12, max = 72) String contrasena,
        @jakarta.validation.constraints.AssertTrue boolean pagoSimulado
) {}
