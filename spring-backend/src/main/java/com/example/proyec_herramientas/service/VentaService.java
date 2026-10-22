package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.persistence.Venta;

import java.util.List;

public interface VentaService {

    // idCliente siempre viene del token del CLIENTE autenticado, nunca del body.
    Venta registrarVenta(String idCliente, CarritoRequestDTO carrito);

    Venta obtenerVentaPorId(String idVenta);

    List<Venta> obtenerBoletas();

    List<Venta> obtenerHistorial(String idCliente);
}
