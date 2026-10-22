package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.StockInsuficienteException;
import com.example.proyec_herramientas.exception.ValidacionComprobanteException;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.facturacion.model.DetalleComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.DomicilioDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import com.example.proyec_herramientas.facturacion.model.ItemComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.LeyendaDTO;
import com.example.proyec_herramientas.facturacion.model.MedioPagoDTO;
import com.example.proyec_herramientas.facturacion.model.ReceptorDTO;
import com.example.proyec_herramientas.facturacion.model.TotalesDTO;
import com.example.proyec_herramientas.persistence.Correlativo;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.CorrelativoRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.example.proyec_herramientas.reportes.ComprobantePdfGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emision de comprobantes de pago electronicos.
 *
 * <p>Reglas implementadas:</p>
 * <ul>
 *   <li>El backend es la fuente de verdad de los importes: el precio, la
 *       afectacion del IGV y la unidad de medida se leen del catalogo de
 *       productos, nunca del request.</li>
 *   <li>La factura exige RUC del adquirente; la boleta sin RUC no puede
 *       superar el limite legal (S/ 700.00, parametro configurable).</li>
 *   <li>El correlativo se reserva bloqueando la fila de la serie
 *       (SELECT ... FOR UPDATE) dentro de la misma transaccion que guarda la
 *       venta: la numeracion no puede tener saltos ni duplicados.</li>
 *   <li>El QR y el Valor Resumen se calculan segun la RSM 300-2019/SUNAT.</li>
 * </ul>
 */
@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    /** Tipo de documento del adquirente: RUC (catalogo 01). */
    private static final String TIPO_DOCUMENTO_RUC = "1";
    /** Tipo de documento del adquirente: DNI (catalogo 01). */
    private static final String TIPO_DOCUMENTO_DNI = "6";

    private static final String REGEX_RUC = "\\d{11}";
    private static final String REGEX_DNI = "\\d{8}";

    /** Estado interno del comprobante. */
    private static final String ESTADO_EMITIDO = "EMITIDO";
    /** Estado frente a SUNAT: el XML aun no se ha enviado al CDR. */
    private static final String ESTADO_SUNAT_GENERADO = "GENERADO";

    private static final String LEYENDA_MONTO_EN_LETRAS = "1000";
    private static final String LEYENDA_TRANSFERENCIA_GRATUITA = "1002";

    /** Medio de pago por defecto cuando el request no indica ninguno. */
    private static final String MEDIO_PAGO_EFECTIVO = "001";

    /** Descripcion -> codigo del catalogo 09 de SUNAT. */
    private static final Map<String, String> MEDIOS_PAGO_CATALOGO_09 = new LinkedHashMap<>();

    /** Codigo del catalogo 09 -> id de la tabla `medio_pago`. */
    private static final Map<String, String> MEDIO_PAGO_BD = new LinkedHashMap<>();

    static {
        MEDIOS_PAGO_CATALOGO_09.put("efectivo", "001");
        MEDIOS_PAGO_CATALOGO_09.put("deposito en cuenta", "002");
        MEDIOS_PAGO_CATALOGO_09.put("transferencia", "003");
        MEDIOS_PAGO_CATALOGO_09.put("transferencia de credito", "003");
        MEDIOS_PAGO_CATALOGO_09.put("ticket", "005");
        MEDIOS_PAGO_CATALOGO_09.put("tarjeta", "006");
        MEDIOS_PAGO_CATALOGO_09.put("tarjeta de credito", "006");
        MEDIOS_PAGO_CATALOGO_09.put("tarjeta de debito", "006");
        MEDIOS_PAGO_CATALOGO_09.put("yape", "009");
        MEDIOS_PAGO_CATALOGO_09.put("plin", "009");
        MEDIOS_PAGO_CATALOGO_09.put("billetera digital", "011");
        MEDIOS_PAGO_CATALOGO_09.put("pago contra entrega", "001");
        MEDIOS_PAGO_CATALOGO_09.put("credito empresarial", "001");
        MEDIOS_PAGO_CATALOGO_09.put("paypal", "003");

        MEDIO_PAGO_BD.put("001", "MP001");
        MEDIO_PAGO_BD.put("002", "MP012");
        MEDIO_PAGO_BD.put("003", "MP006");
        MEDIO_PAGO_BD.put("005", "MP001");
        MEDIO_PAGO_BD.put("006", "MP002");
        MEDIO_PAGO_BD.put("009", "MP004");
        MEDIO_PAGO_BD.put("011", "MP008");
    }

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final CorrelativoRepository correlativoRepository;
    private final ClienteRepository clienteRepository;
    private final PersonaRepository personaRepository;
    private final EmisorService emisorService;
    private final TotalesCalculator totalesCalculator;
    private final UblXmlGenerator ublXmlGenerator;
    private final HashService hashService;
    private final QrCodeService qrCodeService;
    private final ObjectMapper objectMapper;

    public ComprobanteServiceImpl(VentaRepository ventaRepository,
                                  ProductoRepository productoRepository,
                                  CorrelativoRepository correlativoRepository,
                                  ClienteRepository clienteRepository,
                                  PersonaRepository personaRepository,
                                  EmisorService emisorService,
                                  TotalesCalculator totalesCalculator,
                                  UblXmlGenerator ublXmlGenerator,
                                  HashService hashService,
                                  QrCodeService qrCodeService,
                                  ObjectMapper objectMapper) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.correlativoRepository = correlativoRepository;
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
        this.emisorService = emisorService;
        this.totalesCalculator = totalesCalculator;
        this.ublXmlGenerator = ublXmlGenerator;
        this.hashService = hashService;
        this.qrCodeService = qrCodeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ComprobanteRespuestaDTO emitir(ComprobanteRequestDTO request) {
        if (request == null) {
            throw new ValidacionComprobanteException("No se recibió la solicitud de emisión.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidacionComprobanteException("El comprobante debe tener al menos un item.");
        }

        ReceptorDTO receptor = validarReceptor(request.getReceptor());

        EmisorDTO emisor = emisorService.obtenerEmisor();
        BigDecimal igvPorcentaje = emisorService.obtenerIgvPorcentaje();
        String moneda = emisorService.obtenerMoneda();
        String tipoComprobante = normalizarTipoComprobante(request.getTipoComprobante());
        String serie = resolverSerie(request.getSerie(), tipoComprobante);

        List<DetalleComprobanteDTO> detalles = construirDetalles(request);
        TotalesDTO totales = totalesCalculator.calcular(detalles, igvPorcentaje);
        totales.setTotalEnLetras(NumerosALetras.convertir(totales.getMontoTotalPagar()));
        validarTipoComprobante(tipoComprobante, receptor, totales);

        List<MedioPagoDTO> mediosPago = construirMediosPago(request, totales);
        List<LeyendaDTO> leyendas = construirLeyendas(emisor, totales, tipoComprobante);

        // Correlativo correlativo y sin saltos: se bloquea la fila de la serie.
        Correlativo correlativo = correlativoRepository.findBySerieForUpdate(serie)
                .orElseGet(() -> new Correlativo(serie, emisorService.obtenerIdTipoDocumento(tipoComprobante), 0));
        int numero = correlativo.siguienteNumero();
        correlativoRepository.save(correlativo);

        LocalDateTime emision = LocalDateTime.now();
        ComprobanteRespuestaDTO comprobante = new ComprobanteRespuestaDTO();
        comprobante.setIdVenta(java.util.UUID.randomUUID().toString());
        comprobante.setTipoComprobante(tipoComprobante);
        comprobante.setTipoComprobanteDescripcion(emisorService.obtenerDescripcionTipoComprobante(tipoComprobante));
        comprobante.setSerie(serie);
        comprobante.setCorrelativo(numero);
        comprobante.setNumeroDocumento(serie + "-" + String.format("%06d", numero));
        comprobante.setFechaEmision(ublXmlGenerator.fechaIso(emision));
        comprobante.setHoraEmision(ublXmlGenerator.horaIso(emision));
        comprobante.setMoneda(moneda);
        comprobante.setCondicionOperacion(normalizarCondicion(request.getCondicionOperacion()));
        comprobante.setFormaPago(normalizarFormaPago(request.getFormaPago()));
        comprobante.setNombreComercio(emisor.getNombreComercial());
        comprobante.setEmisor(emisor);
        comprobante.setReceptor(receptor);
        comprobante.setEntrega(request.getEntrega());
        comprobante.setDetalles(detalles);
        comprobante.setTotales(totales);
        comprobante.setLeyendas(leyendas);
        comprobante.setMediosPago(mediosPago);

        // XML UBL 2.1, hash, Valor Resumen y codigo QR.
        String xml = ublXmlGenerator.generar(comprobante);
        String hash = hashService.calcularHashXml(xml);
        String valorResumen = hashService.calcularValorResumen(xml);
        String codigoQr = hashService.construirContenidoQr(emisor.getRuc(), tipoComprobante, serie, numero,
                totales.getIgv(), totales.getMontoTotalPagar(), comprobante.getFechaEmision(),
                receptor.getTipoDocumento(), receptor.getNumeroDocumento(), valorResumen);

        comprobante.setHashComprobante(hash);
        comprobante.setValorResumen(valorResumen);
        comprobante.setCodigoQr(codigoQr);
        comprobante.setQrPngBase64(qrCodeService.generarPngBase64(codigoQr));
        comprobante.setNombreArchivoXml(ublXmlGenerator.nombreArchivoXml(emisor.getRuc(), tipoComprobante, serie, numero));
        comprobante.setEstado(ESTADO_EMITIDO);
        comprobante.setEstadoSunat(ESTADO_SUNAT_GENERADO);

        persistir(comprobante, request, detalles, emision);
        return comprobante;
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteRespuestaDTO obtenerPorId(String idVenta) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante " + idVenta + " no encontrado"));
        return mapear(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteRespuestaDTO> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return List.of();
        }
        return ventaRepository.findByIdClienteOrderByFechaEmisionDesc(idCliente).stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String generarXml(String idVenta) {
        return ublXmlGenerator.generar(obtenerPorId(idVenta));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(String idVenta) {
        ComprobanteRespuestaDTO comprobante = obtenerPorId(idVenta);
        try {
            return ComprobantePdfGenerator.generarPdfBytes(comprobante);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la representacion impresa del comprobante", e);
        }
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private ReceptorDTO validarReceptor(ReceptorDTO receptor) {
        if (receptor == null) {
            throw new ValidacionComprobanteException("Se requiere los datos del adquirente del comprobante.");
        }
        if (receptor.getTipoDocumento() == null || receptor.getTipoDocumento().isBlank()) {
            receptor.setTipoDocumento(TIPO_DOCUMENTO_DNI);
        }
        String documento = receptor.getNumeroDocumento() == null ? "" : receptor.getNumeroDocumento().trim();
        if (documento.isEmpty()) {
            throw new ValidacionComprobanteException("El numero de documento del adquirente es obligatorio.");
        }
        if (TIPO_DOCUMENTO_RUC.equals(receptor.getTipoDocumento())) {
            if (!documento.matches(REGEX_RUC)) {
                throw new ValidacionComprobanteException("El RUC del adquirente debe tener 11 digitos.");
            }
        } else if (TIPO_DOCUMENTO_DNI.equals(receptor.getTipoDocumento())) {
            if (!documento.matches(REGEX_DNI)) {
                throw new ValidacionComprobanteException("El DNI del adquirente debe tener 8 digitos.");
            }
        }
        if (receptor.getRazonSocial() == null || receptor.getRazonSocial().isBlank()) {
            throw new ValidacionComprobanteException("El nombre o razon social del adquirente es obligatorio.");
        }
        if (receptor.getDomicilioFiscal() != null) {
            DomicilioDTO domicilio = receptor.getDomicilioFiscal();
            if (domicilio.getPais() == null || domicilio.getPais().isBlank()) {
                domicilio.setPais("PE");
            }
        }
        return receptor;
    }

    private void validarTipoComprobante(String tipoComprobante, ReceptorDTO receptor, TotalesDTO totales) {
        BigDecimal limite = emisorService.obtenerLimiteBoletaSinRuc();
        if (ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante)) {
            if (!TIPO_DOCUMENTO_RUC.equals(receptor.getTipoDocumento())) {
                throw new ValidacionComprobanteException(
                        "Para emitir una factura el adquirente debe identificarse con RUC.");
            }
            return;
        }
        if (!TIPO_DOCUMENTO_RUC.equals(receptor.getTipoDocumento())
                && totales.getMontoTotalPagar().compareTo(limite) > 0) {
            throw new ValidacionComprobanteException("El monto " + totales.getMontoTotalPagar()
                    + " supera el limite de " + limite + " para boletas sin RUC. Emita una factura.");
        }
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    private List<DetalleComprobanteDTO> construirDetalles(ComprobanteRequestDTO request) {
        List<DetalleComprobanteDTO> detalles = new ArrayList<>();
        int numero = 0;

        for (ItemComprobanteDTO item : request.getItems()) {
            if (item == null || item.getIdProducto() == null) {
                throw new ValidacionComprobanteException("Cada item debe indicar el producto del catalogo.");
            }
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new ValidacionComprobanteException(
                            "El producto " + item.getIdProducto() + " no existe en el catalogo."));

            int cantidad = item.getCantidad() == null ? 0 : item.getCantidad().intValue();
            if (cantidad <= 0) {
                throw new ValidacionComprobanteException("La cantidad de cada item debe ser mayor que cero.");
            }
            if (producto.getStock() < cantidad) {
                throw new StockInsuficienteException("Stock insuficiente para " + producto.getNombre()
                        + " (disponible: " + producto.getStock() + ")");
            }

            // El precio y la afectacion del IGV salen del catalogo: el precio que
            // envia el frontend nunca es el que se factura.
            BigDecimal precio = producto.getPrecio();
            BigDecimal descuento = item.getDescuento() == null ? BigDecimal.ZERO : item.getDescuento();
            BigDecimal maximoDescuento = precio.multiply(BigDecimal.valueOf(cantidad));
            if (descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(maximoDescuento) > 0) {
                throw new ValidacionComprobanteException("El descuento del item " + producto.getNombre()
                        + " no puede ser negativo ni superar el importe de la linea.");
            }

            DetalleComprobanteDTO detalle = new DetalleComprobanteDTO();
            detalle.setNumero(++numero);
            detalle.setIdProducto(producto.getIdProducto());
            detalle.setCodigo(producto.getCodigo());
            detalle.setDescripcion(producto.getNombre());
            detalle.setCantidad(BigDecimal.valueOf(cantidad));
            detalle.setUnidadMedida(producto.getUnidadMedida());
            detalle.setPrecioUnitario(precio);
            detalle.setDescuento(descuento);
            detalle.setAfectacionIgv(producto.getAfectacionIgv());
            detalles.add(detalle);

            producto.setStock(producto.getStock() - cantidad);
            productoRepository.save(producto);
        }
        return detalles;
    }

    // ------------------------------------------------------------------
    // Medios de pago y leyendas
    // ------------------------------------------------------------------

    private List<MedioPagoDTO> construirMediosPago(ComprobanteRequestDTO request, TotalesDTO totales) {
        List<MedioPagoDTO> medios = new ArrayList<>();

        if (request.getMediosPago() != null) {
            for (MedioPagoDTO medio : request.getMediosPago()) {
                if (medio == null) {
                    continue;
                }
                String codigo = resolverCodigoMedioPago(medio);
                if (codigo == null) {
                    continue;
                }
                MedioPagoDTO normalizado = new MedioPagoDTO();
                normalizado.setCodigo(codigo);
                normalizado.setDescripcion(medio.getDescripcion() == null ? codigo : medio.getDescripcion());
                normalizado.setMonto(medio.getMonto() == null
                        ? totales.getMontoTotalPagar() : medio.getMonto().setScale(2, RoundingMode.HALF_UP));
                normalizado.setReferencia(medio.getReferencia());
                medios.add(normalizado);
            }
        }

        if (medios.isEmpty()) {
            MedioPagoDTO efectivo = new MedioPagoDTO();
            efectivo.setCodigo(MEDIO_PAGO_EFECTIVO);
            efectivo.setDescripcion("Efectivo");
            efectivo.setMonto(totales.getMontoTotalPagar());
            medios.add(efectivo);
        }

        BigDecimal suma = medios.stream()
                .map(MedioPagoDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(totales.getMontoTotalPagar()) != 0) {
            throw new ValidacionComprobanteException("La suma de los medios de pago (" + suma
                    + ") no coincide con el total del comprobante (" + totales.getMontoTotalPagar() + ").");
        }
        return medios;
    }

    private String resolverCodigoMedioPago(MedioPagoDTO medio) {
        if (medio.getCodigo() != null && medio.getCodigo().trim().matches("\\d{3}")) {
            return medio.getCodigo().trim();
        }
        String descripcion = medio.getDescripcion() == null ? "" : normalizarTexto(medio.getDescripcion());
        String codigo = MEDIOS_PAGO_CATALOGO_09.get(descripcion);
        if (codigo == null && medio.getCodigo() != null) {
            codigo = MEDIOS_PAGO_CATALOGO_09.get(normalizarTexto(medio.getCodigo()));
        }
        return codigo;
    }

    private List<LeyendaDTO> construirLeyendas(EmisorDTO emisor, TotalesDTO totales, String tipoComprobante) {
        List<LeyendaDTO> leyendas = new ArrayList<>();

        leyendas.add(new LeyendaDTO(LEYENDA_MONTO_EN_LETRAS,
                "Son: " + NumerosALetras.convertir(totales.getMontoTotalPagar())));

        if (totales.getOpGratuita() != null
                && totales.getOpGratuita().compareTo(BigDecimal.ZERO) > 0) {
            leyendas.add(new LeyendaDTO(LEYENDA_TRANSFERENCIA_GRATUITA,
                    "TRANSFERENCIA GRATUITA DE UN BIEN Y/O SERVICIO PRESTADO GRATUITAMENTE"));
        }

        leyendas.add(new LeyendaDTO(null, "Representacion impresa de " + emisor.getRazonSocial()
                + ", consulte en " + emisorService.obtenerUrlRepresentacionImpresa()));

        leyendas.add(new LeyendaDTO(null, ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante)
                ? "Factura electronica" : "Boleta de venta electronica"));

        return leyendas;
    }

    // ------------------------------------------------------------------
    // Persistencia
    // ------------------------------------------------------------------

    private void persistir(ComprobanteRespuestaDTO comprobante, ComprobanteRequestDTO request,
                            List<DetalleComprobanteDTO> detalles, LocalDateTime emision) {
        Venta venta = new Venta();
        venta.setId(comprobante.getIdVenta());
        venta.setIdCliente(resolverIdCliente(request.getIdCliente()));
        venta.setIdPersona(resolverIdPersona(venta.getIdCliente()));
        venta.setIdTipoDocumento(emisorService.obtenerIdTipoDocumento(comprobante.getTipoComprobante()));
        venta.setNumeroDocumento(comprobante.getNumeroDocumento());
        venta.setIdMedioPago(idMedioPagoBd(comprobante.getMediosPago()));
        venta.setFechaEmision(Date.from(emision.atZone(ZoneId.systemDefault()).toInstant()));

        venta.setSerie(comprobante.getSerie());
        venta.setCorrelativo(comprobante.getCorrelativo());
        venta.setMoneda(comprobante.getMoneda());
        venta.setCondicionOperacion(comprobante.getCondicionOperacion());
        venta.setFormaPago(comprobante.getFormaPago());

        TotalesDTO totales = comprobante.getTotales();
        venta.setTotalVenta(totales.getTotalVenta());
        venta.setTotalGravada(totales.getOpGravada());
        venta.setTotalInafecta(totales.getOpInafecta());
        venta.setTotalExonerada(totales.getOpExonerada());
        venta.setTotalGratuita(totales.getOpGratuita());
        venta.setTotalIgv(totales.getIgv());
        venta.setTotalDescuentos(totales.getTotalDescuentos());
        venta.setMontoTotalCredito(ComprobanteRequestDTO.FORMA_PAGO_CREDITO.equals(comprobante.getFormaPago())
                ? totales.getMontoTotalPagar() : BigDecimal.ZERO);

        ReceptorDTO receptor = comprobante.getReceptor();
        venta.setReceptorTipoDocumento(valor(receptor.getTipoDocumento()));
        venta.setReceptorNumDocumento(valor(receptor.getNumeroDocumento()));
        venta.setReceptorRazonSocial(valor(receptor.getRazonSocial()));
        DomicilioDTO domicilio = receptor.getDomicilioFiscal();
        if (domicilio != null) {
            venta.setReceptorDireccion(valor(domicilio.getDireccion()));
            venta.setReceptorUbigeo(valor(domicilio.getUbigeo()));
            venta.setReceptorDistrito(valor(domicilio.getDistrito()));
            venta.setReceptorProvincia(valor(domicilio.getProvincia()));
            venta.setReceptorDepartamento(valor(domicilio.getDepartamento()));
            venta.setReceptorPais(valor(domicilio.getPais()));
        } else {
            venta.setReceptorDireccion("");
            venta.setReceptorUbigeo("");
            venta.setReceptorDistrito("");
            venta.setReceptorProvincia("");
            venta.setReceptorDepartamento("");
            venta.setReceptorPais("PE");
        }

        DomicilioDTO entrega = comprobante.getEntrega();
        if (entrega != null) {
            venta.setEntregaDireccion(valor(entrega.getDireccion()));
            venta.setEntregaUbigeo(valor(entrega.getUbigeo()));
        } else {
            venta.setEntregaDireccion("");
            venta.setEntregaUbigeo("");
        }
        venta.setEmailContacto(valor(request.getEmailContacto() != null ? request.getEmailContacto() : receptor.getCorreo()));

        venta.setHashComprobante(comprobante.getHashComprobante());
        venta.setCodigoQr(comprobante.getCodigoQr());
        venta.setXmlName(comprobante.getNombreArchivoXml());
        venta.setEstado(comprobante.getEstado());
        venta.setEstadoSunat(comprobante.getEstadoSunat());
        venta.setLeyendas(serializarLeyendas(comprobante.getLeyendas()));
        venta.setObservacion(valor(request.getObservacion()));

        for (DetalleComprobanteDTO detalle : detalles) {
            DetalleVenta item = new DetalleVenta();
            item.setIdProducto(detalle.getIdProducto());
            item.setCantidad(detalle.getCantidad().intValue());
            item.setPrecioUnitario(detalle.getPrecioUnitario());
            item.setSubtotal(detalle.getTotalItem());
            item.setValorUnitario(detalle.getValorUnitario());
            item.setIgvItem(detalle.getIgv());
            item.setDescuento(detalle.getDescuento());
            item.setCodigoItem(valor(detalle.getCodigo()));
            item.setDescripcion(valor(detalle.getDescripcion()));
            item.setUnidadMedida(valor(detalle.getUnidadMedida()));
            item.setAfectacionIgv(valor(detalle.getAfectacionIgv()));
            item.setProducto(valor(detalle.getDescripcion()));
            venta.agregarDetalle(item);
        }

        ventaRepository.save(venta);
    }

    private String resolverIdCliente(String idCliente) {
        if (idCliente != null && !idCliente.isBlank()
                && clienteRepository.findById(idCliente).isPresent()) {
            return idCliente;
        }
        // Venta de mostrador (consumidor final): se atribuye al cliente por
        // defecto configurado en `parametro`.
        String mostrador = emisorService.obtenerClienteMostrador();
        if (mostrador != null && clienteRepository.findById(mostrador).isPresent()) {
            return mostrador;
        }
        throw new ValidacionComprobanteException("No se pudo determinar el cliente de la venta.");
    }

    private String resolverIdPersona(String idCliente) {
        return personaRepository.findIdPersonaByClienteId(idCliente)
                .orElseGet(() -> emisorService.obtenerPersonaMostrador());
    }

    private String idMedioPagoBd(List<MedioPagoDTO> medios) {
        if (medios == null || medios.isEmpty()) {
            return "MP001";
        }
        String id = MEDIO_PAGO_BD.get(medios.get(0).getCodigo());
        return id == null ? "MP001" : id;
    }

    // ------------------------------------------------------------------
    // Mapeo desde la base de datos
    // ------------------------------------------------------------------

    private ComprobanteRespuestaDTO mapear(Venta venta) {
        ComprobanteRespuestaDTO comprobante = new ComprobanteRespuestaDTO();
        comprobante.setIdVenta(venta.getId());
        comprobante.setTipoComprobante(ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoDocumento(venta))
                ? ComprobanteRequestDTO.TIPO_FACTURA : ComprobanteRequestDTO.TIPO_BOLETA);
        comprobante.setTipoComprobanteDescripcion(emisorService.obtenerDescripcionTipoComprobante(comprobante.getTipoComprobante()));
        comprobante.setSerie(venta.getSerie());
        comprobante.setCorrelativo(venta.getCorrelativo());
        comprobante.setNumeroDocumento(venta.getNumeroDocumento());
        comprobante.setMoneda(venta.getMoneda());
        comprobante.setCondicionOperacion(venta.getCondicionOperacion());
        comprobante.setFormaPago(venta.getFormaPago());
        comprobante.setNombreComercio(emisorService.obtenerEmisor().getNombreComercial());
        comprobante.setEmisor(emisorService.obtenerEmisor());
        comprobante.setEstado(venta.getEstado());
        comprobante.setEstadoSunat(venta.getEstadoSunat());
        comprobante.setObservacion(venta.getObservacion());
        comprobante.setHashComprobante(venta.getHashComprobante());
        comprobante.setValorResumen(hashService.valorResumenDesdeHash(venta.getHashComprobante()));
        comprobante.setCodigoQr(venta.getCodigoQr());
        comprobante.setNombreArchivoXml(venta.getXmlName());

        LocalDateTime emision = venta.getFechaEmision() == null ? LocalDateTime.now()
                : venta.getFechaEmision().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        comprobante.setFechaEmision(ublXmlGenerator.fechaIso(emision));
        comprobante.setHoraEmision(ublXmlGenerator.horaIso(emision));

        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento(venta.getReceptorTipoDocumento());
        receptor.setNumeroDocumento(venta.getReceptorNumDocumento());
        receptor.setRazonSocial(venta.getReceptorRazonSocial());
        DomicilioDTO domicilio = new DomicilioDTO();
        domicilio.setDireccion(venta.getReceptorDireccion());
        domicilio.setUbigeo(venta.getReceptorUbigeo());
        domicilio.setDistrito(venta.getReceptorDistrito());
        domicilio.setProvincia(venta.getReceptorProvincia());
        domicilio.setDepartamento(venta.getReceptorDepartamento());
        domicilio.setPais(venta.getReceptorPais());
        receptor.setDomicilioFiscal(domicilio);
        comprobante.setReceptor(receptor);

        if (venta.getEntregaDireccion() != null && !venta.getEntregaDireccion().isBlank()) {
            DomicilioDTO entrega = new DomicilioDTO();
            entrega.setDireccion(venta.getEntregaDireccion());
            entrega.setUbigeo(venta.getEntregaUbigeo());
            comprobante.setEntrega(entrega);
        }

        List<DetalleComprobanteDTO> detalles = new ArrayList<>();
        int numero = 0;
        if (venta.getDetalles() != null) {
            for (DetalleVenta item : venta.getDetalles()) {
                DetalleComprobanteDTO detalle = new DetalleComprobanteDTO();
                detalle.setNumero(++numero);
                detalle.setCodigo(item.getCodigoItem());
                detalle.setDescripcion(item.getDescripcion());
                detalle.setCantidad(BigDecimal.valueOf(item.getCantidad()));
                detalle.setUnidadMedida(item.getUnidadMedida());
                detalle.setValorUnitario(item.getValorUnitario());
                detalle.setPrecioUnitario(item.getPrecioUnitario());
                detalle.setDescuento(item.getDescuento());
                detalle.setAfectacionIgv(item.getAfectacionIgv());
                detalle.setIgv(item.getIgvItem());
                detalle.setTotalItem(item.getSubtotal());
                detalle.setBase(item.getSubtotal().subtract(item.getIgvItem()).setScale(2, RoundingMode.HALF_UP));
                detalles.add(detalle);
            }
        }
        comprobante.setDetalles(detalles);

        TotalesDTO totales = new TotalesDTO();
        totales.setOpGravada(venta.getTotalGravada());
        totales.setOpInafecta(venta.getTotalInafecta());
        totales.setOpExonerada(venta.getTotalExonerada());
        totales.setOpGratuita(venta.getTotalGratuita());
        totales.setIgv(venta.getTotalIgv());
        totales.setTotalDescuentos(venta.getTotalDescuentos());
        totales.setTotalVenta(venta.getTotalVenta());
        totales.setMontoTotalPagar(venta.getTotalVenta());
        totales.setMontoTotalCredito(venta.getMontoTotalCredito());
        totales.setTotalEnLetras(NumerosALetras.convertir(venta.getTotalVenta()));
        comprobante.setTotales(totales);

        comprobante.setLeyendas(deserializarLeyendas(venta.getLeyendas()));
        comprobante.setMediosPago(mediosPagoDesdeVenta(venta));
        if (comprobante.getCodigoQr() != null) {
            comprobante.setQrPngBase64(qrCodeService.generarPngBase64(comprobante.getCodigoQr()));
        }
        return comprobante;
    }

    private List<MedioPagoDTO> mediosPagoDesdeVenta(Venta venta) {
        List<MedioPagoDTO> medios = new ArrayList<>();
        String codigo = MEDIO_PAGO_EFECTIVO;
        for (Map.Entry<String, String> entry : MEDIO_PAGO_BD.entrySet()) {
            if (entry.getValue().equals(venta.getIdMedioPago())) {
                codigo = entry.getKey();
                break;
            }
        }
        MedioPagoDTO medio = new MedioPagoDTO();
        medio.setCodigo(codigo);
        medio.setDescripcion(venta.getMedioPago() == null ? codigo : venta.getMedioPago());
        medio.setMonto(venta.getTotalVenta());
        medios.add(medio);
        return medios;
    }

    private String tipoDocumento(Venta venta) {
        return "TD002".equals(venta.getIdTipoDocumento())
                ? ComprobanteRequestDTO.TIPO_FACTURA : ComprobanteRequestDTO.TIPO_BOLETA;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private String normalizarTipoComprobante(String tipo) {
        if (ComprobanteRequestDTO.TIPO_FACTURA.equals(tipo) || ComprobanteRequestDTO.TIPO_BOLETA.equals(tipo)) {
            return tipo;
        }
        throw new ValidacionComprobanteException("El tipo de comprobante debe ser 01 (factura) o 03 (boleta).");
    }

    private String resolverSerie(String serieSolicitada, String tipoComprobante) {
        String serie = serieSolicitada == null || serieSolicitada.isBlank()
                ? emisorService.obtenerSerie(tipoComprobante) : serieSolicitada.trim().toUpperCase();
        if (!serie.matches("[A-Z0-9]{4}")) {
            throw new ValidacionComprobanteException("La serie del comprobante debe tener 4 caracteres alfanumericos.");
        }
        return serie;
    }

    private String normalizarCondicion(String condicion) {
        return ComprobanteRequestDTO.CONDICION_CREDITO.equals(condicion)
                ? ComprobanteRequestDTO.CONDICION_CREDITO : ComprobanteRequestDTO.CONDICION_CONTADO;
    }

    private String normalizarFormaPago(String formaPago) {
        return ComprobanteRequestDTO.FORMA_PAGO_CREDITO.equals(formaPago)
                ? ComprobanteRequestDTO.FORMA_PAGO_CREDITO : ComprobanteRequestDTO.FORMA_PAGO_CONTADO;
    }

    private String normalizarTexto(String texto) {
        return texto.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .replace("/", " ").trim();
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private String serializarLeyendas(List<LeyendaDTO> leyendas) {
        try {
            return objectMapper.writeValueAsString(leyendas);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron guardar las leyendas del comprobante", e);
        }
    }

    private List<LeyendaDTO> deserializarLeyendas(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<LeyendaDTO>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
