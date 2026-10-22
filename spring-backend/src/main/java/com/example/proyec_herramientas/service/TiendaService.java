package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.TiendaDTO;

public interface TiendaService {

    TiendaDTO obtenerPorSlug(String slug);

    // Lanza RecursoNoEncontradoException si el slug no existe.
    int obtenerIdPorSlug(String slug);

    // Lanza RecursoNoEncontradoException si el slug no existe o no es de idTiendaAutenticado.
    TiendaDTO actualizar(String slug, TiendaDTO tienda, int idTiendaAutenticado);
}
