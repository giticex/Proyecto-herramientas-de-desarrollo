package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.TiendaDTO;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.TiendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository repository;

    public TiendaServiceImpl(TiendaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaDTO obtenerPorSlug(String slug) {
        return toDTO(buscar(slug));
    }

    @Override
    @Transactional(readOnly = true)
    public int obtenerIdPorSlug(String slug) {
        return buscar(slug).getIdTienda();
    }

    @Override
    @Transactional
    public TiendaDTO actualizar(String slug, TiendaDTO dto, int idTiendaAutenticado) {
        Tienda tienda = buscar(slug);
        if (!tienda.getIdTienda().equals(idTiendaAutenticado)) {
            // No revela que la tienda existe pero es de otro ADMIN.
            throw new RecursoNoEncontradoException("Tienda " + slug + " no existe");
        }
        tienda.setNombre(dto.getNombre());
        tienda.setRubro(dto.getRubro());
        tienda.setColorPrimario(dto.getColorPrimario());
        tienda.setColorSecundario(dto.getColorSecundario());
        tienda.setLogoUrl(dto.getLogoUrl());
        return toDTO(repository.save(tienda));
    }

    private Tienda buscar(String slug) {
        return repository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda " + slug + " no existe"));
    }

    private TiendaDTO toDTO(Tienda tienda) {
        TiendaDTO dto = new TiendaDTO();
        dto.setNombre(tienda.getNombre());
        dto.setRubro(tienda.getRubro());
        dto.setSlug(tienda.getSlug());
        dto.setColorPrimario(tienda.getColorPrimario());
        dto.setColorSecundario(tienda.getColorSecundario());
        dto.setLogoUrl(tienda.getLogoUrl());
        return dto;
    }
}
