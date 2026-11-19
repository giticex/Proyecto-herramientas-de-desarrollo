package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HistorialVentasControllerTest {

    @Autowired
    private com.example.proyec_herramientas.service.TokenService tokens;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private String token;

    @BeforeEach
    void prepararDatos() throws Exception {
        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        Tienda tienda = tiendaRepository.findBySlug(CuentasTest.SLUG_TIENDA).orElseThrow();
        token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);

        Producto producto = new Producto(1, "Mouse Gamer", 10, new BigDecimal("50.00"), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);

        clienteRepository.save(new Cliente("CLI-0001", "Juan", "Perez", "987654321", "juan@correo.com"));
        personaRepository.save(persona("PER001", "Juan", "Perez", "juan@correo.com"));
    }

    @Test
    void historialListaVentasDeLaTiendaConDatosDeLaTabla() throws Exception {
        String idVenta = registrarVenta(2);

        mockMvc.perform(get("/ventas")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].idVenta", is(idVenta)))
                .andExpect(jsonPath("$.contenido[0].numeroDocumento").isNotEmpty())
                .andExpect(jsonPath("$.contenido[0].cliente", is("Juan Perez")))
                .andExpect(jsonPath("$.contenido[0].total", is(100.0)))
                .andExpect(jsonPath("$.contenido[0].estado").isNotEmpty())
                .andExpect(jsonPath("$.contenido[0].fechaEmision").isNotEmpty());
    }

    @Test
    void historialFiltraPorNumeroDeComprobanteYDocumentoDelCliente() throws Exception {
        String idVenta = registrarVenta(1);
        entityManager.flush();
        String numero = jdbcTemplate.queryForObject(
                "SELECT numero_documento FROM venta WHERE id_venta = ?", String.class, idVenta);

        mockMvc.perform(get("/ventas")
                        .param("q", numero)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));

        // En el flujo real el documento del adquirente lo completa la emision
        // del comprobante; aqui se simula esa venta ya emitida.
        jdbcTemplate.update("UPDATE venta SET receptor_num_documento = ? WHERE id_venta = ?",
                "12345678", idVenta);
        entityManager.clear();

        mockMvc.perform(get("/ventas")
                        .param("q", "12345678")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].documentoCliente", is("12345678")));

        mockMvc.perform(get("/ventas")
                        .param("q", "99999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(0)))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void historialEsSoloParaAdmin() throws Exception {
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        mockMvc.perform(get("/ventas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void dosTiendasNoCompartenClientesVentasNiComprobantes() throws Exception {
        String ventaA = registrarVenta(1);
        String tokenB = tokenDeOtraTienda();
        Tienda tiendaB = tiendaRepository.findBySlug("negocio-b").orElseThrow();
        Producto productoB = new Producto(2, "Producto B", 8, new BigDecimal("25.00"), "Accesorios");
        productoB.setIdTienda(tiendaB.getIdTienda());
        productoRepository.save(productoB);
        clienteRepository.save(new Cliente("CLI-0002", "Marta", "Torres", "999999999", "marta@example.com"));
        personaRepository.save(persona("PER002", "Marta", "Torres", "marta@example.com"));
        String ventaB = registrarVenta(1, tokenB, "CLI-0002", 2);

        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idCliente", is("CLI-0001")));
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idCliente", is("CLI-0002")));
        mockMvc.perform(get("/ventas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].idVenta", is(ventaA)));
        mockMvc.perform(get("/historial").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ventaB)));

        for (String sufijo : List.of("", "/pdf", "/xml", "/qr")) {
            mockMvc.perform(get("/comprobantes/{venta}" + sufijo, ventaB)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/comprobantes/{venta}" + sufijo, ventaA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/boleta/{venta}/pdf", ventaB).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/comprobantes/cliente/CLI-0002").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/comprobantes/{venta}", ventaA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/comprobantes/{venta}", ventaA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idVenta", is(ventaA)));

        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenComprador = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE);
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComprador))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/comprobantes/cliente/CLI-0002").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComprador))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosVenta(1, "CLI-0001", 2))))
                .andExpect(status().isNotFound());
    }

    private String tokenDeOtraTienda() throws Exception {
        Map<String, String> datos = Map.of("nombre", "Negocio B", "rubro", "Textil", "slug", "negocio-b",
                "correo", "dueno.b@example.com", "contrasena", CuentasTest.CLAVE.repeat(2));
        String respuesta = mockMvc.perform(post("/auth/registro-tienda")
                        .header("Authorization", "Bearer " + tokens.generar("maestro-pruebas", "SUPER_ADMIN", "", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private String registrarVenta(int cantidad) throws Exception {
        return registrarVenta(cantidad, token, "CLI-0001", 1);
    }

    private String registrarVenta(int cantidad, String token, String cliente, int producto) throws Exception {
        String respuesta = mockMvc.perform(post("/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosVenta(cantidad, cliente, producto))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private Map<String, Object> datosVenta(int cantidad, String cliente, int producto) {
        return Map.of("idCliente", cliente, "tipoDocumento", "Boleta", "medioPago", "Yape",
                "items", List.of(Map.of("idProducto", producto, "cantidad", cantidad)));
    }

    private Persona persona(String idPersona, String nombre, String apellido, String correo) {
        Persona persona = new Persona();
        persona.setIdPersona(idPersona);
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCorreo(correo);
        persona.setTelefono("987654321");
        persona.setUsuario(nombre.toLowerCase());
        return persona;
    }
}
