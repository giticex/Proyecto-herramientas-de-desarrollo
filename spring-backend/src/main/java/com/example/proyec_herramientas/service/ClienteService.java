package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ClienteBusquedaDTO;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.PerfilClienteDTO;
import com.example.proyec_herramientas.model.RegistroClienteRapidoRequest;

import java.util.List;

public interface ClienteService {

    List<ClienteDTO> listar();

    ClienteDTO obtenerPorId(String idCliente);

    // Ficha del comprador para el POS por DNI/RUC, acotada a la tienda del
    // token. Si no hay ficha propia, reutiliza los datos del ultimo
    // comprobante emitido en esa tienda con el mismo documento.
    ClienteBusquedaDTO buscarPorDocumento(int idTienda, String documento);

    // Alta rapida desde el punto de venta cuando el documento no aparece.
    ClienteBusquedaDTO registrarRapido(int idTienda, RegistroClienteRapidoRequest request);

    // Ficha propia del CLIENTE autenticado, con las direcciones que dejó en
    // sus comprobantes.
    PerfilClienteDTO obtenerPerfil(String idCliente);
}
