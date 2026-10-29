package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ArqueoComprobanteDTO;
import com.example.proyec_herramientas.model.ArqueoDTO;
import com.example.proyec_herramientas.model.ArqueoGrupoDTO;
import com.example.proyec_herramientas.model.ArqueoTurnoDTO;
import com.example.proyec_herramientas.model.ArqueoVendedorDTO;
import com.example.proyec_herramientas.persistence.MedioPago;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.reportes.ArqueoPdfGenerator;
import com.example.proyec_herramientas.reportes.MarcaComprobante;
import com.example.proyec_herramientas.repository.MedioPagoRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Corte de caja diario y mensual.
 *
 * <p>Los ingresos se atribuyen a la tienda por los productos del detalle, igual
 * que el resto de consultas del panel. El vendedor y su nombre son el snapshot
 * guardado en el comprobante al vender, no la cuenta actual: si una cuenta se
 * renombra, el arqueo historico no cambia. Las ventas anteriores a la
 * atribucion (o las autocompras del CLIENTE) se agrupan como
 * "Sin vendedor asignado".</p>
 */
@Service
public class ArqueoServiceImpl implements ArqueoService {

    private static final String PERIODO_DIARIO = "DIARIO";
    private static final String PERIODO_MENSUAL = "MENSUAL";

    private static final String GRUPO_EFECTIVO = "Efectivo";
    private static final String GRUPO_TARJETA = "Tarjeta";
    private static final String GRUPO_YAPE_PLIN = "Yape/Plin";
    private static final String GRUPO_CREDITO = "Créditos";
    private static final String GRUPO_OTROS = "Otros medios";

    /** Grupos que el corte muestra siempre, aunque no tengan ventas. */
    private static final List<String> GRUPOS_FIJOS = List.of(GRUPO_EFECTIVO, GRUPO_TARJETA,
            GRUPO_YAPE_PLIN, GRUPO_CREDITO);

    /** Catalogo de medios de pago (`medio_pago`) -> grupo del arqueo. */
    private static final Map<String, String> GRUPO_POR_MEDIO = Map.ofEntries(
            Map.entry("MP001", GRUPO_EFECTIVO),
            Map.entry("MP002", GRUPO_TARJETA),
            Map.entry("MP003", GRUPO_TARJETA),
            Map.entry("MP009", GRUPO_TARJETA),
            Map.entry("MP010", GRUPO_TARJETA),
            Map.entry("MP011", GRUPO_TARJETA),
            Map.entry("MP004", GRUPO_YAPE_PLIN),
            Map.entry("MP005", GRUPO_YAPE_PLIN),
            Map.entry("MP013", GRUPO_CREDITO));

    /** Forma de pago a credito (catalogo 09: 02 = credito). */
    private static final String FORMA_PAGO_CREDITO = "02";

    private static final String TURNO_MANANA = "Mañana";
    private static final String TURNO_TARDE = "Tarde";
    private static final String TURNO_NOCHE = "Noche";
    private static final List<String> TURNOS = List.of(TURNO_MANANA, TURNO_TARDE, TURNO_NOCHE);
    private static final Map<String, String> RANGO_TURNO = Map.of(
            TURNO_MANANA, "06:00 – 13:59",
            TURNO_TARDE, "14:00 – 20:59",
            TURNO_NOCHE, "21:00 – 05:59");

    private static final String SIN_VENDEDOR = "Sin vendedor asignado";
    private static final int ESCALA_IMPORTE = 2;

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VentaRepository ventaRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final TiendaRepository tiendaRepository;

    public ArqueoServiceImpl(VentaRepository ventaRepository,
                             MedioPagoRepository medioPagoRepository,
                             UsuarioRepository usuarioRepository,
                             PersonaRepository personaRepository,
                             TiendaRepository tiendaRepository) {
        this.ventaRepository = ventaRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ArqueoDTO generar(int idTienda, String periodo, LocalDate fecha) {
        String periodoNormalizado = normalizarPeriodo(periodo);
        Rango rango = rango(periodoNormalizado, fecha);
        Tienda tienda = tiendaRepository.findById(idTienda).orElse(null);

        List<Venta> ventas = ventaRepository.ventasPorTiendaEntre(
                Date.from(rango.desde().atStartOfDay(ZONA).toInstant()),
                Date.from(rango.hasta().plusDays(1).atStartOfDay(ZONA).toInstant()),
                idTienda);
        Map<String, String> descripciones = medioPagoRepository.findAll().stream()
                .collect(Collectors.toMap(MedioPago::getIdMedioPago, MedioPago::getDescripcion));

        ArqueoDTO arqueo = new ArqueoDTO();
        arqueo.setTiendaNombre(tienda == null ? "" : tienda.getNombre());
        arqueo.setTiendaRuc(tienda == null || tienda.getRuc() == null ? "" : tienda.getRuc());
        arqueo.setPeriodo(periodoNormalizado);
        arqueo.setDesde(rango.desde().toString());
        arqueo.setHasta(rango.hasta().toString());
        arqueo.setGeneradoEn(LocalDateTime.now(ZONA).format(GENERADO));

        BigDecimal totalVentas = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            totalVentas = totalVentas.add(totalDe(venta));
        }
        arqueo.setTotalVentas(totalVentas);
        arqueo.setComprobantes(ventas.size());
        arqueo.setTicketPromedio(ventas.isEmpty() ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(ventas.size()), ESCALA_IMPORTE, RoundingMode.HALF_UP));

        arqueo.setMetodosPago(gruposDePago(ventas, descripciones, totalVentas));
        arqueo.setVendedores(vendedoresDe(idTienda, ventas));
        arqueo.setTurnos(turnosDe(ventas));
        arqueo.setDetalle(detalleDe(ventas, descripciones));
        return arqueo;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarCsv(int idTienda, String periodo, LocalDate fecha) {
        ArqueoDTO arqueo = generar(idTienda, periodo, fecha);
        String contenido = "\uFEFF" + csv(arqueo);
        return contenido.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarPdf(int idTienda, String periodo, LocalDate fecha) {
        ArqueoDTO arqueo = generar(idTienda, periodo, fecha);
        try {
            return ArqueoPdfGenerator.generarPdfBytes(arqueo, marcaDe(idTienda));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del corte de caja", e);
        }
    }

    @Override
    public String nombreArchivo(String periodo, LocalDate fecha, String extension) {
        String periodoNormalizado = normalizarPeriodo(periodo);
        Rango rango = rango(periodoNormalizado, fecha);
        String sufijo = PERIODO_MENSUAL.equals(periodoNormalizado)
                ? rango.desde().format(MES)
                : rango.desde().toString();
        String prefijo = PERIODO_MENSUAL.equals(periodoNormalizado) ? "mensual" : "diario";
        return "arqueo-" + prefijo + "-" + sufijo + "." + extension;
    }

    private MarcaComprobante marcaDe(int idTienda) {
        return tiendaRepository.findById(idTienda)
                .map(tienda -> new MarcaComprobante(tienda.getColorPrimario(), tienda.getColorSecundario(),
                        tienda.getLogoUrl()))
                .orElseGet(MarcaComprobante::porDefecto);
    }

    // ------------------------------------------------------------------
    // Agrupaciones
    // ------------------------------------------------------------------

    private List<ArqueoGrupoDTO> gruposDePago(List<Venta> ventas, Map<String, String> descripciones,
                                              BigDecimal totalVentas) {
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        Map<String, Long> conteos = new LinkedHashMap<>();
        Map<String, Set<String>> metodos = new LinkedHashMap<>();
        for (String grupo : GRUPOS_FIJOS) {
            totales.put(grupo, BigDecimal.ZERO);
            conteos.put(grupo, 0L);
            metodos.put(grupo, new LinkedHashSet<>());
        }

        for (Venta venta : ventas) {
            String grupo = grupoDe(venta);
            totales.merge(grupo, totalDe(venta), BigDecimal::add);
            conteos.merge(grupo, 1L, Long::sum);
            metodos.computeIfAbsent(grupo, clave -> new LinkedHashSet<>())
                    .add(descripcionMedio(venta, descripciones));
        }

        List<ArqueoGrupoDTO> grupos = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entrada : totales.entrySet()) {
            String grupo = entrada.getKey();
            // "Otros medios" solo se lista cuando tuvo movimiento.
            if (GRUPO_OTROS.equals(grupo) && conteos.get(grupo) == 0) {
                continue;
            }
            ArqueoGrupoDTO dto = new ArqueoGrupoDTO();
            dto.setGrupo(grupo);
            dto.setTotal(entrada.getValue());
            dto.setComprobantes(conteos.get(grupo));
            dto.setMetodos(new ArrayList<>(metodos.get(grupo)));
            dto.setPorcentaje(totalVentas.signum() == 0 ? BigDecimal.ZERO
                    : entrada.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(totalVentas, ESCALA_IMPORTE, RoundingMode.HALF_UP));
            grupos.add(dto);
        }
        return grupos;
    }

    private List<ArqueoVendedorDTO> vendedoresDe(int idTienda, List<Venta> ventas) {
        Map<String, Acumulado> acumulados = new LinkedHashMap<>();
        for (Usuario cuenta : usuarioRepository.findByIdTiendaAndIdRolInOrderByIdUsuarioAsc(idTienda,
                List.of(Rol.ADMIN, Rol.VENDEDOR))) {
            if (cuenta.estaSuspendido()) {
                continue;
            }
            acumulados.put(cuenta.getUsuario(),
                    new Acumulado(cuenta.getUsuario(), nombreDe(cuenta)));
        }

        for (Venta venta : ventas) {
            String usuario = venta.getVendedorUsuario() == null ? "" : venta.getVendedorUsuario();
            String nombre = venta.getVendedorNombre() == null || venta.getVendedorNombre().isBlank()
                    ? (usuario.isBlank() ? SIN_VENDEDOR : usuario)
                    : venta.getVendedorNombre();
            Acumulado acumulado = acumulados.computeIfAbsent(usuario, clave -> new Acumulado(usuario, nombre));
            acumulado.sumar(totalDe(venta));
        }

        return acumulados.values().stream()
                .map(Acumulado::aDto)
                .sorted(Comparator.comparing(ArqueoVendedorDTO::getTotal).reversed()
                        .thenComparing(ArqueoVendedorDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<ArqueoTurnoDTO> turnosDe(List<Venta> ventas) {
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        Map<String, Long> conteos = new LinkedHashMap<>();
        for (String turno : TURNOS) {
            totales.put(turno, BigDecimal.ZERO);
            conteos.put(turno, 0L);
        }
        for (Venta venta : ventas) {
            String turno = turnoDe(fechaHora(venta).toLocalTime());
            totales.merge(turno, totalDe(venta), BigDecimal::add);
            conteos.merge(turno, 1L, Long::sum);
        }

        List<ArqueoTurnoDTO> turnos = new ArrayList<>();
        for (String turno : TURNOS) {
            ArqueoTurnoDTO dto = new ArqueoTurnoDTO();
            dto.setTurno(turno);
            dto.setRango(RANGO_TURNO.get(turno));
            dto.setTotal(totales.get(turno));
            dto.setComprobantes(conteos.get(turno));
            turnos.add(dto);
        }
        return turnos;
    }

    private List<ArqueoComprobanteDTO> detalleDe(List<Venta> ventas, Map<String, String> descripciones) {
        List<ArqueoComprobanteDTO> detalle = new ArrayList<>();
        for (Venta venta : ventas) {
            LocalDateTime fechaHora = fechaHora(venta);
            ArqueoComprobanteDTO dto = new ArqueoComprobanteDTO();
            dto.setIdVenta(venta.getId());
            dto.setFechaHora(fechaHora.format(FECHA_HORA));
            dto.setNumeroDocumento(nulo(venta.getNumeroDocumento()));
            dto.setCliente(nulo(venta.getReceptorRazonSocial()));
            dto.setDocumentoCliente(nulo(venta.getReceptorNumDocumento()));
            dto.setMedioPago(descripcionMedio(venta, descripciones));
            dto.setGrupoPago(grupoDe(venta));
            dto.setTurno(turnoDe(fechaHora.toLocalTime()));
            dto.setVendedor(vendedorDe(venta));
            dto.setVendedorUsuario(nulo(venta.getVendedorUsuario()));
            dto.setTotal(totalDe(venta));
            detalle.add(dto);
        }
        return detalle;
    }

    // ------------------------------------------------------------------
    // Exportacion CSV
    // ------------------------------------------------------------------

    private String csv(ArqueoDTO arqueo) {
        StringBuilder texto = new StringBuilder();
        linea(texto, "CORTE DE CAJA Y ARQUEO");
        linea(texto, "Tienda", arqueo.getTiendaNombre());
        linea(texto, "RUC", arqueo.getTiendaRuc());
        linea(texto, "Periodo", arqueo.getPeriodo());
        linea(texto, "Desde", arqueo.getDesde());
        linea(texto, "Hasta", arqueo.getHasta());
        linea(texto, "Generado", arqueo.getGeneradoEn());
        texto.append("\r\n");

        linea(texto, "RESUMEN");
        linea(texto, "Total vendido", dinero(arqueo.getTotalVentas()));
        linea(texto, "Comprobantes", String.valueOf(arqueo.getComprobantes()));
        linea(texto, "Ticket promedio", dinero(arqueo.getTicketPromedio()));
        texto.append("\r\n");

        linea(texto, "INGRESOS POR METODO DE PAGO");
        linea(texto, "Grupo", "Comprobantes", "Porcentaje", "Total");
        for (ArqueoGrupoDTO grupo : arqueo.getMetodosPago()) {
            linea(texto, grupo.getGrupo(), String.valueOf(grupo.getComprobantes()),
                    dinero(grupo.getPorcentaje()), dinero(grupo.getTotal()));
        }
        texto.append("\r\n");

        linea(texto, "TOTAL POR VENDEDOR");
        linea(texto, "Vendedor", "Usuario", "Comprobantes", "Total");
        for (ArqueoVendedorDTO vendedor : arqueo.getVendedores()) {
            linea(texto, vendedor.getNombre(), vendedor.getUsuario(),
                    String.valueOf(vendedor.getComprobantes()), dinero(vendedor.getTotal()));
        }
        texto.append("\r\n");

        linea(texto, "TOTAL POR TURNO");
        linea(texto, "Turno", "Rango", "Comprobantes", "Total");
        for (ArqueoTurnoDTO turno : arqueo.getTurnos()) {
            linea(texto, turno.getTurno(), turno.getRango(),
                    String.valueOf(turno.getComprobantes()), dinero(turno.getTotal()));
        }
        texto.append("\r\n");

        linea(texto, "DETALLE DE COMPROBANTES");
        linea(texto, "Fecha y hora", "Comprobante", "Cliente", "Documento", "Medio de pago",
                "Turno", "Vendedor", "Total");
        for (ArqueoComprobanteDTO comprobante : arqueo.getDetalle()) {
            linea(texto, comprobante.getFechaHora(), comprobante.getNumeroDocumento(),
                    comprobante.getCliente(), comprobante.getDocumentoCliente(),
                    comprobante.getMedioPago(), comprobante.getTurno(), comprobante.getVendedor(),
                    dinero(comprobante.getTotal()));
        }
        return texto.toString();
    }

    private void linea(StringBuilder texto, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                texto.append(',');
            }
            texto.append(campoCsv(campos[i]));
        }
        texto.append("\r\n");
    }

    // RFC 4180: se entrecomilla cuando el valor contiene el separador, comillas
    // o un salto de linea, y las comillas internas se duplican.
    private String campoCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(",") || texto.contains("\"") || texto.contains(";")
                || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private String normalizarPeriodo(String periodo) {
        String valor = periodo == null ? "" : periodo.trim().toUpperCase();
        if (valor.isEmpty()) {
            return PERIODO_DIARIO;
        }
        if (PERIODO_DIARIO.equals(valor) || PERIODO_MENSUAL.equals(valor)) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El periodo debe ser diario o mensual.");
    }

    private Rango rango(String periodo, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(ZONA) : fecha;
        if (PERIODO_MENSUAL.equals(periodo)) {
            return new Rango(dia.withDayOfMonth(1), dia.withDayOfMonth(dia.lengthOfMonth()));
        }
        return new Rango(dia, dia);
    }

    private String grupoDe(Venta venta) {
        if (FORMA_PAGO_CREDITO.equals(venta.getFormaPago())) {
            return GRUPO_CREDITO;
        }
        return GRUPO_POR_MEDIO.getOrDefault(venta.getIdMedioPago(), GRUPO_OTROS);
    }

    private String descripcionMedio(Venta venta, Map<String, String> descripciones) {
        return descripciones.getOrDefault(venta.getIdMedioPago(), nulo(venta.getIdMedioPago()));
    }

    private String vendedorDe(Venta venta) {
        if (venta.getVendedorNombre() != null && !venta.getVendedorNombre().isBlank()) {
            return venta.getVendedorNombre();
        }
        if (venta.getVendedorUsuario() != null && !venta.getVendedorUsuario().isBlank()) {
            return venta.getVendedorUsuario();
        }
        return SIN_VENDEDOR;
    }

    private String turnoDe(LocalTime hora) {
        if (hora.isBefore(LocalTime.of(6, 0))) {
            return TURNO_NOCHE;
        }
        if (hora.isBefore(LocalTime.of(14, 0))) {
            return TURNO_MANANA;
        }
        if (hora.isBefore(LocalTime.of(21, 0))) {
            return TURNO_TARDE;
        }
        return TURNO_NOCHE;
    }

    private LocalDateTime fechaHora(Venta venta) {
        return venta.getFechaEmision() == null
                ? LocalDateTime.now(ZONA)
                : LocalDateTime.ofInstant(venta.getFechaEmision().toInstant(), ZONA);
    }

    private BigDecimal totalDe(Venta venta) {
        return venta.getTotalVenta() == null ? BigDecimal.ZERO : venta.getTotalVenta();
    }

    private String nombreDe(Usuario usuario) {
        return personaRepository.findById(usuario.getIdPersona())
                .map(this::nombreCompleto)
                .filter(nombre -> !nombre.isBlank())
                .orElse(usuario.getUsuario());
    }

    private String nombreCompleto(Persona persona) {
        String nombre = persona.getNombre() == null ? "" : persona.getNombre().trim();
        String apellido = persona.getApellido() == null ? "" : persona.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }

    private String nulo(String valor) {
        return valor == null ? "" : valor;
    }

    private String dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private record Rango(LocalDate desde, LocalDate hasta) {
    }

    private static final class Acumulado {
        private final String usuario;
        private final String nombre;
        private BigDecimal total = BigDecimal.ZERO;
        private long comprobantes;

        private Acumulado(String usuario, String nombre) {
            this.usuario = usuario;
            this.nombre = nombre;
        }

        private void sumar(BigDecimal importe) {
            total = total.add(importe);
            comprobantes++;
        }

        private ArqueoVendedorDTO aDto() {
            ArqueoVendedorDTO dto = new ArqueoVendedorDTO();
            dto.setUsuario(usuario);
            dto.setNombre(nombre);
            dto.setTotal(total);
            dto.setComprobantes(comprobantes);
            return dto;
        }
    }
}
