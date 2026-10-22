package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.auth.AuthResponse;
import com.example.proyec_herramientas.model.auth.LoginRequest;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;

public interface AuthService {

    AuthResponse registroTienda(RegistroTiendaRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse registroCliente(RegistroClienteRequest request);
}
