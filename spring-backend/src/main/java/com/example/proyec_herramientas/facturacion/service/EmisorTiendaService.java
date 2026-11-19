package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.ConfiguracionEmisor;
import com.example.proyec_herramientas.persistence.EmisorTienda;
import com.example.proyec_herramientas.repository.EmisorTiendaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class EmisorTiendaService {
    private final EmisorTiendaRepository emisores;

    public EmisorTiendaService(EmisorTiendaRepository emisores) {
        this.emisores = emisores;
    }

    @Transactional(readOnly = true)
    public Optional<ConfiguracionEmisor> obtener(int idTienda) {
        return emisores.findById(idTienda).map(EmisorTienda::configuracion);
    }

    @Transactional
    public ConfiguracionEmisor bloquearParaEmitir(int idTienda) {
        return emisores.bloquear(idTienda).map(EmisorTienda::configuracion).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "La tienda debe configurar su emisor antes de emitir comprobantes"));
    }

    @Transactional
    public ConfiguracionEmisor guardar(int idTienda, ConfiguracionEmisor configuracion) {
        EmisorTienda emisor = emisores.bloquear(idTienda)
                .orElseGet(() -> new EmisorTienda(idTienda, configuracion));
        emisor.actualizar(configuracion);
        return emisores.save(emisor).configuracion();
    }
}
