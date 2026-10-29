package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.persistence.Correlativo;
import com.example.proyec_herramientas.repository.CorrelativoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fuente unica de numeracion de comprobantes. La tabla `correlativo` bloquea
 * la fila de la serie (SELECT ... FOR UPDATE) dentro de la transaccion de la
 * venta, por lo que el punto de venta y la emision electronica comparten la
 * misma secuencia y no pueden duplicar un numero de la clave
 * (serie, correlativo) de `venta`.
 *
 * <p>Si la serie todavia no tiene fila (series heredadas como BOL/FAC, que se
 * numeraron con MAX sobre `venta`), se inicializa desde el mayor correlativo
 * ya emitido de esa serie para no reutilizar numeros historicos.</p>
 */
@Service
public class CorrelativoService {

    private final CorrelativoRepository correlativoRepository;
    private final VentaRepository ventaRepository;

    public CorrelativoService(CorrelativoRepository correlativoRepository,
                              VentaRepository ventaRepository) {
        this.correlativoRepository = correlativoRepository;
        this.ventaRepository = ventaRepository;
    }

    /**
     * Reserva el siguiente numero de la serie. Debe invocarse dentro de la
     * transaccion que persiste la venta para que el bloqueo y la insercion se
     * confirmen (o reviertan) juntos.
     */
    @Transactional
    public synchronized int siguiente(String serie, String idTipoDocumento) {
        Correlativo correlativo = correlativoRepository.findBySerieForUpdate(serie)
                .orElseGet(() -> new Correlativo(serie, idTipoDocumento,
                        ventaRepository.maxCorrelativoPorSerie(serie)));
        int numero = correlativo.siguienteNumero();
        correlativoRepository.save(correlativo);
        return numero;
    }
}
