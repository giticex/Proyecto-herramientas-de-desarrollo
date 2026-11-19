package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.ValidacionComprobanteException;
import com.example.proyec_herramientas.model.auth.SolicitudTiendaRequest;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SolicitudTiendaService {
    private static final String PENDIENTE = "PENDIENTE";
    private static final String REQUIERE_DATOS = "REQUIERE_DATOS";
    private static final String APROBADA = "APROBADA";
    private static final String RECHAZADA = "RECHAZADA";

    private static final String CONSULTA = """
            SELECT s.id_solicitud, u.usuario, p.nombre, p.apellido, p.telefono,
                   s.negocio, s.rubro, s.slug, s.dni_ultimos4, s.telefono_alternativo,
                   s.estado, s.referencia_demo, s.mensaje, s.respuesta,
                   s.creada, s.revisada, s.revisor
              FROM solicitud_tienda s
              JOIN usuario u ON u.id_usuario = s.id_usuario
              JOIN persona p ON p.id_persona = u.id_persona
            """;

    private static final RowMapper<Solicitud> MAPA = (rs, i) -> new Solicitud(
            rs.getString("id_solicitud"), rs.getString("usuario"),
            rs.getString("nombre"), rs.getString("apellido"), rs.getString("negocio"),
            rs.getString("rubro"), rs.getString("slug"), rs.getString("dni_ultimos4"),
            rs.getString("telefono"), rs.getString("telefono_alternativo"), rs.getString("estado"),
            rs.getString("referencia_demo"), rs.getString("mensaje"), rs.getString("respuesta"),
            rs.getTimestamp("creada").toInstant(),
            rs.getTimestamp("revisada") == null ? null : rs.getTimestamp("revisada").toInstant(),
            rs.getString("revisor"));

    private final JdbcTemplate jdbc;
    private final AuthServiceImpl auth;
    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;

    public SolicitudTiendaService(JdbcTemplate jdbc, AuthServiceImpl auth,
            UsuarioRepository usuarios, TiendaRepository tiendas) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.usuarios = usuarios;
        this.tiendas = tiendas;
    }

    @Transactional
    public Solicitud registrar(SolicitudTiendaRequest datos) {
        if (datos.telefono().equals(datos.telefonoAlternativo())) {
            throw new ValidacionComprobanteException("Ingresa dos números telefónicos distintos.");
        }
        String correo = datos.correo().strip().toLowerCase(Locale.ROOT);
        if (tiendas.findBySlug(datos.slug()).isPresent()
                || Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM solicitud_tienda WHERE slug = ?)", Boolean.class, datos.slug()))) {
            throw new RecursoDuplicadoException("La dirección elegida ya está en uso.");
        }
        var cuenta = auth.crearCuentaPanel(correo, datos.contrasena(), datos.nombres(),
                datos.apellidos(), datos.telefono(), Rol.SOLICITANTE, null);
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO solicitud_tienda (id_solicitud, id_usuario, negocio, rubro, slug,
                    dni_ultimos4, telefono_alternativo, estado, referencia_demo)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?)
                """, id, cuenta.getIdUsuario(), datos.negocio().strip(), datos.rubro().strip(), datos.slug(),
                datos.dni().substring(4), datos.telefonoAlternativo(), "DEMO-" + id);
        return obtener(id, false);
    }

    @Transactional(readOnly = true)
    public Solicitud consultarPropia(String usuario) {
        return jdbc.query(CONSULTA + " WHERE u.usuario = ?", MAPA, usuario).stream()
                .findFirst().orElseThrow(this::noEncontrada);
    }

    @Transactional(readOnly = true)
    public List<Solicitud> listar() {
        return jdbc.query(CONSULTA + """
                ORDER BY CASE WHEN s.estado = 'PENDIENTE' THEN 0 ELSE 1 END, s.creada DESC
                """, MAPA);
    }

    @Transactional
    public Solicitud resolver(String id, String revisor, String decision, String comentario) {
        Solicitud solicitud = obtener(id, true);
        if (APROBADA.equals(solicitud.estado()) && APROBADA.equals(decision)) return solicitud;
        if (!PENDIENTE.equals(solicitud.estado()) && !REQUIERE_DATOS.equals(solicitud.estado())) {
            throw new RecursoDuplicadoException("La solicitud ya está resuelta.");
        }
        String mensaje = comentario == null ? "" : comentario.strip();
        if (!APROBADA.equals(decision) && mensaje.isBlank()) {
            throw new ValidacionComprobanteException("Escribe un motivo para la persona solicitante.");
        }
        if (APROBADA.equals(decision)) activar(solicitud);
        jdbc.update("""
                UPDATE solicitud_tienda SET estado = ?, mensaje = ?, revisor = ?,
                    revisada = CURRENT_TIMESTAMP WHERE id_solicitud = ?
                """, decision, mensaje, revisor, id);
        return obtener(id, false);
    }

    @Transactional
    public Solicitud responder(String usuario, String mensaje) {
        Solicitud solicitud = obtener(consultarPropia(usuario).id(), true);
        if (!REQUIERE_DATOS.equals(solicitud.estado())) {
            throw new RecursoDuplicadoException("En este momento la solicitud no necesita información adicional.");
        }
        jdbc.update("UPDATE solicitud_tienda SET estado = ?, respuesta = ? WHERE id_solicitud = ?",
                PENDIENTE, mensaje.strip(), solicitud.id());
        return obtener(solicitud.id(), false);
    }

    private void activar(Solicitud solicitud) {
        if (tiendas.findBySlug(solicitud.slug()).isPresent()) {
            throw new RecursoDuplicadoException("La dirección solicitada ya pertenece a una tienda.");
        }
        var cuenta = usuarios.findByUsuario(solicitud.correo()).orElseThrow(this::noEncontrada);
        if (!Rol.SOLICITANTE.equals(cuenta.getIdRol()) || cuenta.getIdTienda() != null) {
            throw new RecursoDuplicadoException("La cuenta ya tiene acceso a un negocio.");
        }
        Tienda tienda = new Tienda();
        tienda.setNombre(solicitud.negocio());
        tienda.setRubro(solicitud.rubro());
        tienda.setSlug(solicitud.slug());
        tienda.setPublicada(false);
        cuenta.setIdTienda(tiendas.saveAndFlush(tienda).getIdTienda());
        cuenta.setIdRol(Rol.ADMIN);
        usuarios.saveAndFlush(cuenta);
    }

    private Solicitud obtener(String id, boolean bloqueo) {
        return jdbc.query(CONSULTA + " WHERE s.id_solicitud = ?" + (bloqueo ? " FOR UPDATE" : ""), MAPA, id)
                .stream().findFirst().orElseThrow(this::noEncontrada);
    }

    private RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException("No se encontró la solicitud.");
    }

    public record Solicitud(String id, String correo, String nombres, String apellidos, String negocio,
            String rubro, String slug, String dniUltimos4, String telefono, String telefonoAlternativo, String estado,
            String referenciaDemo, String mensaje, String respuesta, Instant creada, Instant revisada, String revisor) {}
}
