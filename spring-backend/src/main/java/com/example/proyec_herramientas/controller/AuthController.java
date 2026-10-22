package com.example.proyec_herramientas.controller;

import com.example.proyec_herramientas.model.auth.AuthResponse;
import com.example.proyec_herramientas.model.auth.LoginRequest;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;
import com.example.proyec_herramientas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro-tienda")
    public ResponseEntity<AuthResponse> registroTienda(@Valid @RequestBody RegistroTiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registroTienda(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro-cliente")
    public ResponseEntity<AuthResponse> registroCliente(@Valid @RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registroCliente(request));
    }
}
