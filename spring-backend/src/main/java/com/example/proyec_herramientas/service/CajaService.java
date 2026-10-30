package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.AperturaCajaRequest;
import com.example.proyec_herramientas.model.CajaEstadoDTO;
import com.example.proyec_herramientas.model.CierreCajaRequest;
import com.example.proyec_herramientas.security.SesionPrincipal;

/**
 * Turnos de caja del personal de tienda: apertura con fondo inicial, resumen
 * del turno por metodo de pago y cierre con arqueo fisico.
 */
public interface CajaService {

    CajaEstadoDTO estado(SesionPrincipal sesion);

    CajaEstadoDTO abrir(SesionPrincipal sesion, AperturaCajaRequest request);

    CajaEstadoDTO cerrar(SesionPrincipal sesion, CierreCajaRequest request);
}
