package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.persistence.Venta;

import java.util.List;

public interface VentaService {

    // idCliente siempre viene del token del CLIENTE autenticado, nunca del body.
    Venta registrarVenta(String idCliente, CarritoRequestDTO carrito, int idTienda);

    Venta obtenerVentaPorId(String idVenta);

    List<Venta> obtenerBoletas(int idTienda);

    List<Venta> obtenerHistorial(String idCliente, Integer idTienda);

    // Historial de la tienda para el panel: paginado y filtrable.
    PaginaVentasDTO listarVentas(int idTienda, String busqueda, int pagina, int tamanio);
}
