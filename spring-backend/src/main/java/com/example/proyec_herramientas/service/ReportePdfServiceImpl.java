package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.BoletaDTO;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.FacturaDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.VentaDTO;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.reportes.BoletaPDFGenerator;
import com.example.proyec_herramientas.reportes.FacturaPDFGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportePdfServiceImpl implements ReportePdfService {

    private final VentaService ventaService;
    private final ClienteService clienteService;

    public ReportePdfServiceImpl(VentaService ventaService, ClienteService clienteService) {
        this.ventaService = ventaService;
        this.clienteService = clienteService;
    }

    @Override
    public byte[] generarBoletaPdf(String idVenta) {
        Venta venta = ventaService.obtenerVentaPorId(idVenta);
        BoletaDTO boleta = new BoletaDTO(
                venta.getNumeroDocumento(),
                venta.getFechaEmision(),
                toClienteDTO(venta),
                toVentasDTO(venta));
        try {
            return BoletaPDFGenerator.generarPdfBytes(boleta);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando PDF de boleta", e);
        }
    }

    @Override
    public byte[] generarFacturaPdf(String idVenta) {
        Venta venta = ventaService.obtenerVentaPorId(idVenta);
        FacturaDTO factura = new FacturaDTO(
                venta.getNumeroDocumento(),
                venta.getFechaEmision(),
                toClienteDTO(venta),
                toVentasDTO(venta));
        try {
            return FacturaPDFGenerator.generarPdfBytes(factura);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando PDF de factura", e);
        }
    }

    private ClienteDTO toClienteDTO(Venta venta) {
        ClienteDTO cliente = clienteService.obtenerPorId(venta.getIdCliente());
        if (cliente != null) {
            return cliente;
        }
        ClienteDTO fallback = new ClienteDTO();
        String nombreCompleto = venta.getCliente();
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            String[] partes = nombreCompleto.trim().split("\\s+", 2);
            fallback.setNombre(partes[0]);
            fallback.setApellido(partes.length > 1 ? partes[1] : "");
        } else {
            fallback.setNombre("Invitado");
            fallback.setApellido("");
        }
        return fallback;
    }

    private List<VentaDTO> toVentasDTO(Venta venta) {
        return venta.getDetalles().stream()
                .map(this::toVentaDTO)
                .toList();
    }

    private VentaDTO toVentaDTO(DetalleVenta detalle) {
        ProductoDTO producto = new ProductoDTO();
        producto.setIdProducto(detalle.getIdProducto());
        producto.setNombre(detalle.getProducto());
        producto.setPrecio(detalle.getPrecioUnitario().doubleValue());

        VentaDTO ventaDTO = new VentaDTO();
        ventaDTO.setProductoId(detalle.getIdProducto());
        ventaDTO.setCantidad(detalle.getCantidad());
        ventaDTO.setPrecioUnitario(detalle.getPrecioUnitario().doubleValue());
        ventaDTO.setProducto(producto);
        return ventaDTO;
    }
}