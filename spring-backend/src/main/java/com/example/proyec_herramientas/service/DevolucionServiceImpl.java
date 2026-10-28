package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.ValidacionComprobanteException;
import com.example.proyec_herramientas.model.DevolucionRequestDTO;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.Devolucion;
import com.example.proyec_herramientas.persistence.DevolucionDetalle;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.DevolucionDetalleRepository;
import com.example.proyec_herramientas.repository.DevolucionRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.example.proyec_herramientas.security.AccesoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DevolucionServiceImpl implements DevolucionService {

    private final DevolucionRepository devolucionRepository;
    private final DevolucionDetalleRepository devolucionDetalleRepository;
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final VentaService ventaService;
    private final CreditoService creditoService;
    private final AccesoService accesoService;

    public DevolucionServiceImpl(DevolucionRepository devolucionRepository,
            DevolucionDetalleRepository devolucionDetalleRepository,
            VentaRepository ventaRepository,
            ProductoRepository productoRepository,
            VentaService ventaService,
            CreditoService creditoService,
            AccesoService accesoService) {
        this.devolucionRepository = devolucionRepository;
        this.devolucionDetalleRepository = devolucionDetalleRepository;
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.ventaService = ventaService;
        this.creditoService = creditoService;
        this.accesoService = accesoService;
    }

    // La venta original NO se modifica (integridad documental): la devolución
    // queda registrada aparte y el saldo a favor vive en el crédito generado.
    @Override
    @Transactional
    public Devolucion registrarDevolucion(DevolucionRequestDTO request, Authentication authentication) {
        // Lanza 404 si la venta no existe y 403 si no pertenece a la tienda del
        // personal autenticado (un CLIENTE no llega aqui por SecurityConfig).
        accesoService.validarVenta(request.idVenta(), authentication);
        Venta venta = ventaRepository.findById(request.idVenta())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Venta " + request.idVenta() + " no encontrada"));

        Map<Integer, Integer> devueltosPorProducto = devueltosActuales(venta.getId());
        Devolucion devolucion = new Devolucion();
        devolucion.setIdDevolucion(UUID.randomUUID().toString());
        devolucion.setIdVenta(venta.getId());
        devolucion.setFecha(new Date());
        devolucion.setMotivo(request.motivo() == null ? "" : request.motivo().trim());
        devolucion.setUsuario(accesoService.principal(authentication).usuario());

        BigDecimal total = BigDecimal.ZERO;
        for (DevolucionRequestDTO.Item item : request.items()) {
            DetalleVenta detalle = venta.getDetalles().stream()
                    .filter(d -> d.getIdProducto() == item.idProducto())
                    .findFirst()
                    .orElseThrow(() -> new ValidacionComprobanteException(
                            "El producto " + item.idProducto() + " no pertenece a la venta "
                                    + venta.getNumeroDocumento()));

            int yaDevueltos = devueltosPorProducto.getOrDefault(item.idProducto(), 0);
            if (item.cantidad() + yaDevueltos > detalle.getCantidad()) {
                throw new ValidacionComprobanteException(
                        "No se pueden devolver " + item.cantidad() + " unidades de «"
                                + detalle.getDescripcion() + "»: solo quedan "
                                + (detalle.getCantidad() - yaDevueltos) + " por devolver");
            }

            Producto producto = productoRepository.findByIdForUpdate(item.idProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto " + item.idProducto() + " no existe"));
            producto.setStock(producto.getStock() + item.cantidad());
            productoRepository.save(producto);

            devolucion.agregarDetalle(new DevolucionDetalle(
                    item.idProducto(), detalle.getDescripcion(),
                    item.cantidad(), detalle.getPrecioUnitario()));
            devueltosPorProducto.merge(item.idProducto(), item.cantidad(), Integer::sum);
            total = total.add(detalle.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.cantidad())));
        }
        devolucion.setTotal(total);

        // Devolución y crédito se guardan en la misma transacción: o se crean
        // ambos o no se crea ninguno.
        Credito credito = creditoService.generarParaDevolucion(devolucion, venta);
        devolucion.setIdCredito(credito.getIdCredito());
        devolucion.setCreditoCodigo(credito.getCodigo());

        // La entidad tiene id asignado, asi que save() hace merge y devuelve
        // otra instancia: los campos @Transient hay que copiarlos al resultado
        // para que la respuesta incluya el codigo del credito.
        Devolucion guardada = devolucionRepository.save(devolucion);
        guardada.setCreditoCodigo(credito.getCodigo());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerVentaParaDevolucion(String idVenta, Authentication authentication) {
        accesoService.validarVenta(idVenta, authentication);
        Venta venta = ventaService.obtenerVentaPorId(idVenta);
        Map<Integer, Integer> devueltos = devueltosActuales(idVenta);
        for (DetalleVenta detalle : venta.getDetalles()) {
            detalle.setCantidadDevuelta(devueltos.getOrDefault(detalle.getIdProducto(), 0));
        }
        return venta;
    }

    private Map<Integer, Integer> devueltosActuales(String idVenta) {
        Map<Integer, Integer> devueltos = new HashMap<>();
        for (Object[] fila : devolucionDetalleRepository.devueltosPorVenta(idVenta)) {
            devueltos.put((Integer) fila[0], ((Number) fila[1]).intValue());
        }
        return devueltos;
    }
}
