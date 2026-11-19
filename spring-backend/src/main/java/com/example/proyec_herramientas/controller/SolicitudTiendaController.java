package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.auth.SolicitudTiendaRequest;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.SolicitudTiendaService;
import com.example.proyec_herramientas.service.SolicitudTiendaService.Solicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudTiendaController {
    private final SolicitudTiendaService solicitudes;

    public SolicitudTiendaController(SolicitudTiendaService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> registrar(@Valid @RequestBody SolicitudTiendaRequest datos) {
        Solicitud solicitud = solicitudes.registrar(datos);
        return Map.of("id", solicitud.id(), "estado", solicitud.estado());
    }

    @GetMapping("/mia")
    public Solicitud propia(@AuthenticationPrincipal SesionPrincipal cuenta) {
        return solicitudes.consultarPropia(cuenta.usuario());
    }

    @PostMapping("/mia/respuesta")
    public Solicitud responder(@AuthenticationPrincipal SesionPrincipal cuenta, @Valid @RequestBody Respuesta cuerpo) {
        return solicitudes.responder(cuenta.usuario(), cuerpo.mensaje());
    }

    @GetMapping("/maestro")
    public List<Solicitud> listar() {
        return solicitudes.listar();
    }

    @PutMapping("/maestro/{id}")
    public Solicitud decidir(@AuthenticationPrincipal SesionPrincipal maestro,
            @PathVariable String id, @Valid @RequestBody Decision cuerpo) {
        return solicitudes.resolver(id, maestro.usuario(), cuerpo.estado(), cuerpo.mensaje());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflictoDeDatos() {
        return Map.of("error", "El correo o la dirección de tienda ya está en uso.");
    }

    public record Decision(@NotBlank @Pattern(regexp = "APROBADA|REQUIERE_DATOS|RECHAZADA") String estado,
            @Size(max = 500) String mensaje) {}
    public record Respuesta(@NotBlank @Size(max = 500) String mensaje) {}
}
