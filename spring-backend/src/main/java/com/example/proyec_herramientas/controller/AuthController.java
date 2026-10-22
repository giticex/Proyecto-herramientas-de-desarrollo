package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.LoginRequestDTO;
import com.example.proyec_herramientas.model.LoginResponseDTO;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;
import com.example.proyec_herramientas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /auth/login { usuario, clave } -> { token, usuario, rol, ... }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO respuesta = authService.login(request);
        if (respuesta == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contrasena incorrectos"));
        }
        return ResponseEntity.ok(respuesta);
    }

    // POST /auth/registro-tienda -> crea la tienda y su cuenta ADMIN, y deja
    // la sesion iniciada (devuelve token + tienda).
    @PostMapping("/registro-tienda")
    public ResponseEntity<LoginResponseDTO> registroTienda(@Valid @RequestBody RegistroTiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registroTienda(request));
    }

    // POST /auth/registro-cliente -> crea la cuenta del comprador y deja la
    // sesion iniciada.
    @PostMapping("/registro-cliente")
    public ResponseEntity<LoginResponseDTO> registroCliente(@Valid @RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registroCliente(request));
    }

    // GET /auth/rol -> rol de la cuenta que llamo, segun el token.
    @GetMapping("/rol")
    public ResponseEntity<?> rol(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesion no iniciada"));
        }
        String rol = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(Map.of("usuario", authentication.getName(), "rol", rol));
    }
}
