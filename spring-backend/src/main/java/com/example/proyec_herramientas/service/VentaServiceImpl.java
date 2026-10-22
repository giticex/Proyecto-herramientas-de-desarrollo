package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.StockInsuficienteException;
import com.example.proyec_herramientas.model.CarritoRequestDTO;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.MedioPago;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.TipoDocumento;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.MedioPagoRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TipoDocumentoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VentaServiceImpl implements VentaService {

    private static final String TIPO_BOLETA = "Boleta";
    private static final String TIPO_BOLETA_ID = "TD001";
    private static final String TIPO_FACTURA_ID = "TD002";
    private static final String MEDIO_PAGO_EFECTIVO = "Efectivo";
    private static final String MEDIO_PAGO_EFECTIVO_ID = "MP001";

    private static final Map<String, String> ALIAS_MEDIO_PAGO = Map.of(
            "tarjeta", "Tarjeta de crédito",
            "tarjeta de credito", "Tarjeta de crédito");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;
    private final PersonaRepository personaRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final MedioPagoRepository medioPagoRepository;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            ClienteService clienteService,
                            PersonaRepository personaRepository,
                            TipoDocumentoRepository tipoDocumentoRepository,
                            MedioPagoRepository medioPagoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clienteService = clienteService;
        this.personaRepository = personaRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.medioPagoRepository = medioPagoRepository;
    }

    @Override
    @Transactional
    public Venta registrarVenta(String idCliente, CarritoRequestDTO carrito) {
        String tipoDocumento = conValor(carrito.getTipoDocumento(), TIPO_BOLETA);
        String medioPago = conValor(carrito.getMedioPago(), MEDIO_PAGO_EFECTIVO);

        ClienteDTO cliente = clienteService.obtenerPorId(idCliente);
        if (cliente == null) {
            throw new RecursoNoEncontradoException("Cliente " + idCliente + " no existe");
        }

        String idPersona = personaRepository.findIdPersonaByClienteId(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El cliente " + idCliente + " no tiene una persona asociada"));

        Venta venta = new Venta();
        venta.setId(UUID.randomUUID().toString());
        venta.setIdCliente(idCliente);
        venta.setIdPersona(idPersona);
        venta.setIdTipoDocumento(resolverTipoDocumento(tipoDocumento));
        venta.setNumeroDocumento(siguienteNumero(tipoDocumento));
        venta.setIdMedioPago(resolverMedioPago(medioPago));
        venta.setFechaEmision(new Date());

        for (CarritoRequestDTO.Item item : carrito.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto " + item.getIdProducto() + " no existe"));

            if (producto.getStock() < item.getCantidad()) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para " + producto.getNombre()
                                + " (disponible: " + producto.getStock() + ")");
            }

            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            venta.agregarDetalle(new DetalleVenta(
                    producto.getIdProducto(),
                    producto.getNombre(),
                    item.getCantidad(),
                    producto.getPrecio()));
        }

        return enriquecer(ventaRepository.save(venta));
    }

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerVentaPorId(String idVenta) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta " + idVenta + " no encontrada"));
        return enriquecer(List.of(venta)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> obtenerBoletas() {
        String idTipoDocumento = tipoDocumentoRepository.findByNombreIgnoreCase(TIPO_BOLETA)
                .map(TipoDocumento::getIdDocumento)
                .orElse(TIPO_BOLETA_ID);
        return enriquecer(ventaRepository.findByIdTipoDocumentoOrderByFechaEmisionDesc(idTipoDocumento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> obtenerHistorial(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return enriquecer(ventaRepository.findAllByOrderByFechaEmisionDesc());
        }
        return enriquecer(ventaRepository.findByIdClienteOrderByFechaEmisionDesc(idCliente));
    }

    private String resolverTipoDocumento(String nombre) {
        return tipoDocumentoRepository.findByNombreIgnoreCase(nombre)
                .map(TipoDocumento::getIdDocumento)
                .orElseGet(() -> TIPO_BOLETA.equalsIgnoreCase(nombre) ? TIPO_BOLETA_ID : TIPO_FACTURA_ID);
    }

    private String resolverMedioPago(String nombre) {
        String descripcion = ALIAS_MEDIO_PAGO.getOrDefault(nombre.trim().toLowerCase(), nombre.trim());
        return medioPagoRepository.findByDescripcionIgnoreCase(descripcion)
                .map(MedioPago::getIdMedioPago)
                .orElse(MEDIO_PAGO_EFECTIVO_ID);
    }

    private String siguienteNumero(String tipoDocumento) {
        long contador = ventaRepository.count();
        String prefijo = TIPO_BOLETA.equalsIgnoreCase(tipoDocumento) ? "BOL" : "FAC";
        return prefijo + "-" + String.format("%06d", contador + 1);
    }

    private String conValor(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    private Venta enriquecer(Venta venta) {
        return enriquecer(List.of(venta)).get(0);
    }

    private List<Venta> enriquecer(List<Venta> ventas) {
        if (ventas.isEmpty()) {
            return ventas;
        }

        Map<String, String> nombresTipoDocumento = tipoDocumentoRepository.findAll().stream()
                .collect(Collectors.toMap(TipoDocumento::getIdDocumento, TipoDocumento::getNombre));
        Map<String, String> descripcionesMedioPago = medioPagoRepository.findAll().stream()
                .collect(Collectors.toMap(MedioPago::getIdMedioPago, MedioPago::getDescripcion));
        Map<String, String> nombresCliente = clienteService.listar().stream()
                .collect(Collectors.toMap(ClienteDTO::getIdCliente, c -> c.getNombre() + " " + c.getApellido()));

        Set<Integer> idsProducto = ventas.stream()
                .filter(venta -> venta.getDetalles() != null)
                .flatMap(venta -> venta.getDetalles().stream())
                .map(DetalleVenta::getIdProducto)
                .collect(Collectors.toSet());
        Map<Integer, String> nombresProducto = productoRepository.findAllById(idsProducto).stream()
                .collect(Collectors.toMap(Producto::getIdProducto, Producto::getNombre));

        for (Venta venta : ventas) {
            venta.setTipoDocumento(nombresTipoDocumento.get(venta.getIdTipoDocumento()));
            venta.setMedioPago(descripcionesMedioPago.get(venta.getIdMedioPago()));
            venta.setCliente(nombresCliente.getOrDefault(venta.getIdCliente(), "Invitado"));
            if (venta.getDetalles() != null) {
                for (DetalleVenta detalle : venta.getDetalles()) {
                    detalle.setProducto(nombresProducto.get(detalle.getIdProducto()));
                }
            }
        }
        return ventas;
    }
}
