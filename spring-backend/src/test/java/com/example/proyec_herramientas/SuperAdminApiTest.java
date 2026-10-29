package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La consola global es una capa superior: el SUPER_ADMIN ve todas las
// empresas y sus comprobantes; ningun otro rol puede entrar a esos endpoints.
// Cada empresa sigue viendose por separado (dashboard, productos, ventas).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SuperAdminApiTest {

    private static final String SLUG_EMPRESA_B = "empresa-dos";
    private static final String USUARIO_ADMIN_B = "adminb";
    private static final String CORREO_ADMIN_B = "adminb@prueba.com";
    private static final String ID_PERSONA_ADMIN_B = "PER-PRUEBA-ADMB";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    private Tienda tiendaA;
    private Tienda tiendaB;
    private String tokenSuper;
    private String tokenAdminA;
    private String tokenVendedor;
    private String tokenCliente;
    private String idVentaA;
    private String idVentaB;

    @BeforeEach
    void preparar() throws Exception {
        jdbcTemplate.update("DELETE FROM auditoria_evento");
        jdbcTemplate.update("DELETE FROM credito");
        jdbcTemplate.update("DELETE FROM devolucion_detalle");
        jdbcTemplate.update("DELETE FROM devolucion");
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();
        // Otras clases de prueba registran tiendas que no borran. Aqui se
        // limpian para que el resumen global tenga un universo conocido.
        tiendaRepository.deleteAll();

        tiendaA = CuentasTest.crearTienda(tiendaRepository);
        tiendaB = tiendaRepository.findBySlug(SLUG_EMPRESA_B).orElseGet(() -> {
            Tienda tienda = new Tienda();
            tienda.setNombre("Empresa Dos");
            tienda.setRubro("Moda");
            tienda.setSlug(SLUG_EMPRESA_B);
            return tiendaRepository.save(tienda);
        });

        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearVendedor(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        CuentasTest.crearSuperAdmin(usuarioRepository, personaRepository, tiendaRepository);
        crearAdminDeEmpresaB();

        tokenSuper = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_SUPER, CuentasTest.CLAVE);
        tokenAdminA = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);
        tokenVendedor = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_VENDEDOR,
                CuentasTest.CLAVE);
        tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        guardarProducto(tiendaA, 1, "Mouse Gamer", 10, "50.00");
        guardarProducto(tiendaB, 2, "Polo Oversize", 5, "30.00");

        String tokenAdminB = CuentasTest.token(mockMvc, objectMapper, USUARIO_ADMIN_B, CuentasTest.CLAVE);
        idVentaA = registrarVenta(tokenAdminA, 1, 2);
        idVentaB = registrarVenta(tokenAdminB, 2, 1);
    }

    @Test
    void elLoginDelSuperAdminEsGlobalYNoLlevaTienda() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_SUPER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("SUPER_ADMIN")))
                .andExpect(jsonPath("$.nombreRol", is("Super Administrador")))
                .andExpect(jsonPath("$.idTienda", is(nullValue())))
                .andExpect(jsonPath("$.tienda", is(nullValue())))
                .andExpect(jsonPath("$.token", notNullValue()));

        // Tambien se reconoce por el correo de la persona.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.CORREO_SUPER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("SUPER_ADMIN")));
    }

    @Test
    void elResumenGlobalSumaTodasLasEmpresas() throws Exception {
        mockMvc.perform(get("/api/super-admin/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmpresas", is(2)))
                .andExpect(jsonPath("$.totalProductos", is(2)))
                .andExpect(jsonPath("$.productosSinStock", is(0)))
                .andExpect(jsonPath("$.totalVentas", is(2)))
                .andExpect(jsonPath("$.ingresoHoy", is(130.0)))
                .andExpect(jsonPath("$.ingresoMes", is(130.0)))
                .andExpect(jsonPath("$.ingresoTotal", is(130.0)));
    }

    @Test
    void elListadoDeEmpresasTraeLasMetricasDeCadaTienda() throws Exception {
        mockMvc.perform(get("/api/super-admin/empresas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Orden alfabetico: Empresa Dos antes que Tienda Prueba.
                .andExpect(jsonPath("$[0].nombre", is("Empresa Dos")))
                .andExpect(jsonPath("$[0].slug", is(SLUG_EMPRESA_B)))
                .andExpect(jsonPath("$[0].totalProductos", is(1)))
                .andExpect(jsonPath("$[0].totalVentas", is(1)))
                .andExpect(jsonPath("$[0].ventasHoy", is(30.0)))
                .andExpect(jsonPath("$[0].ventasTotales", is(30.0)))
                .andExpect(jsonPath("$[0].ultimaVenta", notNullValue()))
                .andExpect(jsonPath("$[0].estado", is("Operativa")))
                .andExpect(jsonPath("$[1].nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$[1].totalVentas", is(1)))
                .andExpect(jsonPath("$[1].ventasTotales", is(100.0)));
    }

    @Test
    void cadaEmpresaSeInspeccionaPorSeparado() throws Exception {
        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaA.getIdTienda() + "/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa.nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.empresa.ventasTotales", is(100.0)))
                .andExpect(jsonPath("$.topProductos", hasSize(1)))
                .andExpect(jsonPath("$.topProductos[0].nombre", is("Mouse Gamer")))
                .andExpect(jsonPath("$.topProductos[0].cantidad", is(2)));

        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaB.getIdTienda() + "/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa.nombre", is("Empresa Dos")))
                .andExpect(jsonPath("$.empresa.ventasTotales", is(30.0)))
                .andExpect(jsonPath("$.topProductos[0].nombre", is("Polo Oversize")));

        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaA.getIdTienda() + "/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Mouse Gamer")))
                .andExpect(jsonPath("$[0].idTienda", is(tiendaA.getIdTienda())));

        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaB.getIdTienda() + "/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Polo Oversize")))
                .andExpect(jsonPath("$[0].idTienda", is(tiendaB.getIdTienda())));
    }

    @Test
    void losComprobantesSeListanPorEmpresa() throws Exception {
        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaA.getIdTienda() + "/comprobantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numeroDocumento", startsWith("BOL-")))
                .andExpect(jsonPath("$.contenido[0].total", is(100.0)));

        mockMvc.perform(get("/api/super-admin/empresas/" + tiendaB.getIdTienda() + "/comprobantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].total", is(30.0)));
    }

    @Test
    void unaEmpresaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/super-admin/empresas/999999/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElSuperAdminEntraALaConsolaGlobal() throws Exception {
        mockMvc.perform(get("/api/super-admin/resumen"))
                .andExpect(status().isUnauthorized());

        for (String token : List.of(tokenAdminA, tokenVendedor, tokenCliente)) {
            mockMvc.perform(get("/api/super-admin/resumen")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/super-admin/empresas")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/super-admin/empresas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk());

        // La consola del ADMIN sigue siendo exclusiva del ADMIN: el rol global
        // no hereda permisos del panel de tienda.
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isForbidden());
    }

    @Test
    void elSuperAdminPuedeDescargarComprobantesDeCualquierEmpresa() throws Exception {
        mockMvc.perform(get("/api/comprobantes/" + idVentaB + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // La descarga desde una empresa no abre las ventas de otra: el ADMIN
        // de la tienda A sigue sin poder leer el comprobante de la tienda B.
        mockMvc.perform(get("/api/comprobantes/" + idVentaB + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspenderUnaEmpresaLeQuitaElAccesoYActivarlaLaRestaura() throws Exception {
        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.estadoAcceso", is("SUSPENDIDA")));

        // Un nuevo login de la tienda se rechaza con un mensaje explicito.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("suspendido")));

        // El token que ya estaba en uso deja de servir de inmediato, sin
        // esperar a que expire.
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());

        // Las demas empresas siguen operando con normalidad.
        String tokenAdminB = CuentasTest.token(mockMvc, objectMapper, USUARIO_ADMIN_B, CuentasTest.CLAVE);
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminB))
                .andExpect(status().isOk());

        // La consola global distingue el estado administrativo por empresa.
        mockMvc.perform(get("/api/super-admin/empresas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoAcceso", is("ACTIVA")))
                .andExpect(jsonPath("$[1].estadoAcceso", is("SUSPENDIDA")));

        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAcceso", is("ACTIVA")));

        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPlanYLosLimitesSeAdministranPorEmpresa() throws Exception {
        mockMvc.perform(get(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.plan", is("BASICO")))
                .andExpect(jsonPath("$.limiteProductos", is(100)))
                .andExpect(jsonPath("$.limiteUsuarios", is(5)))
                .andExpect(jsonPath("$.totalProductos", is(1)))
                // Solo el personal operativo: admin y vendedor. El comprador de
                // la tienda tiene ficha de cliente y no consume cupo ni cuenta
                // como usuario del plan; la cuenta global tampoco.
                .andExpect(jsonPath("$.totalUsuarios", is(2)))
                .andExpect(jsonPath("$.planes", hasSize(3)))
                .andExpect(jsonPath("$.planes[1].codigo", is("PRO")));

        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\",\"limiteProductos\":250,\"limiteUsuarios\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("PRO")))
                .andExpect(jsonPath("$.limiteProductos", is(250)))
                .andExpect(jsonPath("$.limiteUsuarios", is(12)));

        mockMvc.perform(get("/api/super-admin/empresas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].plan", is("PRO")))
                .andExpect(jsonPath("$[1].limiteProductos", is(250)))
                .andExpect(jsonPath("$[1].limiteUsuarios", is(12)));

        // Solo el plan basta para tomar sus limites por defecto.
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"ENTERPRISE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteProductos", is(5000)))
                .andExpect(jsonPath("$.limiteUsuarios", is(100)));

        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"GOLD\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elLimiteDeProductosDelPlanBloqueaNuevasAltas() throws Exception {
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":1,\"limiteUsuarios\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(10, "Teclado Nuevo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("límite")));

        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":5,\"limiteUsuarios\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(10, "Teclado Nuevo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre", is("Teclado Nuevo")));
    }

    @Test
    void laAuditoriaGlobalRegistraElAltaLosBloqueosYLosCambiosDePlan() throws Exception {
        mockMvc.perform(post("/api/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Nueva Empresa",
                                "rubro", "Servicios",
                                "slug", "nueva-empresa",
                                "correo", "nueva@prueba.com",
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\",\"limiteProductos\":300,\"limiteUsuarios\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/super-admin/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(4)))
                // Mas reciente primero: plan, activacion, suspension y alta.
                .andExpect(jsonPath("$.contenido[0].tipo", is("PLAN_ACTUALIZADO")))
                .andExpect(jsonPath("$.contenido[0].actor", is(CuentasTest.USUARIO_SUPER)))
                .andExpect(jsonPath("$.contenido[0].tiendaNombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.contenido[1].tipo", is("TIENDA_ACTIVADA")))
                .andExpect(jsonPath("$.contenido[2].tipo", is("TIENDA_SUSPENDIDA")))
                .andExpect(jsonPath("$.contenido[2].severidad", is("ALERTA")))
                .andExpect(jsonPath("$.contenido[3].tipo", is("TIENDA_CREADA")))
                .andExpect(jsonPath("$.contenido[3].actor", is("nueva@prueba.com")));

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=TIENDA_SUSPENDIDA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALERTA")));

        mockMvc.perform(get("/api/super-admin/auditoria")
                        .param("q", "Tienda Prueba")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(3)));

        mockMvc.perform(get("/api/super-admin/auditoria?page=0&size=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.totalPaginas", is(2)));
    }

    @Test
    void laConsolaMuestraElAdministradorDeCadaEmpresa() throws Exception {
        mockMvc.perform(get(adminUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())))
                .andExpect(jsonPath("$.tiendaNombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.estadoAcceso", is("ACTIVA")))
                .andExpect(jsonPath("$.usuario", is(CuentasTest.USUARIO_ADMIN)))
                .andExpect(jsonPath("$.nombreCompleto", is("Administrador Prueba")))
                .andExpect(jsonPath("$.correo", is(CuentasTest.CORREO_ADMIN)))
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.estadoRol", is("ACTIVO")))
                // El login previo de las pruebas migro el SHA-256 a BCrypt.
                .andExpect(jsonPath("$.formatoContrasena", is("BCrypt")));

        mockMvc.perform(get(adminUrl(tiendaB))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario", is(USUARIO_ADMIN_B)))
                .andExpect(jsonPath("$.correo", is(CORREO_ADMIN_B)))
                .andExpect(jsonPath("$.nombreCompleto", is("Admin Empresa Dos")))
                .andExpect(jsonPath("$.rol", is("ADMIN")));

        // El estado del rol refleja el bloqueo administrativo de la tienda.
        mockMvc.perform(put(estadoUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(adminUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAcceso", is("SUSPENDIDA")))
                .andExpect(jsonPath("$.estadoRol", is("SUSPENDIDO")));
    }

    @Test
    void elSuperAdminRestableceLaContrasenaDelAdministradorConBcrypt() throws Exception {
        String nuevaClave = "NuevaClave#2026";

        mockMvc.perform(put(adminPasswordUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", nuevaClave))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario", is(CuentasTest.USUARIO_ADMIN)))
                .andExpect(jsonPath("$.formatoContrasena", is("BCrypt")));

        Usuario admin = usuarioRepository.findByUsuario(CuentasTest.USUARIO_ADMIN).orElseThrow();
        assertTrue(admin.getContrasena().startsWith("$2"),
                "La contraseña debe quedar guardada como hash BCrypt");

        // La clave anterior deja de funcionar y la nueva abre sesion.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, nuevaClave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")));

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=ADMIN_PASSWORD_RESTABLECIDO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALERTA")))
                .andExpect(jsonPath("$.contenido[0].actor", is(CuentasTest.USUARIO_SUPER)))
                .andExpect(jsonPath("$.contenido[0].tiendaNombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.contenido[0].descripcion", containsString(CuentasTest.USUARIO_ADMIN)));
    }

    @Test
    void elRestablecimientoExigeUnaClaveValida() throws Exception {
        for (String clave : List.of("corta", "        ")) {
            mockMvc.perform(put(adminPasswordUrl(tiendaA))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("password", clave))))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(put(adminPasswordUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(adminPasswordUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "x".repeat(73)))))
                .andExpect(status().isBadRequest());

        // Ningun intento invalido tocó la credencial vigente.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void unaEmpresaSinAdministradorNoExponeCredenciales() throws Exception {
        Tienda sinAdmin = new Tienda();
        sinAdmin.setNombre("Empresa Sin Admin");
        sinAdmin.setRubro("Servicios");
        sinAdmin.setSlug("empresa-sin-admin");
        tiendaRepository.save(sinAdmin);

        mockMvc.perform(get(adminUrl(sinAdmin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(adminPasswordUrl(sinAdmin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NuevaClave#2026\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElSuperAdminGestionaLasCredencialesDelAdministrador() throws Exception {
        for (String token : List.of(tokenAdminA, tokenVendedor, tokenCliente)) {
            mockMvc.perform(put(adminPasswordUrl(tiendaA))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"NuevaClave#2026\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(adminUrl(tiendaA))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(put(adminPasswordUrl(tiendaA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NuevaClave#2026\"}"))
                .andExpect(status().isUnauthorized());

        // La credencial del administrador sigue intacta.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void laConsolaListaElPersonalSinCuentasAjenasALaTienda() throws Exception {
        mockMvc.perform(get(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].usuario", is(CuentasTest.USUARIO_ADMIN)))
                .andExpect(jsonPath("$[0].nombreCompleto", is("Administrador Prueba")))
                .andExpect(jsonPath("$[0].correo", is(CuentasTest.CORREO_ADMIN)))
                .andExpect(jsonPath("$[0].rol", is("ADMIN")))
                .andExpect(jsonPath("$[0].estado", is("ACTIVO")))
                .andExpect(jsonPath("$[0].formatoContrasena", is("BCrypt")))
                .andExpect(jsonPath("$[1].usuario", is(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(jsonPath("$[1].rol", is("VENDEDOR")))
                // El comprador de la tienda tambien tiene fila en "usuario",
                // pero queda fuera del personal por su rol CLIENTE.
                .andExpect(jsonPath("$[?(@.rol == 'CLIENTE')]", hasSize(0)));

        mockMvc.perform(get(usuariosUrl(tiendaB))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuario", is(USUARIO_ADMIN_B)));

        mockMvc.perform(get("/api/super-admin/empresas/999999/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNotFound());
    }

    @Test
    void elSuperAdminCreaCuentasDeStaffConBcrypt() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "usuario", "cajero1",
                "nombre", "Caja",
                "apellido", "Uno",
                "correo", "cajero1@prueba.com",
                "rol", "VENDEDOR",
                "password", "ClaveStaff#2026"));

        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario", is("cajero1")))
                .andExpect(jsonPath("$.nombreCompleto", is("Caja Uno")))
                .andExpect(jsonPath("$.correo", is("cajero1@prueba.com")))
                .andExpect(jsonPath("$.rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.estado", is("ACTIVO")))
                .andExpect(jsonPath("$.formatoContrasena", is("BCrypt")))
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())));

        Usuario creado = usuarioRepository.findByUsuario("cajero1").orElseThrow();
        assertTrue(creado.getContrasena().startsWith("$2"),
                "La contraseña de la cuenta nueva debe quedar como hash BCrypt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("cajero1", "ClaveStaff#2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())));

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_CREADO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("INFO")))
                .andExpect(jsonPath("$.contenido[0].actor", is(CuentasTest.USUARIO_SUPER)))
                .andExpect(jsonPath("$.contenido[0].descripcion", containsString("cajero1")));

        // Nombre de usuario y correo son unicos; el rol se acota al personal.
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero1", "nombre", "Otro", "correo", "otro@prueba.com",
                                "rol", "VENDEDOR", "password", "ClaveStaff#2026"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero2", "nombre", "Otro",
                                "correo", "cajero1@prueba.com",
                                "rol", "VENDEDOR", "password", "ClaveStaff#2026"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero3", "nombre", "Otro",
                                "correo", "cajero3@prueba.com",
                                "rol", "CLIENTE", "password", "ClaveStaff#2026"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero4", "nombre", "Otro",
                                "correo", "cajero4@prueba.com",
                                "rol", "VENDEDOR", "password", "corta"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCreacionRespetaElLimiteDeUsuariosDelPlan() throws Exception {
        // En la tienda hay dos cuentas de personal y una fila de comprador en
        // "usuario". El cupo mide puestos de staff, asi que con limite 3 cabe
        // una cuenta mas antes de bloquear.
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":100,\"limiteUsuarios\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero6", "nombre", "Caja", "correo", "cajero6@prueba.com",
                                "rol", "VENDEDOR", "password", "ClaveStaff#2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero7", "nombre", "Caja", "correo", "cajero7@prueba.com",
                                "rol", "VENDEDOR", "password", "ClaveStaff#2026"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("usuarios de su plan")));

        // El comprador sigue existiendo como cuenta de portal, pero no suma.
        assertTrue(usuarioRepository.findByUsuario(CuentasTest.USUARIO_CLIENTE).isPresent());
        mockMvc.perform(get(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios", is(3)));
    }

    @Test
    void elAltaYLaEdicionDeStaffRechazanIdentidadesDeComprador() throws Exception {
        // Comprador registrado solo en la tabla cliente: su correo no existe
        // como persona, asi que la colision se detecta en la validacion de
        // compradores y no en la de correos de persona.
        Cliente soloComprador = new Cliente("CLI-SOLO-CORREO", "Solo", "Comprador",
                "987654399", "solo.comprador@prueba.com");
        soloComprador.setContrasena(CuentasTest.HASH_123456);
        soloComprador.setIdRol(Rol.CLIENTE);
        clienteRepository.save(soloComprador);

        // El correo de un comprador no puede ser el correo de una cuenta staff.
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero8", "nombre", "Caja", "correo",
                                "solo.comprador@prueba.com", "rol", "VENDEDOR",
                                "password", "ClaveStaff#2026"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));

        // Tampoco puede usarse como nombre de cuenta: el login resolveria
        // primero al comprador y la sesion saldria con rol CLIENTE.
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "solo.comprador@prueba.com", "nombre", "Caja",
                                "correo", "cajero8@prueba.com", "rol", "VENDEDOR",
                                "password", "ClaveStaff#2026"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));

        assertTrue(usuarioRepository.findByUsuario("cajero8").isEmpty(),
                "Una alta rechazada no debe dejar cuentas a medias");

        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR).orElseThrow();

        // La edicion aplica las mismas reglas.
        mockMvc.perform(put(usuarioUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", CuentasTest.USUARIO_VENDEDOR, "nombre", "Vendedor",
                                "apellido", "Prueba", "correo", "solo.comprador@prueba.com",
                                "rol", "VENDEDOR"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));
        mockMvc.perform(put(usuarioUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "solo.comprador@prueba.com", "nombre", "Vendedor",
                                "apellido", "Prueba", "correo", CuentasTest.CORREO_VENDEDOR,
                                "rol", "VENDEDOR"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));

        // La cuenta conserva su identidad original y puede seguir entrando.
        Usuario intacto = usuarioRepository.findById(vendedor.getIdUsuario()).orElseThrow();
        assertTrue(intacto.getUsuario().equals(CuentasTest.USUARIO_VENDEDOR));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(status().isOk());
    }

    @Test
    void elSuperAdminEditaDatosRolYClaveDelPersonal() throws Exception {
        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR).orElseThrow();

        mockMvc.perform(put(usuarioUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "vendedor-nuevo",
                                "nombre", "Vendedor",
                                "apellido", "Actualizado",
                                "correo", "vendedor.nuevo@prueba.com",
                                "rol", "ADMIN",
                                "password", "StaffNueva#2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario", is("vendedor-nuevo")))
                .andExpect(jsonPath("$.nombreCompleto", is("Vendedor Actualizado")))
                .andExpect(jsonPath("$.correo", is("vendedor.nuevo@prueba.com")))
                .andExpect(jsonPath("$.rol", is("ADMIN")));

        // La clave y el nombre de usuario anteriores dejan de funcionar.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("vendedor-nuevo", "StaffNueva#2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")));

        // El token emitido con el rol anterior ya no sirve.
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_ACTUALIZADO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].descripcion", containsString("vendedor-nuevo")));
        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_PASSWORD_RESTABLECIDO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALERTA")));
    }

    @Test
    void laEmpresaNoPuedeQuedarseSinAdministradorActivo() throws Exception {
        Usuario admin = usuarioRepository.findByUsuario(CuentasTest.USUARIO_ADMIN).orElseThrow();

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, admin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put(usuarioUrl(tiendaA, admin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", CuentasTest.USUARIO_ADMIN, "nombre", "Administrador",
                                "apellido", "Prueba", "correo", CuentasTest.CORREO_ADMIN,
                                "rol", "VENDEDOR"))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete(usuarioUrl(tiendaA, admin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isConflict());

        // Con un segundo ADMIN activo, el bloqueo individual ya es posible.
        mockMvc.perform(post(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "admin2", "nombre", "Segundo", "apellido", "Admin",
                                "correo", "admin2@prueba.com", "rol", "ADMIN",
                                "password", "ClaveStaff#2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, admin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("SUSPENDIDO")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("suspendida")));

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, admin))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ACTIVO")));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void suspenderUnaCuentaDeStaffLeQuitaElAccesoDeInmediato() throws Exception {
        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR).orElseThrow();

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("SUSPENDIDO")));

        // El token vigente y un login nuevo quedan bloqueados; la tienda y el
        // resto del personal siguen operando.
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("suspendida")));
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ACTIVO")));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(status().isOk());

        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_SUSPENDIDO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALERTA")));
        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_ACTIVADO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));
    }

    @Test
    void elSuperAdminEliminaCuentasDeStaffSinHistorial() throws Exception {
        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR).orElseThrow();

        mockMvc.perform(delete(usuarioUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNoContent());

        assertTrue(usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR).isEmpty(),
                "La cuenta eliminada no debe seguir existiendo");
        assertTrue(personaRepository.findById(CuentasTest.ID_PERSONA_VENDEDOR).isEmpty(),
                "La ficha sin cuentas ni comprobantes también se elimina");

        mockMvc.perform(get(usuariosUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuario", is(CuentasTest.USUARIO_ADMIN)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete(usuarioUrl(tiendaA, vendedor))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/super-admin/auditoria?tipo=USUARIO_ELIMINADO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALERTA")));
    }

    @Test
    void elCrudDePersonalEsSoloDelSuperAdminYNoCruzaEmpresas() throws Exception {
        for (String token : List.of(tokenAdminA, tokenVendedor, tokenCliente)) {
            mockMvc.perform(get(usuariosUrl(tiendaA))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(usuariosUrl(tiendaA))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "usuario", "intruso", "nombre", "Intruso",
                                    "correo", "intruso@prueba.com", "rol", "ADMIN",
                                    "password", "ClaveStaff#2026"))))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get(usuariosUrl(tiendaA)))
                .andExpect(status().isUnauthorized());

        Usuario adminB = usuarioRepository.findByUsuario(USUARIO_ADMIN_B).orElseThrow();
        mockMvc.perform(put(usuarioUrl(tiendaA, adminB))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", USUARIO_ADMIN_B, "nombre", "Admin",
                                "apellido", "Empresa Dos", "correo", CORREO_ADMIN_B,
                                "rol", "VENDEDOR"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(usuarioUrl(tiendaA, adminB))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isNotFound());

        // La cuenta global no es personal de ninguna tienda.
        Usuario superCuenta = usuarioRepository.findByUsuario(CuentasTest.USUARIO_SUPER).orElseThrow();
        mockMvc.perform(put(usuarioEstadoUrl(tiendaA, superCuenta))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());
    }

    private void crearAdminDeEmpresaB() {
        Persona persona = new Persona();
        persona.setIdPersona(ID_PERSONA_ADMIN_B);
        persona.setNombre("Admin");
        persona.setApellido("Empresa Dos");
        persona.setCorreo(CORREO_ADMIN_B);
        persona.setTelefono("987654320");
        persona.setUsuario(USUARIO_ADMIN_B);
        personaRepository.save(persona);

        Usuario usuario = new Usuario(USUARIO_ADMIN_B, CuentasTest.HASH_123456,
                ID_PERSONA_ADMIN_B, Rol.ADMIN);
        usuario.setIdTienda(tiendaB.getIdTienda());
        usuarioRepository.save(usuario);
    }

    private void guardarProducto(Tienda tienda, int id, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
    }

    private String registrarVenta(String token, int idProducto, int cantidad) throws Exception {
        Map<String, Object> body = Map.of(
                "idCliente", CuentasTest.ID_CLIENTE,
                "tipoDocumento", "Boleta",
                "medioPago", "Efectivo",
                "items", List.of(Map.of("idProducto", idProducto, "cantidad", cantidad)));
        String respuesta = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(respuesta);
        return json.get("id").asText();
    }

    private String credenciales(String usuario) throws Exception {
        return credenciales(usuario, CuentasTest.CLAVE);
    }

    private String credenciales(String usuario, String clave) throws Exception {
        return objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave));
    }

    private String producto(int idProducto, String nombre) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "idProducto", idProducto,
                "nombre", nombre,
                "stock", 4,
                "precio", 45.0,
                "categoria", "Accesorios"));
    }

    private String estadoUrl(Tienda tienda) {
        return "/api/super-admin/empresas/" + tienda.getIdTienda() + "/estado";
    }

    private String planUrl(Tienda tienda) {
        return "/api/super-admin/empresas/" + tienda.getIdTienda() + "/plan";
    }

    private String adminUrl(Tienda tienda) {
        return "/api/super-admin/empresas/" + tienda.getIdTienda() + "/admin";
    }

    private String adminPasswordUrl(Tienda tienda) {
        return adminUrl(tienda) + "/password";
    }

    private String usuariosUrl(Tienda tienda) {
        return "/api/super-admin/empresas/" + tienda.getIdTienda() + "/usuarios";
    }

    private String usuarioUrl(Tienda tienda, Usuario usuario) {
        return usuariosUrl(tienda) + "/" + usuario.getIdUsuario();
    }

    private String usuarioEstadoUrl(Tienda tienda, Usuario usuario) {
        return usuarioUrl(tienda, usuario) + "/estado";
    }
}
