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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Busqueda y alta rapida de compradores desde el POS: la tienda sale del token,
// el historial de comprobantes autocompleta nombres y la venta guarda la
// direccion y el cliente elegido.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteBusquedaApiTest {

    private static final String BUSCAR_URL = "/api/clientes/buscar";
    private static final String CLIENTES_URL = "/api/clientes";
    private static final String SLUG_EMPRESA_B = "empresa-dos";
    private static final String USUARIO_ADMIN_B = "adminb";
    private static final String CORREO_ADMIN_B = "adminb@prueba.com";
    private static final String ID_PERSONA_ADMIN_B = "PER-PRUEBA-ADMB";
    private static final int PRODUCTO_TIENDA_A = 1;
    private static final int PRODUCTO_TIENDA_B = 900;

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
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenVendedor;
    private String tokenCliente;
    private String tokenSuper;
    private int correlativoJdbc;

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
        tiendaRepository.deleteAll();
        correlativoJdbc = 0;

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
        crearProducto(PRODUCTO_TIENDA_A, tiendaA, "Polo basico", 50, "50.00");
        crearProducto(PRODUCTO_TIENDA_B, tiendaB, "Gorra", 50, "30.00");

        tokenAdminA = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        tokenAdminB = CuentasTest.token(mockMvc, objectMapper, USUARIO_ADMIN_B, CuentasTest.CLAVE);
        tokenVendedor = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_VENDEDOR,
                CuentasTest.CLAVE);
        tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        tokenSuper = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_SUPER,
                CuentasTest.CLAVE);
    }

    @Test
    void buscaPorDniConLaFichaDeLaTienda() throws Exception {
        JsonNode creado = registrar(tokenAdminA, "12345678", "María Quispe", "Av. Los Álamos 123");
        assertEquals("REGISTRADO", creado.get("origen").asText());
        assertTrue(creado.get("encontrado").asBoolean());

        JsonNode hallado = buscar(tokenAdminA, "12345678");
        assertTrue(hallado.get("encontrado").asBoolean());
        assertEquals(creado.get("idCliente").asText(), hallado.get("idCliente").asText());
        assertEquals("DNI", hallado.get("tipoDocumento").asText());
        assertEquals("María Quispe", hallado.get("nombre").asText());
        assertEquals("Av. Los Álamos 123", hallado.get("direccion").asText());
        assertEquals("TIENDA", hallado.get("origen").asText());

        // El documento se normaliza: puntos y espacios se ignoran.
        assertTrue(buscar(tokenAdminA, "12.345.678").get("encontrado").asBoolean());
    }

    @Test
    void buscaPorRucConRazonSocialYDireccion() throws Exception {
        registrar(tokenAdminA, "20123456789", "Comercial Andina SAC", "Av. Industrial 450");

        JsonNode hallado = buscar(tokenAdminA, "20123456789");
        assertTrue(hallado.get("encontrado").asBoolean());
        assertEquals("RUC", hallado.get("tipoDocumento").asText());
        assertEquals("Comercial Andina SAC", hallado.get("nombre").asText());
        assertEquals("Av. Industrial 450", hallado.get("direccion").asText());
        assertEquals("TIENDA", hallado.get("origen").asText());
    }

    @Test
    void elHistorialDeVentasAutocompletaSinFicha() throws Exception {
        insertarVenta(PRODUCTO_TIENDA_A, "87654321", "Juan Pérez", "Jr. Puno 456");

        JsonNode hallado = buscar(tokenAdminA, "87654321");
        assertTrue(hallado.get("encontrado").asBoolean());
        assertEquals("HISTORIAL", hallado.get("origen").asText());
        assertEquals("Juan Pérez", hallado.get("nombre").asText());
        assertEquals("Jr. Puno 456", hallado.get("direccion").asText());
        assertTrue(hallado.get("idCliente").isNull());
    }

    @Test
    void laBusquedaNoCruzaTiendas() throws Exception {
        registrar(tokenAdminB, "55555555", "Cliente de Empresa Dos", "Calle B 100");
        insertarVenta(PRODUCTO_TIENDA_B, "44444444", "Historial de Dos", "Calle B 200");

        assertFalse(buscar(tokenAdminA, "55555555").get("encontrado").asBoolean(),
                "El ADMIN no debe ver las fichas de otra tienda");
        assertFalse(buscar(tokenAdminA, "44444444").get("encontrado").asBoolean(),
                "El historial de otra tienda no debe autocompletar");

        assertTrue(buscar(tokenAdminB, "55555555").get("encontrado").asBoolean());
        assertEquals("HISTORIAL", buscar(tokenAdminB, "44444444").get("origen").asText());
    }

    @Test
    void rechazaDuplicadosYDocumentosInvalidos() throws Exception {
        registrar(tokenAdminA, "12345678", "María Quispe", "Av. Los Álamos 123");

        registrarAccion(tokenAdminA, "12345678", "Otra Ficha", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Ya existe un cliente")));

        registrarAccion(tokenAdminA, "123", "Documento Corto", "")
                .andExpect(status().isBadRequest());
        registrarAccion(tokenAdminA, "87654321", "   ", "")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BUSCAR_URL).param("documento", "123456789")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BUSCAR_URL).param("documento", "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laBusquedaEsSoloParaElPersonalDeLaTienda() throws Exception {
        mockMvc.perform(get(BUSCAR_URL).param("documento", "12345678"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BUSCAR_URL).param("documento", "12345678")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BUSCAR_URL).param("documento", "12345678")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CLIENTES_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("12345678", "X", ""))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CLIENTES_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("12345678", "X", ""))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BUSCAR_URL).param("documento", "12345678")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isOk());
        mockMvc.perform(post(CLIENTES_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("22222222", "Cliente del Vendedor", "Calle 1"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isCreated());
    }

    @Test
    void laVentaDelPosGuardaElClienteLaDireccionYElVendedor() throws Exception {
        JsonNode creado = registrar(tokenVendedor, "87654321", "Lucía Ramos", "Calle Lima 789");
        String idCliente = creado.get("idCliente").asText();

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipoDocumento", "Boleta");
        cuerpo.put("medioPago", "Efectivo");
        cuerpo.put("idCliente", idCliente);
        cuerpo.put("documentoCliente", "87654321");
        cuerpo.put("razonSocial", "Lucía Ramos");
        cuerpo.put("direccionCliente", "Calle Lima 789");
        cuerpo.put("items", List.of(Map.of("idProducto", PRODUCTO_TIENDA_A, "cantidad", 1)));

        MvcResult resultado = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode venta = objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
        String idVenta = venta.get("id").asText();

        Map<String, Object> fila = jdbcTemplate.queryForMap("""
                SELECT id_cliente, receptor_num_documento, receptor_razon_social,
                       receptor_direccion, vendedor_usuario, vendedor_nombre
                FROM venta WHERE id_venta = ?
                """, idVenta);
        assertEquals(idCliente, fila.get("id_cliente"));
        assertEquals("87654321", fila.get("receptor_num_documento"));
        assertEquals("Lucía Ramos", fila.get("receptor_razon_social"));
        assertEquals("Calle Lima 789", fila.get("receptor_direccion"));
        assertEquals(CuentasTest.USUARIO_VENDEDOR, fila.get("vendedor_usuario"));
        assertEquals("Vendedor Prueba", fila.get("vendedor_nombre"));
    }

    private JsonNode buscar(String token, String documento) throws Exception {
        MvcResult resultado = mockMvc.perform(get(BUSCAR_URL)
                        .param("documento", documento)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private JsonNode registrar(String token, String documento, String nombre, String direccion)
            throws Exception {
        MvcResult resultado = registrarAccion(token, documento, nombre, direccion)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private ResultActions registrarAccion(String token, String documento, String nombre,
            String direccion) throws Exception {
        return mockMvc.perform(post(CLIENTES_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoRegistro(documento, nombre, direccion)));
    }

    private String cuerpoRegistro(String documento, String nombre, String direccion)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("documento", documento);
        cuerpo.put("nombre", nombre);
        cuerpo.put("direccion", direccion);
        return objectMapper.writeValueAsString(cuerpo);
    }

    // Venta historica insertada sin pasar por el POS para probar el
    // autocompletado desde comprobantes ya emitidos.
    private void insertarVenta(int idProducto, String receptorDocumento, String razonSocial,
            String direccion) {
        String id = UUID.randomUUID().toString();
        int correlativo = ++correlativoJdbc;
        jdbcTemplate.update("""
                INSERT INTO venta (id_venta, id_cliente, id_persona, id_tipo_documento,
                    numero_documento, id_medio_pago, fecha_emision, serie, correlativo,
                    total_venta, receptor_tipo_documento, receptor_num_documento,
                    receptor_razon_social, receptor_direccion)
                VALUES (?, ?, ?, 'TD001', ?, 'MP001', ?, 'T999', ?, 50.00, '1', ?, ?, ?)
                """, id, CuentasTest.ID_CLIENTE, CuentasTest.ID_PERSONA_ADMIN,
                "T999-" + String.format("%06d", correlativo), Timestamp.valueOf(LocalDateTime.now()),
                correlativo, receptorDocumento, razonSocial, direccion);
        jdbcTemplate.update("""
                INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, 1, 50.00, 50.00)
                """, id, idProducto);
    }

    private void crearProducto(int id, Tienda tienda, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
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
}
