package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.LoginRequestDTO;
import com.example.proyec_herramientas.model.LoginResponseDTO;

public interface AuthService {

    // Devuelve null si las credenciales no coinciden con ninguna cuenta.
    LoginResponseDTO login(LoginRequestDTO request);
}
