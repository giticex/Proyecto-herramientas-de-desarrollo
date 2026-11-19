package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository repository;

    public ClienteServiceImpl(ClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO> listar(int idTienda) {
        return repository.listarConVentasDeTienda(idTienda).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO> obtenerPorIds(Collection<String> ids) {
        return repository.findAllById(ids).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDTO obtenerPorId(String idCliente) {
        Cliente cliente = repository.findById(idCliente).orElse(null);
        return cliente != null ? toDTO(cliente) : null;
    }

    private ClienteDTO toDTO(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setIdCliente(cliente.getIdCliente());
        dto.setNombre(cliente.getNombre());
        dto.setApellido(cliente.getApellido());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        return dto;
    }
}
