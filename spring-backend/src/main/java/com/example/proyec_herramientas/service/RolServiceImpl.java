package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.RolDTO;
import com.example.proyec_herramientas.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository repository;

    public RolServiceImpl(RolRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> listar() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getIdRol().compareToIgnoreCase(b.getIdRol()))
                .map(rol -> new RolDTO(rol.getIdRol(), rol.getNombre(), rol.getDescripcion()))
                .toList();
    }
}
