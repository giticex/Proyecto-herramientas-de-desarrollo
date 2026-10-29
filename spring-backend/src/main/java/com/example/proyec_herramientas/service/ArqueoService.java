package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ArqueoDTO;

import java.time.LocalDate;

/**
 * Corte de caja y arqueo de la tienda: ingresos del dia o del mes agrupados
 * por metodo de pago, vendedor y turno, con su detalle y las exportaciones
 * contables en CSV y PDF.
 */
public interface ArqueoService {

    /**
     * Arqueo del periodo indicado. Si la fecha es null se usa el dia actual;
     * en el periodo mensual la fecha solo elige el mes.
     */
    ArqueoDTO generar(int idTienda, String periodo, LocalDate fecha);

    byte[] exportarCsv(int idTienda, String periodo, LocalDate fecha);

    byte[] exportarPdf(int idTienda, String periodo, LocalDate fecha);

    /** Nombre del archivo exportado (sin ruta), con el periodo ya normalizado. */
    String nombreArchivo(String periodo, LocalDate fecha, String extension);
}
