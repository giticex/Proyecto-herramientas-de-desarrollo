package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.LoginRequestDTO;
import com.example.proyec_herramientas.model.LoginResponseDTO;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;

public interface AuthService {

    // Devuelve null si las credenciales no coinciden con ninguna cuenta.
    LoginResponseDTO login(LoginRequestDTO request);

    // Crea la tienda y su cuenta ADMIN; devuelve la sesion ya iniciada.
    LoginResponseDTO registroTienda(RegistroTiendaRequest request);

    // Crea la cuenta del comprador; devuelve la sesion ya iniciada.
    LoginResponseDTO registroCliente(RegistroClienteRequest request);
}
