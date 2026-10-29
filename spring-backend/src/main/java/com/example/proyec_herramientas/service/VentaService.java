package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.security.Vendedor;

import java.util.List;

public interface VentaService {

    // idCliente siempre viene del token del CLIENTE autenticado, nunca del body.
    // El vendedor tambien sale del token: es null en las autocompras.
    Venta registrarVenta(String idCliente, CarritoRequestDTO carrito, Vendedor vendedor);

    Venta obtenerVentaPorId(String idVenta);

    List<Venta> obtenerBoletas();

    List<Venta> obtenerHistorial(String idCliente);

    // Boletas e historial acotados a la tienda del personal autenticado.
    List<Venta> obtenerBoletasDeTienda(int idTienda);

    List<Venta> obtenerHistorialDeTienda(int idTienda, String idCliente);

    // Historial de la tienda para el panel: paginado y filtrable.
    PaginaVentasDTO listarVentas(int idTienda, String busqueda, int pagina, int tamanio);
}
