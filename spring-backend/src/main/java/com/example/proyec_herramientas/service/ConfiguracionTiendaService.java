package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ConfiguracionTiendaDTO;

public interface ConfiguracionTiendaService {

    /** Configuracion de la tienda del ADMIN autenticado. */
    ConfiguracionTiendaDTO obtener(int idTienda);

    /**
     * Guarda los datos comerciales y las series de facturacion. La tienda
     * destino sale del token: el cuerpo de la solicitud no puede elegirla.
     */
    ConfiguracionTiendaDTO actualizar(int idTienda, ConfiguracionTiendaDTO configuracion);
}
