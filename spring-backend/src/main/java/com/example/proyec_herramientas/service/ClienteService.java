package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.PerfilClienteDTO;

import java.util.List;

public interface ClienteService {

    List<ClienteDTO> listar();

    ClienteDTO obtenerPorId(String idCliente);

    // Ficha propia del CLIENTE autenticado, con las direcciones que dejó en
    // sus comprobantes.
    PerfilClienteDTO obtenerPerfil(String idCliente);
}
