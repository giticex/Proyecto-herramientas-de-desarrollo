package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.model.AperturaCajaRequest;
import com.example.proyec_herramientas.model.CajaEstadoDTO;
import com.example.proyec_herramientas.model.CajaGrupoDTO;
import com.example.proyec_herramientas.model.CierreCajaRequest;
import com.example.proyec_herramientas.persistence.MovimientoCaja;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.MovimientoCajaRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.example.proyec_herramientas.security.SesionPrincipal;
import com.example.proyec_herramientas.security.Vendedor;
import com.example.proyec_herramientas.security.VendedorResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Apertura, resumen y cierre del turno de caja del vendedor autenticado.
 *
 * <p>La tienda y el usuario salen siempre del token. Las ventas se atribuyen
 * por el snapshot vendedor_usuario del comprobante y se acotan a la tienda por
 * los productos del detalle, igual que el resto de consultas del panel.</p>
 *
 * <p>Las devoluciones del POS no descuentan efectivo: generan un credito a
 * favor del cliente, por lo que el cajon solo cambia con las ventas.</p>
 */
@Service
public class CajaServiceImpl implements CajaService {

    private static final String GRUPO_EFECTIVO = "Efectivo";
    private static final String GRUPO_TARJETA = "Tarjeta";
    private static final String GRUPO_YAPE_PLIN = "Yape/Plin";
    private static final String GRUPO_CREDITO = "Créditos";
    private static final String GRUPO_OTROS = "Otros medios";

    /** Grupos que el arqueo muestra siempre, aunque no tengan ventas. */
    private static final List<String> GRUPOS_FIJOS = List.of(GRUPO_EFECTIVO, GRUPO_TARJETA,
            GRUPO_YAPE_PLIN, GRUPO_CREDITO);

    private static final Map<String, String> GRUPO_POR_MEDIO = Map.ofEntries(
            Map.entry("MP001", GRUPO_EFECTIVO),
            Map.entry("MP002", GRUPO_TARJETA),
            Map.entry("MP003", GRUPO_TARJETA),
            Map.entry("MP004", GRUPO_YAPE_PLIN),
            Map.entry("MP005", GRUPO_YAPE_PLIN),
            Map.entry("MP009", GRUPO_TARJETA),
            Map.entry("MP010", GRUPO_TARJETA),
            Map.entry("MP011", GRUPO_TARJETA),
            Map.entry("MP013", GRUPO_CREDITO));

    private static final String FORMA_PAGO_CREDITO = "02";
    private static final int ESCALA_IMPORTE = 2;
    private static final int MAX_OBSERVACION = 200;

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MovimientoCajaRepository movimientoRepository;
    private final VentaRepository ventaRepository;
    private final TiendaRepository tiendaRepository;
    private final VendedorResolver vendedorResolver;
    private final AuditoriaService auditoriaService;

    public CajaServiceImpl(MovimientoCajaRepository movimientoRepository,
                           VentaRepository ventaRepository,
                           TiendaRepository tiendaRepository,
                           VendedorResolver vendedorResolver,
                           AuditoriaService auditoriaService) {
        this.movimientoRepository = movimientoRepository;
        this.ventaRepository = ventaRepository;
        this.tiendaRepository = tiendaRepository;
        this.vendedorResolver = vendedorResolver;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public CajaEstadoDTO estado(SesionPrincipal sesion) {
        Contexto contexto = contexto(sesion);

        Optional<MovimientoCaja> abierto = movimientoRepository
                .findFirstByIdTiendaAndUsuarioAndEstadoOrderByFechaAperturaDesc(
                        contexto.idTienda(), contexto.vendedor().usuario(),
                        MovimientoCaja.ESTADO_ABIERTA);
        if (abierto.isPresent()) {
            return construir(abierto.get(), contexto, true);
        }

        // El ultimo cierre del dia queda visible para consultar el arqueo final
        // del turno; un cierre de dias anteriores ya no es el turno vigente.
        Date inicioDelDia = Date.from(LocalDate.now(ZONA).atStartOfDay(ZONA).toInstant());
        Optional<MovimientoCaja> cerrado = movimientoRepository
                .findFirstByIdTiendaAndUsuarioAndEstadoAndFechaCierreGreaterThanEqualOrderByFechaCierreDesc(
                        contexto.idTienda(), contexto.vendedor().usuario(),
                        MovimientoCaja.ESTADO_CERRADA, inicioDelDia);
        if (cerrado.isPresent()) {
            return construir(cerrado.get(), contexto, false);
        }

        return sinTurno(contexto);
    }

    @Override
    @Transactional
    public CajaEstadoDTO abrir(SesionPrincipal sesion, AperturaCajaRequest request) {
        Contexto contexto = contexto(sesion);
        Optional<MovimientoCaja> abierto = movimientoRepository
                .findFirstByIdTiendaAndUsuarioAndEstadoOrderByFechaAperturaDesc(
                        contexto.idTienda(), contexto.vendedor().usuario(),
                        MovimientoCaja.ESTADO_ABIERTA);
        if (abierto.isPresent()) {
            MovimientoCaja turno = abierto.get();
            throw new RecursoDuplicadoException("Ya tienes un turno de caja abierto desde el "
                    + fechaHora(turno.getFechaApertura()) + ". Cierralo antes de abrir otro.");
        }

        MovimientoCaja turno = new MovimientoCaja();
        turno.setIdCaja(UUID.randomUUID().toString());
        turno.setIdTienda(contexto.idTienda());
        turno.setUsuario(contexto.vendedor().usuario());
        turno.setNombreVendedor(contexto.vendedor().nombre());
        turno.setEstado(MovimientoCaja.ESTADO_ABIERTA);
        turno.setMontoApertura(redondear(request.getMontoApertura()));
        turno.setFechaApertura(new Date());
        turno = movimientoRepository.save(turno);

        auditar("Apertura de caja con fondo inicial de S/ "
                        + dinero(turno.getMontoApertura()) + ".",
                contexto, "INFO");
        return construir(turno, contexto, true);
    }

    @Override
    @Transactional
    public CajaEstadoDTO cerrar(SesionPrincipal sesion, CierreCajaRequest request) {
        Contexto contexto = contexto(sesion);
        MovimientoCaja turno = movimientoRepository
                .findFirstByIdTiendaAndUsuarioAndEstadoOrderByFechaAperturaDesc(
                        contexto.idTienda(), contexto.vendedor().usuario(),
                        MovimientoCaja.ESTADO_ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay un turno de caja abierto. Registra la apertura antes de cerrar."));

        Date ahora = new Date();
        Resumen resumen = resumir(contexto, turno.getFechaApertura(), ahora);
        BigDecimal esperado = redondear(turno.getMontoApertura().add(resumen.totalEfectivo()));
        BigDecimal contado = redondear(request.getEfectivoContado());
        BigDecimal diferencia = redondear(contado.subtract(esperado));

        turno.setEstado(MovimientoCaja.ESTADO_CERRADA);
        turno.setFechaCierre(ahora);
        turno.setMontoSistema(esperado);
        turno.setEfectivoContado(contado);
        turno.setDiferencia(diferencia);
        turno.setObservacion(observacion(request.getObservacion()));
        turno = movimientoRepository.save(turno);

        auditar("Cierre de caja: esperado S/ " + dinero(esperado) + ", contado S/ "
                        + dinero(contado) + ", diferencia S/ " + dinero(diferencia)
                        + " (" + etiquetaDiferencia(diferencia) + ").",
                contexto, diferencia.signum() == 0 ? "INFO" : "ALERTA");
        return construir(turno, contexto, false);
    }

    // ------------------------------------------------------------------
    // Resumen del turno
    // ------------------------------------------------------------------

    private CajaEstadoDTO construir(MovimientoCaja turno, Contexto contexto, boolean abierto) {
        Date hasta = abierto ? new Date() : turno.getFechaCierre();
        Resumen resumen = resumir(contexto, turno.getFechaApertura(), hasta);

        CajaEstadoDTO dto = new CajaEstadoDTO();
        dto.setTurnoAbierto(abierto);
        dto.setEstado(abierto ? MovimientoCaja.ESTADO_ABIERTA : MovimientoCaja.ESTADO_CERRADA);
        dto.setIdCaja(turno.getIdCaja());
        dto.setUsuario(turno.getUsuario());
        dto.setNombreVendedor(turno.getNombreVendedor());
        dto.setMontoApertura(turno.getMontoApertura());
        dto.setFechaApertura(fechaHora(turno.getFechaApertura()));
        dto.setFechaCierre(turno.getFechaCierre() == null ? null : fechaHora(turno.getFechaCierre()));
        dto.setMontoSistema(turno.getMontoSistema());
        dto.setEfectivoContado(turno.getEfectivoContado());
        dto.setDiferencia(turno.getDiferencia());
        dto.setObservacion(turno.getObservacion());
        dto.setPeriodo(CajaEstadoDTO.PERIODO_TURNO);
        dto.setDesde(fechaHora(turno.getFechaApertura()));
        dto.setHasta(fechaHora(hasta));
        dto.setEfectivoEsperado(abierto
                ? redondear(turno.getMontoApertura().add(resumen.totalEfectivo()))
                : turno.getMontoSistema());
        dto.setTotalVentas(resumen.total());
        dto.setComprobantes(resumen.comprobantes());
        dto.setMetodosPago(resumen.grupos());
        return dto;
    }

    // Sin turno abierto el POS muestra el dia en curso como referencia, con
    // fondo inicial cero: el efectivo esperado no aplica hasta abrir la caja.
    private CajaEstadoDTO sinTurno(Contexto contexto) {
        Date inicio = Date.from(LocalDate.now(ZONA).atStartOfDay(ZONA).toInstant());
        Date ahora = new Date();
        Resumen resumen = resumir(contexto, inicio, ahora);

        CajaEstadoDTO dto = new CajaEstadoDTO();
        dto.setTurnoAbierto(false);
        dto.setEstado(CajaEstadoDTO.ESTADO_SIN_TURNO);
        dto.setUsuario(contexto.vendedor().usuario());
        dto.setNombreVendedor(contexto.vendedor().nombre());
        dto.setMontoApertura(BigDecimal.ZERO);
        dto.setPeriodo(CajaEstadoDTO.PERIODO_DIA);
        dto.setDesde(fechaHora(inicio));
        dto.setHasta(fechaHora(ahora));
        dto.setTotalVentas(resumen.total());
        dto.setComprobantes(resumen.comprobantes());
        dto.setMetodosPago(resumen.grupos());
        return dto;
    }

    private Resumen resumir(Contexto contexto, Date desde, Date hasta) {
        // Las columnas de fecha guardan segundos y MySQL redondea los
        // milisegundos al insertar: una venta hecha una fraccion de segundo
        // antes del cierre puede quedar guardada en el segundo siguiente. El
        // rango se abre en el segundo completo de la apertura y se cierra en el
        // segundo completo del limite superior (inclusivo), para no perder
        // ventas del mismo segundo en que se abre o se cierra el turno.
        List<Venta> ventas = ventaRepository.ventasDeVendedorEntre(
                contexto.vendedor().usuario(), inicioSegundo(desde), finSegundo(hasta),
                contexto.idTienda());
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        Map<String, Long> conteos = new LinkedHashMap<>();
        for (String grupo : GRUPOS_FIJOS) {
            totales.put(grupo, BigDecimal.ZERO);
            conteos.put(grupo, 0L);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            BigDecimal importe = venta.getTotalVenta() == null ? BigDecimal.ZERO : venta.getTotalVenta();
            String grupo = grupoDe(venta);
            totales.merge(grupo, importe, BigDecimal::add);
            conteos.merge(grupo, 1L, Long::sum);
            total = total.add(importe);
        }

        List<CajaGrupoDTO> grupos = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entrada : totales.entrySet()) {
            String grupo = entrada.getKey();
            if (GRUPO_OTROS.equals(grupo) && conteos.get(grupo) == 0) {
                continue;
            }
            grupos.add(new CajaGrupoDTO(grupo, redondear(entrada.getValue()), conteos.get(grupo)));
        }
        BigDecimal efectivo = totales.getOrDefault(GRUPO_EFECTIVO, BigDecimal.ZERO);
        return new Resumen(grupos, redondear(total), ventas.size(), redondear(efectivo));
    }

    private String grupoDe(Venta venta) {
        if (FORMA_PAGO_CREDITO.equals(venta.getFormaPago())) {
            return GRUPO_CREDITO;
        }
        return GRUPO_POR_MEDIO.getOrDefault(venta.getIdMedioPago(), GRUPO_OTROS);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Contexto contexto(SesionPrincipal sesion) {
        if (sesion == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no iniciada");
        }
        if (sesion.idTienda() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene una tienda asignada");
        }
        Vendedor vendedor = vendedorResolver.resolver(sesion);
        if (vendedor == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el personal de la tienda puede operar la caja");
        }
        return new Contexto(sesion.idTienda(), vendedor);
    }

    private void auditar(String descripcion, Contexto contexto, String severidad) {
        String tienda = tiendaRepository.findById(contexto.idTienda())
                .map(Tienda::getNombre)
                .orElse("");
        auditoriaService.registrar("CAJA", severidad, descripcion, contexto.idTienda(), tienda,
                contexto.vendedor().usuario());
    }

    private String etiquetaDiferencia(BigDecimal diferencia) {
        if (diferencia.signum() == 0) {
            return "cuadra";
        }
        return diferencia.signum() < 0 ? "faltante" : "sobrante";
    }

    private String observacion(String valor) {
        String limpia = valor == null ? "" : valor.trim();
        return limpia.length() > MAX_OBSERVACION ? limpia.substring(0, MAX_OBSERVACION) : limpia;
    }

    private BigDecimal redondear(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        return seguro.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }

    private String dinero(BigDecimal valor) {
        return redondear(valor).toPlainString();
    }

    private String fechaHora(Date fecha) {
        return LocalDateTime.ofInstant(fecha.toInstant(), ZONA).format(FECHA_HORA);
    }

    private Date inicioSegundo(Date fecha) {
        long ms = fecha.getTime();
        return new Date(ms - Math.floorMod(ms, 1000L));
    }

    private Date finSegundo(Date fecha) {
        long ms = fecha.getTime();
        return new Date(ms - Math.floorMod(ms, 1000L) + 1000L);
    }

    private record Contexto(int idTienda, Vendedor vendedor) {
    }

    private record Resumen(List<CajaGrupoDTO> grupos, BigDecimal total, long comprobantes,
                           BigDecimal totalEfectivo) {
    }
}
