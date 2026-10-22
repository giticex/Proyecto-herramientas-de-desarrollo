package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.DashboardResumenDTO;

public interface DashboardService {

    // Resumen de la tienda: ventas de hoy, ingresos del mes y top de productos.
    DashboardResumenDTO resumen(int idTienda);
}
