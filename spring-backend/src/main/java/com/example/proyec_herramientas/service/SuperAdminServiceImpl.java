package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.ActualizarPlanRequest;
import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.AdminTiendaDTO;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PaginaAuditoriaDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.PlanInfoDTO;
import com.example.proyec_herramientas.model.PlanTiendaDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.RestablecerPasswordRequest;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;
import com.example.proyec_herramientas.persistence.AuditoriaEvento;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.DetalleVentaRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final int TOP_PRODUCTOS = 5;
    private static final int LONGITUD_MINIMA_CONTRASENA = 8;
    // BCrypt solo considera los primeros 72 bytes de la clave.
    private static final int LONGITUD_MAXIMA_CONTRASENA = 72;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // La consola global crea cuentas de staff; los compradores tienen su
    // propio registro y el SUPER_ADMIN no pertenece a ninguna tienda.
    private static final Set<String> ROLES_STAFF = Set.of(Rol.ADMIN, Rol.VENDEDOR);
    private static final Pattern FORMATO_USUARIO = Pattern.compile("^[A-Za-z0-9._@-]{3,50}$");
    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Catalogo de planes de la plataforma. Los limites del plan son solo los
    // valores sugeridos: el SUPER_ADMIN puede ajustarlos por tienda.
    private static final List<PlanInfoDTO> PLANES = List.of(
            new PlanInfoDTO(Tienda.PLAN_BASICO, "Básico", 100, 5),
            new PlanInfoDTO(Tienda.PLAN_PRO, "Pro", 500, 20),
            new PlanInfoDTO(Tienda.PLAN_ENTERPRISE, "Enterprise", 5000, 100));

    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoService productoService;
    private final VentaService ventaService;
    private final AuditoriaService auditoriaService;
    private final ContrasenaService contrasenaService;

    public SuperAdminServiceImpl(TiendaRepository tiendaRepository,
            ProductoRepository productoRepository,
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            UsuarioRepository usuarioRepository,
            PersonaRepository personaRepository,
            ClienteRepository clienteRepository,
            ProductoService productoService,
            VentaService ventaService,
            AuditoriaService auditoriaService,
            ContrasenaService contrasenaService) {
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.productoService = productoService;
        this.ventaService = ventaService;
        this.auditoriaService = auditoriaService;
        this.contrasenaService = contrasenaService;
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

    @Override
    @Transactional
    public EmpresaResumenDTO cambiarEstado(int idTienda, String estado) {
        Tienda tienda = tienda(idTienda);
        String nuevoEstado = estadoValido(estado);
        if (!nuevoEstado.equalsIgnoreCase(tienda.getEstadoAcceso())) {
            tienda.setEstadoAcceso(nuevoEstado);
            tiendaRepository.save(tienda);
            boolean suspendida = Tienda.ESTADO_SUSPENDIDA.equals(nuevoEstado);
            auditoriaService.registrar(
                    suspendida ? AuditoriaEvento.TIPO_TIENDA_SUSPENDIDA
                            : AuditoriaEvento.TIPO_TIENDA_ACTIVADA,
                    suspendida ? AuditoriaEvento.SEVERIDAD_ALERTA : AuditoriaEvento.SEVERIDAD_INFO,
                    (suspendida ? "Se suspendió el acceso de la tienda "
                            : "Se reactivó el acceso de la tienda ") + tienda.getNombre(),
                    idTienda, tienda.getNombre());
        }
        return resumenDeTienda(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanTiendaDTO plan(int idTienda) {
        return planDeTienda(tienda(idTienda));
    }

    @Override
    @Transactional
    public PlanTiendaDTO actualizarPlan(int idTienda, ActualizarPlanRequest request) {
        Tienda tienda = tienda(idTienda);
        PlanInfoDTO plan = planValido(request == null ? null : request.plan());
        int limiteProductos = limiteValido(
                request == null ? null : request.limiteProductos(),
                plan.limiteProductos(), "productos");
        int limiteUsuarios = limiteValido(
                request == null ? null : request.limiteUsuarios(),
                plan.limiteUsuarios(), "usuarios");

        tienda.setPlan(plan.codigo());
        tienda.setLimiteProductos(limiteProductos);
        tienda.setLimiteUsuarios(limiteUsuarios);
        tiendaRepository.save(tienda);

        auditoriaService.registrar(AuditoriaEvento.TIPO_PLAN_ACTUALIZADO,
                AuditoriaEvento.SEVERIDAD_INFO,
                "Plan de " + tienda.getNombre() + " actualizado a " + plan.nombre()
                        + " (" + limiteProductos + " productos, " + limiteUsuarios + " usuarios)",
                idTienda, tienda.getNombre());
        return planDeTienda(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTiendaDTO admin(int idTienda) {
        return adminDTO(tienda(idTienda), buscarAdmin(idTienda));
    }

    @Override
    @Transactional
    public AdminTiendaDTO restablecerPasswordAdmin(int idTienda, RestablecerPasswordRequest request) {
        Tienda tienda = tienda(idTienda);
        Usuario admin = buscarAdmin(idTienda);
        String nuevaClave = claveValida(request == null ? null : request.password());

        admin.setContrasena(contrasenaService.hashBcrypt(nuevaClave));
        usuarioRepository.save(admin);

        // La clave nunca se registra: la auditoria solo deja constancia del
        // cambio y de quien lo hizo.
        auditoriaService.registrar(AuditoriaEvento.TIPO_ADMIN_PASSWORD_RESTABLECIDO,
                AuditoriaEvento.SEVERIDAD_ALERTA,
                "Se restableció la contraseña del administrador " + admin.getUsuario()
                        + " de la tienda " + tienda.getNombre(),
                idTienda, tienda.getNombre());
        return adminDTO(tienda, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioStaffDTO> usuarios(int idTienda) {
        tienda(idTienda);
        return usuarioRepository
                .findByIdTiendaAndIdRolInOrderByIdUsuarioAsc(idTienda, ROLES_STAFF).stream()
                .map(this::staffDTO)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioStaffDTO crearUsuario(int idTienda, CrearUsuarioStaffRequest request) {
        Tienda tienda = tienda(idTienda);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La solicitud de alta es obligatoria");
        }
        int limite = tienda.getLimiteUsuarios() == null ? 0 : tienda.getLimiteUsuarios();
        // El cupo del plan mide puestos de trabajo: solo ADMIN y VENDEDOR.
        // Las fichas de comprador no son personal y nunca consumen un cupo.
        long total = usuarioRepository.countByIdTiendaAndIdRolIn(idTienda, ROLES_STAFF);
        if (limite > 0 && total >= limite) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La empresa alcanzó el límite de " + limite
                            + " usuarios de su plan; amplía el plan para crear otra cuenta");
        }

        String usuario = usuarioValido(request.usuario(), null);
        String correo = correoValido(request.correo(), null);
        String nombre = nombreValido(request.nombre());
        String apellido = apellidoValido(request.apellido());
        String rol = rolStaffValido(request.rol());
        String clave = claveValida(request.password());

        Persona persona = new Persona();
        persona.setIdPersona(generarId("PER"));
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCorreo(correo);
        persona.setUsuario(usuario);
        personaRepository.save(persona);

        Usuario cuenta = new Usuario();
        cuenta.setUsuario(usuario);
        cuenta.setContrasena(contrasenaService.hashBcrypt(clave));
        cuenta.setIdPersona(persona.getIdPersona());
        cuenta.setIdRol(rol);
        cuenta.setIdTienda(idTienda);
        cuenta.setEstado(Usuario.ESTADO_ACTIVO);
        usuarioRepository.save(cuenta);

        auditoriaService.registrar(AuditoriaEvento.TIPO_USUARIO_CREADO,
                AuditoriaEvento.SEVERIDAD_INFO,
                "Se creó la cuenta " + rol + " " + usuario + " (" + nombreCompleto(nombre, apellido)
                        + ") en la tienda " + tienda.getNombre(),
                idTienda, tienda.getNombre());
        return staffDTO(cuenta);
    }

    @Override
    @Transactional
    public UsuarioStaffDTO actualizarUsuario(int idTienda, int idUsuario,
            ActualizarUsuarioStaffRequest request) {
        Tienda tienda = tienda(idTienda);
        Usuario cuenta = buscarStaff(idTienda, idUsuario);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La solicitud de edición es obligatoria");
        }

        String usuario = usuarioValido(request.usuario(), cuenta);
        String correo = correoValido(request.correo(), cuenta);
        String nombre = nombreValido(request.nombre());
        String apellido = apellidoValido(request.apellido());
        String rol = rolStaffValido(request.rol());
        String rolAnterior = cuenta.getIdRol();

        // Una tienda no puede quedarse sin ningún administrador activo.
        if (Rol.ADMIN.equals(rolAnterior) && !Rol.ADMIN.equals(rol)
                && !cuenta.estaSuspendido() && contarAdminsActivos(idTienda) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La empresa quedaría sin administrador activo; asigna ese rol a otra cuenta primero");
        }

        Persona persona = personaRepository.findById(cuenta.getIdPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La ficha de la cuenta " + cuenta.getUsuario() + " no existe"));

        cuenta.setUsuario(usuario);
        cuenta.setIdRol(rol);
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCorreo(correo);
        persona.setUsuario(usuario);

        String clave = request.password();
        boolean cambiaClave = clave != null && !clave.isBlank();
        if (cambiaClave) {
            cuenta.setContrasena(contrasenaService.hashBcrypt(claveValida(clave)));
        }
        personaRepository.save(persona);
        usuarioRepository.save(cuenta);

        String detalleRol = rolAnterior.equals(rol) ? ""
                : " · rol " + rolAnterior + " → " + rol;
        auditoriaService.registrar(AuditoriaEvento.TIPO_USUARIO_ACTUALIZADO,
                AuditoriaEvento.SEVERIDAD_INFO,
                "Se actualizó la cuenta " + usuario + " (" + nombreCompleto(nombre, apellido)
                        + ") de la tienda " + tienda.getNombre() + detalleRol,
                idTienda, tienda.getNombre());
        if (cambiaClave) {
            auditoriaService.registrar(AuditoriaEvento.TIPO_USUARIO_PASSWORD_RESTABLECIDO,
                    AuditoriaEvento.SEVERIDAD_ALERTA,
                    "Se restableció la contraseña de la cuenta " + usuario
                            + " de la tienda " + tienda.getNombre(),
                    idTienda, tienda.getNombre());
        }
        return staffDTO(cuenta);
    }

    @Override
    @Transactional
    public UsuarioStaffDTO cambiarEstadoUsuario(int idTienda, int idUsuario,
            EstadoUsuarioStaffRequest request) {
        Tienda tienda = tienda(idTienda);
        Usuario cuenta = buscarStaff(idTienda, idUsuario);
        String destino = estadoUsuarioValido(request == null ? null : request.estado());

        if (destino.equals(cuenta.estaSuspendido() ? Usuario.ESTADO_SUSPENDIDO : Usuario.ESTADO_ACTIVO)) {
            return staffDTO(cuenta);
        }
        if (Usuario.ESTADO_SUSPENDIDO.equals(destino) && esUltimoAdminActivo(cuenta)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede suspender al único administrador activo de la empresa");
        }

        cuenta.setEstado(destino);
        usuarioRepository.save(cuenta);
        boolean suspendida = Usuario.ESTADO_SUSPENDIDO.equals(destino);
        auditoriaService.registrar(
                suspendida ? AuditoriaEvento.TIPO_USUARIO_SUSPENDIDO
                        : AuditoriaEvento.TIPO_USUARIO_ACTIVADO,
                suspendida ? AuditoriaEvento.SEVERIDAD_ALERTA : AuditoriaEvento.SEVERIDAD_INFO,
                (suspendida ? "Se suspendió el acceso de la cuenta "
                        : "Se reactivó el acceso de la cuenta ") + cuenta.getUsuario()
                        + " de la tienda " + tienda.getNombre(),
                idTienda, tienda.getNombre());
        return staffDTO(cuenta);
    }

    @Override
    @Transactional
    public void eliminarUsuario(int idTienda, int idUsuario) {
        Tienda tienda = tienda(idTienda);
        Usuario cuenta = buscarStaff(idTienda, idUsuario);

        if (esUltimoAdminActivo(cuenta)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar al único administrador activo de la empresa");
        }
        if (ventaRepository.countByIdPersona(cuenta.getIdPersona()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta tiene comprobantes emitidos; suspéndela en lugar de eliminarla");
        }

        // La ficha de persona solo se borra si ninguna otra cuenta la usa; los
        // comprobantes históricos se resguardan por la verificación anterior.
        boolean otraCuenta = usuarioRepository.findByIdPersona(cuenta.getIdPersona())
                .filter(otra -> !otra.getIdUsuario().equals(cuenta.getIdUsuario()))
                .isPresent();
        usuarioRepository.delete(cuenta);
        usuarioRepository.flush();
        if (!otraCuenta) {
            personaRepository.findById(cuenta.getIdPersona()).ifPresent(personaRepository::delete);
        }

        auditoriaService.registrar(AuditoriaEvento.TIPO_USUARIO_ELIMINADO,
                AuditoriaEvento.SEVERIDAD_ALERTA,
                "Se eliminó la cuenta " + cuenta.getUsuario() + " de la tienda " + tienda.getNombre(),
                idTienda, tienda.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaAuditoriaDTO auditoria(String tipo, String busqueda, int pagina, int tamanio) {
        return auditoriaService.listar(tipo, busqueda, pagina, tamanio);
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
                estado(totalProductos, productosSinStock, totalVentas),
                tienda.getEstadoAcceso() == null ? Tienda.ESTADO_ACTIVA : tienda.getEstadoAcceso(),
                tienda.getPlan() == null ? Tienda.PLAN_BASICO : tienda.getPlan(),
                tienda.getLimiteProductos() == null ? 0 : tienda.getLimiteProductos(),
                tienda.getLimiteUsuarios() == null ? 0 : tienda.getLimiteUsuarios(),
                usuarioRepository.countByIdTiendaAndIdRolIn(idTienda, ROLES_STAFF));
    }

    // Estado operativo derivado de la actividad real de la empresa. Es
    // independiente del estado_acceso administrativo (ACTIVA/SUSPENDIDA).
    private String estado(long totalProductos, long productosSinStock, long totalVentas) {
        if (totalProductos == 0) {
            return "Sin catálogo";
        }
        if (productosSinStock == totalProductos) {
            return "Stock agotado";
        }
        return totalVentas == 0 ? "Sin ventas" : "Operativa";
    }

    private PlanTiendaDTO planDeTienda(Tienda tienda) {
        Integer idTienda = tienda.getIdTienda();
        return new PlanTiendaDTO(
                idTienda,
                tienda.getNombre(),
                tienda.getEstadoAcceso() == null ? Tienda.ESTADO_ACTIVA : tienda.getEstadoAcceso(),
                tienda.getPlan() == null ? Tienda.PLAN_BASICO : tienda.getPlan(),
                tienda.getLimiteProductos() == null ? 0 : tienda.getLimiteProductos(),
                tienda.getLimiteUsuarios() == null ? 0 : tienda.getLimiteUsuarios(),
                productoRepository.countByIdTienda(idTienda),
                usuarioRepository.countByIdTiendaAndIdRolIn(idTienda, ROLES_STAFF),
                PLANES);
    }

    private String estadoValido(String estado) {
        String valor = estado == null ? "" : estado.trim().toUpperCase();
        if (Tienda.ESTADO_ACTIVA.equals(valor) || Tienda.ESTADO_SUSPENDIDA.equals(valor)) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Estado inválido: use ACTIVA o SUSPENDIDA");
    }

    private PlanInfoDTO planValido(String codigo) {
        String valor = codigo == null ? "" : codigo.trim().toUpperCase();
        return PLANES.stream()
                .filter(plan -> plan.codigo().equals(valor))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Plan inválido: use BASICO, PRO o ENTERPRISE"));
    }

    private int limiteValido(Integer solicitado, int sugerido, String recurso) {
        int valor = solicitado == null ? sugerido : solicitado;
        if (valor < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El límite de " + recurso + " debe ser mayor que cero");
        }
        return valor;
    }

    private Usuario buscarAdmin(int idTienda) {
        return usuarioRepository.findFirstByIdTiendaAndIdRolOrderByIdUsuarioAsc(idTienda, Rol.ADMIN)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La empresa " + idTienda + " no tiene una cuenta ADMIN asignada"));
    }

    private Usuario buscarStaff(int idTienda, int idUsuario) {
        Usuario cuenta = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La cuenta " + idUsuario + " no existe"));
        if (cuenta.getIdTienda() == null || cuenta.getIdTienda() != idTienda || !esStaff(cuenta)) {
            throw new RecursoNoEncontradoException(
                    "La cuenta no pertenece al personal de esta empresa");
        }
        return cuenta;
    }

    private boolean esStaff(Usuario cuenta) {
        return ROLES_STAFF.contains(cuenta.getIdRol());
    }

    private long contarAdminsActivos(int idTienda) {
        return usuarioRepository.contarActivasPorTiendaYRol(idTienda, Rol.ADMIN);
    }

    private boolean esUltimoAdminActivo(Usuario cuenta) {
        return Rol.ADMIN.equals(cuenta.getIdRol()) && !cuenta.estaSuspendido()
                && contarAdminsActivos(cuenta.getIdTienda()) <= 1;
    }

    private UsuarioStaffDTO staffDTO(Usuario cuenta) {
        Persona persona = personaRepository.findById(cuenta.getIdPersona()).orElse(null);
        String nombre = persona == null || persona.getNombre() == null ? "" : persona.getNombre();
        String apellido = persona == null || persona.getApellido() == null ? "" : persona.getApellido();
        return new UsuarioStaffDTO(
                cuenta.getIdUsuario(),
                cuenta.getIdTienda(),
                cuenta.getUsuario(),
                nombre,
                apellido,
                nombreCompleto(nombre, apellido),
                persona == null ? null : persona.getCorreo(),
                cuenta.getIdRol(),
                cuenta.estaSuspendido() ? Usuario.ESTADO_SUSPENDIDO : Usuario.ESTADO_ACTIVO,
                contrasenaService.esHashLegado(cuenta.getContrasena()) ? "SHA-256" : "BCrypt");
    }

    private String nombreCompleto(String nombre, String apellido) {
        return (nombre + " " + apellido).trim();
    }

    private String usuarioValido(String valor, Usuario actual) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre de usuario es obligatorio");
        }
        if (!FORMATO_USUARIO.matcher(limpio).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre de usuario debe tener entre 3 y 50 caracteres sin espacios "
                            + "(letras, números, punto, guion, guion bajo o @)");
        }
        Usuario existente = usuarioRepository.findByUsuario(limpio).orElse(null);
        if (existente != null && (actual == null
                || !existente.getIdUsuario().equals(actual.getIdUsuario()))) {
            throw new RecursoDuplicadoException("Ese nombre de usuario ya está en uso");
        }
        // El login del panel acepta nombre de cuenta o correo: si el nombre
        // coincide con el correo de otra persona, la sesión se resolvería por
        // la cuenta equivocada.
        personaRepository.findByCorreo(limpio).ifPresent(persona -> {
            if (actual == null || !persona.getIdPersona().equals(actual.getIdPersona())) {
                throw new RecursoDuplicadoException(
                        "Ese nombre de usuario coincide con un correo registrado");
            }
        });
        // El comprador entra con su correo: si el staff usara ese mismo texto
        // como nombre de cuenta, el login resolveria primero la ficha de
        // comprador y la sesion saldria con el rol equivocado.
        if (clienteRepository.findByCorreo(limpio).isPresent()) {
            throw new RecursoDuplicadoException(
                    "Ese nombre de usuario coincide con el correo de un comprador registrado");
        }
        return limpio;
    }

    private String correoValido(String valor, Usuario actual) {
        String limpio = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo electrónico es obligatorio");
        }
        if (limpio.length() > 100 || !FORMATO_CORREO.matcher(limpio).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo electrónico no tiene un formato válido");
        }
        Persona porCorreo = personaRepository.findByCorreo(limpio).orElse(null);
        if (porCorreo != null && (actual == null
                || !porCorreo.getIdPersona().equals(actual.getIdPersona()))) {
            throw new RecursoDuplicadoException("Ese correo ya está registrado");
        }
        if (clienteRepository.findByCorreo(limpio).isPresent()) {
            throw new RecursoDuplicadoException("Ese correo pertenece a un comprador registrado");
        }
        Usuario porUsuario = usuarioRepository.findByUsuario(limpio).orElse(null);
        if (porUsuario != null && (actual == null
                || !porUsuario.getIdUsuario().equals(actual.getIdUsuario()))) {
            throw new RecursoDuplicadoException("Ese correo coincide con un nombre de usuario existente");
        }
        return limpio;
    }

    private String nombreValido(String valor) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (limpio.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los 50 caracteres");
        }
        return limpio;
    }

    private String apellidoValido(String valor) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El apellido no puede superar los 50 caracteres");
        }
        return limpio;
    }

    private String rolStaffValido(String valor) {
        String limpio = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!ROLES_STAFF.contains(limpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rol inválido: use ADMIN o VENDEDOR");
        }
        return limpio;
    }

    private String estadoUsuarioValido(String valor) {
        String limpio = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (Usuario.ESTADO_ACTIVO.equals(limpio) || Usuario.ESTADO_SUSPENDIDO.equals(limpio)) {
            return limpio;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Estado inválido: use ACTIVO o SUSPENDIDO");
    }

    private String generarId(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private AdminTiendaDTO adminDTO(Tienda tienda, Usuario admin) {
        Persona persona = personaRepository.findById(admin.getIdPersona()).orElse(null);
        String nombreCompleto = persona == null ? null
                : (persona.getNombre() + " " + persona.getApellido()).trim();
        boolean suspendida = tienda.estaSuspendida();
        return new AdminTiendaDTO(
                tienda.getIdTienda(),
                tienda.getNombre(),
                tienda.getEstadoAcceso() == null ? Tienda.ESTADO_ACTIVA : tienda.getEstadoAcceso(),
                admin.getIdUsuario(),
                admin.getUsuario(),
                nombreCompleto,
                persona == null ? null : persona.getCorreo(),
                admin.getIdRol(),
                suspendida ? "SUSPENDIDO" : "ACTIVO",
                contrasenaService.esHashLegado(admin.getContrasena()) ? "SHA-256" : "BCrypt");
    }

    private String claveValida(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña es obligatoria");
        }
        if (clave.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA
                            + " caracteres");
        }
        if (clave.length() > LONGITUD_MAXIMA_CONTRASENA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña no puede superar los " + LONGITUD_MAXIMA_CONTRASENA
                            + " caracteres");
        }
        return clave;
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
