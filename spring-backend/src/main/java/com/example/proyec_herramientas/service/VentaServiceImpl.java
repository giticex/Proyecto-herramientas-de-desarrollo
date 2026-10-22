package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.StockInsuficienteException;
import com.example.proyec_herramientas.facturacion.service.EmisorService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final String PREFIJO_BOLETA = "BOL";
    private static final String PREFIJO_FACTURA = "FAC";
    private static final String CONDICION_OPERACION = "01";
    private static final String FORMA_PAGO = "01";
    private static final String TIPO_DOCUMENTO_SIN_DOCUMENTO = "0";
    private static final String ESTADO_LEGACY = "LEGACY";
    private static final int ESCALA_VALOR_UNITARIO = 6;
    private static final int ESCALA_IMPORTE = 2;

    private static final Map<String, String> ALIAS_MEDIO_PAGO = Map.of(
            "tarjeta", "Tarjeta de crédito",
            "tarjeta de credito", "Tarjeta de crédito");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;
    private final PersonaRepository personaRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final EmisorService emisorService;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            ClienteService clienteService,
                            PersonaRepository personaRepository,
                            TipoDocumentoRepository tipoDocumentoRepository,
                            MedioPagoRepository medioPagoRepository,
                            EmisorService emisorService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clienteService = clienteService;
        this.personaRepository = personaRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.emisorService = emisorService;
    }

    @Override
    @Transactional
    public Venta registrarVenta(CarritoRequestDTO carrito) {
        String tipoDocumento = conValor(carrito.getTipoDocumento(), TIPO_BOLETA);
        String medioPago = conValor(carrito.getMedioPago(), MEDIO_PAGO_EFECTIVO);
        String idCliente = carrito.getIdCliente();

        ClienteDTO cliente = clienteService.obtenerPorId(idCliente);
        if (cliente == null) {
            throw new RecursoNoEncontradoException("Cliente " + idCliente + " no existe");
        }

        String idPersona = personaRepository.findIdPersonaByClienteId(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El cliente " + idCliente + " no tiene una persona asociada"));

        Venta venta = new Venta();
        int correlativo = siguienteCorrelativo();
        String prefijo = prefijo(tipoDocumento);
        venta.setId(UUID.randomUUID().toString());
        venta.setIdCliente(idCliente);
        venta.setIdPersona(idPersona);
        venta.setIdTipoDocumento(resolverTipoDocumento(tipoDocumento));
        venta.setNumeroDocumento(prefijo + "-" + String.format("%06d", correlativo));
        venta.setIdMedioPago(resolverMedioPago(medioPago));
        venta.setFechaEmision(new Date());
        venta.setSerie(prefijo);
        venta.setCorrelativo(correlativo);
        venta.setMoneda(emisorService.obtenerMoneda());
        venta.setCondicionOperacion(CONDICION_OPERACION);
        venta.setFormaPago(FORMA_PAGO);
        venta.setEstado(ESTADO_LEGACY);
        venta.setObservacion("");

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

            venta.agregarDetalle(detalle(producto, item.getCantidad()));
        }

        aplicarDatosFiscales(venta, cliente);

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

    private String prefijo(String tipoDocumento) {
        return TIPO_BOLETA.equalsIgnoreCase(tipoDocumento) ? PREFIJO_BOLETA : PREFIJO_FACTURA;
    }

    private int siguienteCorrelativo() {
        return (int) ventaRepository.count() + 1;
    }

    private DetalleVenta detalle(Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta(
                producto.getIdProducto(),
                producto.getNombre(),
                cantidad,
                producto.getPrecio());
        detalle.setCodigoItem(producto.getCodigo());
        detalle.setDescripcion(producto.getNombre());
        detalle.setUnidadMedida(producto.getUnidadMedida());
        detalle.setAfectacionIgv(producto.getAfectacionIgv());
        return detalle;
    }

    /**
     * Calcula los datos fiscales del comprobante: valor unitario sin IGV, IGV
     * por item y los totales por afectacion del IGV.
     */
    private void aplicarDatosFiscales(Venta venta, ClienteDTO cliente) {
        BigDecimal igvPorcentaje = emisorService.obtenerIgvPorcentaje();
        BigDecimal factor = BigDecimal.ONE.add(igvPorcentaje.movePointRight(2));

        BigDecimal totalVenta = BigDecimal.ZERO;
        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalInafecta = BigDecimal.ZERO;
        BigDecimal totalExonerada = BigDecimal.ZERO;
        BigDecimal totalGratuita = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        for (DetalleVenta detalle : venta.getDetalles()) {
            BigDecimal precio = detalle.getPrecioUnitario();
            BigDecimal subtotal = detalle.getSubtotal();
            totalVenta = totalVenta.add(subtotal);

            if (detalle.esGravado()) {
                BigDecimal valorUnitario = precio.divide(factor, ESCALA_VALOR_UNITARIO, RoundingMode.HALF_UP);
                BigDecimal base = valorUnitario.multiply(BigDecimal.valueOf(detalle.getCantidad()))
                        .setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
                BigDecimal igv = subtotal.subtract(base);
                detalle.setValorUnitario(valorUnitario);
                detalle.setIgvItem(igv);
                totalGravada = totalGravada.add(base);
                totalIgv = totalIgv.add(igv);
            } else {
                detalle.setValorUnitario(precio.setScale(ESCALA_VALOR_UNITARIO, RoundingMode.HALF_UP));
                detalle.setIgvItem(BigDecimal.ZERO);
                if (DetalleVenta.AFECTACION_INAFECTA.equals(detalle.getAfectacionIgv())) {
                    totalInafecta = totalInafecta.add(subtotal);
                } else if (DetalleVenta.AFECTACION_EXONERADA.equals(detalle.getAfectacionIgv())) {
                    totalExonerada = totalExonerada.add(subtotal);
                } else {
                    totalGratuita = totalGratuita.add(subtotal);
                }
            }
        }

        venta.setTotalVenta(totalVenta);
        venta.setTotalGravada(totalGravada);
        venta.setTotalInafecta(totalInafecta);
        venta.setTotalExonerada(totalExonerada);
        venta.setTotalGratuita(totalGratuita);
        venta.setTotalIgv(totalIgv);
        venta.setTotalDescuentos(BigDecimal.ZERO);
        venta.setMontoTotalCredito(BigDecimal.ZERO);

        venta.setReceptorTipoDocumento(TIPO_DOCUMENTO_SIN_DOCUMENTO);
        venta.setReceptorNumDocumento("");
        venta.setReceptorRazonSocial(cliente.getNombre() + " " + cliente.getApellido());
        venta.setReceptorDireccion("");
        venta.setReceptorUbigeo("");
        venta.setReceptorDistrito("");
        venta.setReceptorProvincia("");
        venta.setReceptorDepartamento("");
        venta.setReceptorPais("PE");
        venta.setEntregaDireccion("");
        venta.setEntregaUbigeo("");
        venta.setEmailContacto(cliente.getCorreo() == null ? "" : cliente.getCorreo());
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
