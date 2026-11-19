package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ClienteDTO;

import java.util.Collection;
import java.util.List;

public interface ClienteService {

    List<ClienteDTO> listar(int idTienda);

    List<ClienteDTO> obtenerPorIds(Collection<String> ids);

    ClienteDTO obtenerPorId(String idCliente);
}
