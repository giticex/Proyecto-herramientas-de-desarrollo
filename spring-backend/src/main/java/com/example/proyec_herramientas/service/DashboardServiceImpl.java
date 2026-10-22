package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.DashboardResumenDTO;
import com.example.proyec_herramientas.model.ProductoVendidoDTO;
import com.example.proyec_herramientas.repository.DetalleVentaRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int TOP_PRODUCTOS = 5;

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public DashboardServiceImpl(VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResumenDTO resumen(int idTienda) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(zona);

        Date inicioHoy = inicio(hoy, zona);
        Date inicioManana = inicio(hoy.plusDays(1), zona);
        Date inicioMes = inicio(hoy.withDayOfMonth(1), zona);
        Date inicioMesSiguiente = inicio(hoy.withDayOfMonth(1).plusMonths(1), zona);

        BigDecimal ventasHoy = sinNulos(
                ventaRepository.sumarTotalPorTiendaEntre(inicioHoy, inicioManana, idTienda));
        BigDecimal ingresosMes = sinNulos(
                ventaRepository.sumarTotalPorTiendaEntre(inicioMes, inicioMesSiguiente, idTienda));
        List<ProductoVendidoDTO> topProductos = detalleVentaRepository
                .topProductosPorTienda(idTienda, PageRequest.of(0, TOP_PRODUCTOS));

        return new DashboardResumenDTO(ventasHoy, ingresosMes, topProductos);
    }

    private Date inicio(LocalDate fecha, ZoneId zona) {
        return Date.from(fecha.atStartOfDay(zona).toInstant());
    }

    private BigDecimal sinNulos(BigDecimal monto) {
        return monto != null ? monto : BigDecimal.ZERO;
    }
}
