package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.security.Vendedor;

import java.util.List;

/**
 * Emision y consulta de comprobantes de pago electronicos (factura y boleta
 * de venta) con el estandar UBL 2.1 de SUNAT.
 */
public interface ComprobanteService {

    /**
     * Emite un comprobante: valida los datos, recalcula los importes desde el
     * catalogo, reserva el correlativo, genera el XML UBL 2.1, calcula el hash
     * y el codigo QR, y lo persiste. El vendedor sale del token y queda
     * congelado en el comprobante; es null en las autocompras del CLIENTE.
     */
    ComprobanteRespuestaDTO emitir(ComprobanteRequestDTO request, Vendedor vendedor);

    /** Recupera un comprobante ya emitido por su identificador. */
    ComprobanteRespuestaDTO obtenerPorId(String idVenta);

    /** Lista los comprobantes emitidos a un cliente (historial de compras). */
    List<ComprobanteRespuestaDTO> listarPorCliente(String idCliente);

    /** Comprobantes del cliente cuyos productos pertenecen a la tienda indicada. */
    List<ComprobanteRespuestaDTO> listarPorClienteDeTienda(String idCliente, int idTienda);

    /** XML UBL 2.1 del comprobante (regenerado a partir de la informacion guardada). */
    String generarXml(String idVenta);

    /** Representacion impresa del comprobante en PDF. */
    byte[] generarPdf(String idVenta);

    /** Ticket compacto de 80 mm para impresora termica. */
    byte[] generarTicketPdf(String idVenta);
}
