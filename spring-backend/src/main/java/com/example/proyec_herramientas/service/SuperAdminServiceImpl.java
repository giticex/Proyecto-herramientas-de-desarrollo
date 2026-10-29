package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.DetalleVentaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final int TOP_PRODUCTOS = 5;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoService productoService;
    private final VentaService ventaService;

    public SuperAdminServiceImpl(TiendaRepository tiendaRepository,
            ProductoRepository productoRepository,
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProductoService productoService,
            VentaService ventaService) {
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoService = productoService;
        this.ventaService = ventaService;
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminResumenDTO resumen() {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(zona);
        Date inicioHoy = inicio(hoy, zona);
        Date inicioManana = inicio(hoy.plusDays(1), zona);
        Date inicioMes = inicio(hoy.withDayOfMonth(1), zona);
        Date inicioMesSiguiente = inicio(hoy.withDayOfMonth(1).plusMonths(1), zona);

        List<Tienda> tiendas = tiendaRepository.findAllByOrderByNombreAsc();
        long totalProductos = 0;
        long productosSinStock = 0;
        for (Tienda tienda : tiendas) {
            totalProductos += productoRepository.countByIdTienda(tienda.getIdTienda());
            productosSinStock += productoRepository
                    .countByIdTiendaAndStockLessThanEqual(tienda.getIdTienda(), 0);
        }

        return new SuperAdminResumenDTO(
                tiendas.size(),
                totalProductos,
                productosSinStock,
                ventaRepository.contarVentasGlobal(),
                sinNulos(ventaRepository.sumarTotalGlobalEntre(inicioHoy, inicioManana)),
                sinNulos(ventaRepository.sumarTotalGlobalEntre(inicioMes, inicioMesSiguiente)),
                sinNulos(ventaRepository.sumarTotalGlobal()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaResumenDTO> empresas() {
        return tiendaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::resumenDeTienda)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminEmpresaDTO dashboard(int idTienda) {
        EmpresaResumenDTO empresa = resumenDeTienda(tienda(idTienda));
        return new SuperAdminEmpresaDTO(empresa,
                detalleVentaRepository.topProductosPorTienda(idTienda, PageRequest.of(0, TOP_PRODUCTOS)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> productos(int idTienda) {
        tienda(idTienda);
        return productoService.listar(idTienda);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaVentasDTO comprobantes(int idTienda, String busqueda, int pagina, int tamanio) {
        tienda(idTienda);
        return ventaService.listarVentas(idTienda, busqueda, pagina, tamanio);
    }

    private EmpresaResumenDTO resumenDeTienda(Tienda tienda) {
        Integer idTienda = tienda.getIdTienda();
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(zona);
        Date inicioHoy = inicio(hoy, zona);
        Date inicioManana = inicio(hoy.plusDays(1), zona);
        Date inicioMes = inicio(hoy.withDayOfMonth(1), zona);
        Date inicioMesSiguiente = inicio(hoy.withDayOfMonth(1).plusMonths(1), zona);

        long totalProductos = productoRepository.countByIdTienda(idTienda);
        long productosSinStock = productoRepository
                .countByIdTiendaAndStockLessThanEqual(idTienda, 0);
        long totalVentas = ventaRepository.contarVentasPorTienda(idTienda);
        BigDecimal ventasHoy = sinNulos(
                ventaRepository.sumarTotalPorTiendaEntre(inicioHoy, inicioManana, idTienda));
        BigDecimal ventasMes = sinNulos(
                ventaRepository.sumarTotalPorTiendaEntre(inicioMes, inicioMesSiguiente, idTienda));
        BigDecimal ventasTotales = sinNulos(ventaRepository.sumarTotalPorTienda(idTienda));

        return new EmpresaResumenDTO(
                idTienda,
                tienda.getNombre(),
                tienda.getRubro(),
                tienda.getSlug(),
                tienda.getColorPrimario(),
                tienda.getLogoUrl(),
                totalProductos,
                productosSinStock,
                totalVentas,
                ventasHoy,
                ventasMes,
                ventasTotales,
                fecha(ventaRepository.ultimaVentaPorTienda(idTienda)),
                estado(totalProductos, productosSinStock, totalVentas));
    }

    // Estado operativo derivado de la actividad real de la empresa: no existe
    // una columna de habilitacion en la tabla tienda.
    private String estado(long totalProductos, long productosSinStock, long totalVentas) {
        if (totalProductos == 0) {
            return "Sin catálogo";
        }
        if (productosSinStock == totalProductos) {
            return "Stock agotado";
        }
        return totalVentas == 0 ? "Sin ventas" : "Operativa";
    }

    private Tienda tienda(int idTienda) {
        return tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La empresa " + idTienda + " no existe"));
    }

    private Date inicio(LocalDate fecha, ZoneId zona) {
        return Date.from(fecha.atStartOfDay(zona).toInstant());
    }

    private BigDecimal sinNulos(BigDecimal monto) {
        return monto != null ? monto : BigDecimal.ZERO;
    }

    private String fecha(Date valor) {
        if (valor == null) {
            return null;
        }
        return LocalDateTime.ofInstant(valor.toInstant(), ZoneId.systemDefault()).format(FECHA_HORA);
    }
}
