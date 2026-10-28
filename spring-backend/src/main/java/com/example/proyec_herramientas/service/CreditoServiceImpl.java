package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.ValidacionComprobanteException;
import com.example.proyec_herramientas.model.CanjeCreditoDTO;
import com.example.proyec_herramientas.model.ReembolsoCreditoDTO;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.persistence.Devolucion;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.CreditoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CreditoServiceImpl implements CreditoService {

    private final CreditoRepository creditoRepository;

    public CreditoServiceImpl(CreditoRepository creditoRepository) {
        this.creditoRepository = creditoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Credito> listar() {
        return creditoRepository.findAllByOrderByFechaDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Credito buscarPorCodigo(String codigo) {
        return creditoRepository.findByCodigo(normalizar(codigo))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Crédito " + codigo + " no encontrado"));
    }

    @Override
    @Transactional
    public Credito generarParaDevolucion(Devolucion devolucion, Venta venta) {
        Credito credito = new Credito();
        credito.setIdCredito(UUID.randomUUID().toString());
        credito.setCodigo(siguienteCodigo());
        credito.setIdVentaOrigen(venta.getId());
        credito.setNumeroDocumentoOrigen(venta.getNumeroDocumento());
        credito.setClienteNombre(venta.getReceptorRazonSocial());
        credito.setDniCliente(venta.getReceptorNumDocumento());
        BigDecimal total = devolucion.getTotal() == null
                ? BigDecimal.ZERO
                : devolucion.getTotal().setScale(2, RoundingMode.HALF_UP);
        credito.setMontoTotal(total);
        credito.setMontoDisponible(total);
        credito.setEstado(Credito.ESTADO_VIGENTE);
        credito.setFecha(new Date());
        return creditoRepository.save(credito);
    }

    @Override
    @Transactional
    public Credito canjear(CanjeCreditoDTO request) {
        Credito credito = bloquear(request.codigo());
        BigDecimal monto = request.monto().setScale(2, RoundingMode.HALF_UP);
        validarVigencia(credito);
        if (monto.compareTo(credito.getMontoDisponible()) > 0) {
            throw new ValidacionComprobanteException(
                    "El crédito " + credito.getCodigo() + " solo tiene disponible S/ "
                            + credito.getMontoDisponible());
        }
        descontar(credito, monto);
        return creditoRepository.save(credito);
    }

    @Override
    @Transactional
    public Credito reembolsar(ReembolsoCreditoDTO request) {
        Credito credito = bloquear(request.codigo());
        if (Credito.ESTADO_REEMBOLSADO.equals(credito.getEstado())) {
            throw new ValidacionComprobanteException(
                    "El crédito " + credito.getCodigo() + " ya fue reembolsado");
        }
        if (Credito.ESTADO_USADO.equals(credito.getEstado())) {
            throw new ValidacionComprobanteException(
                    "El crédito " + credito.getCodigo() + " no tiene saldo disponible para reembolsar");
        }
        credito.setMontoDisponible(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        credito.setEstado(Credito.ESTADO_REEMBOLSADO);
        return creditoRepository.save(credito);
    }

    @Override
    @Transactional
    public ResultadoUsoCredito aplicarEnVenta(String codigo, BigDecimal totalVenta) {
        Credito credito = bloquear(codigo);
        validarVigencia(credito);
        BigDecimal total = totalVenta == null ? BigDecimal.ZERO : totalVenta;
        BigDecimal usado = total.min(credito.getMontoDisponible()).setScale(2, RoundingMode.HALF_UP);
        if (usado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionComprobanteException(
                    "El crédito " + credito.getCodigo() + " no tiene saldo disponible");
        }
        descontar(credito, usado);
        creditoRepository.save(credito);
        return new ResultadoUsoCredito(credito.getCodigo(), usado, credito.getMontoDisponible());
    }

    private void validarVigencia(Credito credito) {
        if (!credito.esVigente()) {
            throw new ValidacionComprobanteException(
                    "El crédito " + credito.getCodigo() + " no está vigente (estado: "
                            + credito.getEstado() + ")");
        }
    }

    private void descontar(Credito credito, BigDecimal monto) {
        BigDecimal restante = credito.getMontoDisponible().subtract(monto)
                .setScale(2, RoundingMode.HALF_UP);
        credito.setMontoDisponible(restante);
        if (restante.compareTo(BigDecimal.ZERO) <= 0) {
            credito.setEstado(Credito.ESTADO_USADO);
        }
    }

    private Credito bloquear(String codigo) {
        return creditoRepository.findByCodigoForUpdate(normalizar(codigo))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Crédito " + codigo + " no encontrado"));
    }

    private String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }

    // Secuencial BOL-CR-000001. El repositorio devuelve el último por orden
    // lexicográfico, que coincide con el numérico por el relleno a 6 dígitos.
    private synchronized String siguienteCodigo() {
        return creditoRepository
                .findTopByCodigoStartingWithOrderByCodigoDesc(Credito.PREFIJO_CODIGO)
                .map(ultimo -> Credito.PREFIJO_CODIGO
                        + String.format("%06d", numeroDe(ultimo.getCodigo()) + 1))
                .orElse(Credito.PREFIJO_CODIGO + "000001");
    }

    private int numeroDe(String codigo) {
        try {
            return Integer.parseInt(codigo.substring(Credito.PREFIJO_CODIGO.length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return 0;
        }
    }
}
