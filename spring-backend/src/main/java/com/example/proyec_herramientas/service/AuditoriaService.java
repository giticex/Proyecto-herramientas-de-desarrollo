package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.AuditoriaEventoDTO;
import com.example.proyec_herramientas.model.PaginaAuditoriaDTO;
import com.example.proyec_herramientas.persistence.AuditoriaEvento;
import com.example.proyec_herramientas.repository.AuditoriaEventoRepository;
import com.example.proyec_herramientas.security.SesionPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Registro de actividad critica de la plataforma. Comparte la transaccion de
 * la operacion que lo origina: el evento y el cambio que describe se guardan
 * juntos o no se guarda ninguno.
 */
@Service
public class AuditoriaService {

    private static final int TAMANIO_MAXIMO_PAGINA = 50;
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AuditoriaEventoRepository repository;

    public AuditoriaService(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(String tipo, String severidad, String descripcion,
            Integer idTienda, String tiendaNombre) {
        registrar(tipo, severidad, descripcion, idTienda, tiendaNombre, actorActual());
    }

    @Transactional
    public void registrar(String tipo, String severidad, String descripcion,
            Integer idTienda, String tiendaNombre, String actor) {
        String responsable = actor == null || actor.isBlank() ? actorActual() : actor.trim();
        repository.save(new AuditoriaEvento(tipo, severidad, descripcion, idTienda, tiendaNombre,
                responsable));
    }

    @Transactional(readOnly = true)
    public PaginaAuditoriaDTO listar(String tipo, String busqueda, int pagina, int tamanio) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO_PAGINA);
        String filtroTipo = tipo == null ? "" : tipo.trim().toUpperCase();
        String texto = busqueda == null ? "" : busqueda.trim();

        Page<AuditoriaEvento> resultado = repository.buscar(filtroTipo, texto,
                PageRequest.of(paginaSegura, tamanioSeguro,
                        Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("idEvento"))));
        List<AuditoriaEventoDTO> contenido = resultado.getContent().stream()
                .map(this::aDTO)
                .toList();
        return new PaginaAuditoriaDTO(contenido, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private AuditoriaEventoDTO aDTO(AuditoriaEvento evento) {
        String fecha = evento.getFecha() == null
                ? ""
                : LocalDateTime.ofInstant(evento.getFecha().toInstant(), ZoneId.systemDefault())
                        .format(FECHA_HORA);
        return new AuditoriaEventoDTO(evento.getIdEvento(), fecha, evento.getTipo(),
                evento.getSeveridad(), evento.getDescripcion(), evento.getIdTienda(),
                evento.getTiendaNombre(), evento.getActor());
    }

    private String actorActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof SesionPrincipal principal) {
            return principal.usuario();
        }
        return "sistema";
    }
}
