package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ProductoDTO;

import java.util.List;

public interface ProductoService {

    // idTienda nunca sale del body: siempre viene del token del ADMIN.
    List<ProductoDTO> listar(int idTienda);

    // Método para filtrar por nombre o código dentro de la tienda actual
    List<ProductoDTO> filtrarPorNombreOCodigo(int idTienda, String filtro);

    ProductoDTO obtenerPorId(int idProducto, int idTienda);

    ProductoDTO crear(ProductoDTO producto, int idTienda);

    ProductoDTO actualizar(int idProducto, ProductoDTO producto, int idTienda);

    void eliminar(int idProducto, int idTienda);
}