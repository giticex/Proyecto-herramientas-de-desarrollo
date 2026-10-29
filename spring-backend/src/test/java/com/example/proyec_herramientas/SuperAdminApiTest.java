package com.example.proyec_herramientas;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        return objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", CuentasTest.CLAVE));
    }
}
