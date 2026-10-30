package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.ClienteBusquedaDTO;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.PerfilClienteDTO;
import com.example.proyec_herramientas.model.RegistroClienteRapidoRequest;
import com.example.proyec_herramientas.security.AccesoService;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final AccesoService accesoService;

    public ClienteController(ClienteService clienteService, AccesoService accesoService) {
        this.clienteService = clienteService;
        this.accesoService = accesoService;
    }

    @GetMapping
    public List<ClienteDTO> listar() {
        return clienteService.listar();
    }

    // Busqueda del POS por DNI/RUC. La tienda sale del token: un ADMIN o
    // VENDEDOR solo encuentra las fichas y los comprobantes de su empresa.
    @GetMapping("/buscar")
    public ClienteBusquedaDTO buscar(@RequestParam("documento") String documento,
            Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        return clienteService.buscarPorDocumento(accesoService.tiendaObligatoria(sesion), documento);
    }

    // Alta rapida del comprador desde el punto de venta.
    @PostMapping
    public ResponseEntity<ClienteBusquedaDTO> registrarRapido(
            @RequestBody RegistroClienteRapidoRequest request, Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.registrarRapido(accesoService.tiendaObligatoria(sesion), request));
    }

    // Ficha del CLIENTE autenticado. El id sale del token; no hay forma de
    // consultar el perfil de otra cuenta desde el portal.
    @GetMapping("/mi-perfil")
    public PerfilClienteDTO miPerfil(Authentication authentication) {
        SesionPrincipal sesion = accesoService.principal(authentication);
        if (sesion.idCliente() == null || sesion.idCliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene un cliente asociado");
        }
        return clienteService.obtenerPerfil(sesion.idCliente());
    }
}
