package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Producto;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenCliente;
    private String tokenAdmin;

    @BeforeEach
    void limpiar() throws Exception {
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        tiendaRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();

        int idTienda = registrarTienda("Tienda Uno", "tienda-uno", "admin@correo.com");
        tokenAdmin = loginToken("admin@correo.com", "clave1234");

        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));
        productoRepository.save(new Producto(2, "Teclado Mecánico", 5, 120.0, "Accesorios", idTienda));

        tokenCliente = registrarCliente("Juan", "Pérez", "juan@correo.com");
    }

    @Test
    void totalDeBoletaEsSumaDeItems() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 2), item(2, 1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoDocumento", is("TD001")))
                .andExpect(jsonPath("$.tipoDocumento", is("Boleta")))
                .andExpect(jsonPath("$.numeroDocumento", is("BOL-000001")))
                .andExpect(jsonPath("$.idMedioPago", is("MP004")))
                .andExpect(jsonPath("$.medioPago", is("Yape")))
                .andExpect(jsonPath("$.cliente", is("Juan Pérez")))
                .andExpect(jsonPath("$.total", is(220.0)))
                .andExpect(jsonPath("$.detalles", hasSize(2)))
                .andExpect(jsonPath("$.detalles[0].producto", is("Mouse Gamer")))
                .andExpect(jsonPath("$.detalles[0].subtotal", is(100.0)));
    }

    @Test
    void registrarVentaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarVentaComoAdminDevuelve403() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ventaSinItemsDevuelve400() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void boletasSoloDevuelveTipoBoleta() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 1)))));
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Factura", "Efectivo", List.of(item(1, 1)))));

        mockMvc.perform(get("/boletas").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoDocumento", is("Boleta")))
                .andExpect(jsonPath("$[0].total", is(50.0)));
    }

    @Test
    void misVentasDevuelveSoloLasDelClienteAutenticado() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 2)))))
                .andExpect(status().isCreated());

        String otroToken = registrarCliente("Ana", "Torres", "ana@correo.com");
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + otroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Efectivo", List.of(item(2, 1)))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/ventas/mias").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente", is("Juan Pérez")));
    }

    @Test
    void historialGeneralDevuelveTodasLasVentas() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Efectivo", List.of(item(1, 1)))));

        mockMvc.perform(get("/historial").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoDocumento", is("Boleta")));
    }

    @Test
    void ventaDescartaElStock() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 2)))));

        mockMvc.perform(get("/productos/1").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(6)));
    }

    @Test
    void ventaConStockInsuficienteDevuelve409YNoCambiaElStock() throws Exception {
        mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 99)))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/productos/1").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(8)));
    }

    @Test
    void generaPdfDeBoleta() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/ventas")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        String idVenta = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        MvcResult pdf = mockMvc.perform(get("/boleta/" + idVenta + "/pdf").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 1000, "El PDF debe tener contenido");
        assertTrue(new String(bytes, 0, 4).startsWith("%PDF"), "Debe ser un PDF válido");
    }

    private int registrarTienda(String nombre, String slug, String correo) throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", nombre, "rubro", "moda", "slug", slug,
                                "correo", correo, "contrasena", "clave1234"))))
                .andExpect(status().isCreated());
        return tiendaRepository.findBySlug(slug).orElseThrow().getIdTienda();
    }

    private String loginToken(String usuario, String contrasena) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("usuario", usuario, "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private String registrarCliente(String nombre, String apellido, String correo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", nombre, "apellido", apellido, "correo", correo,
                                "contrasena", "clave1234", "telefono", "987654321"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String carrito(String tipoDocumento, String medioPago, List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = Map.of(
                "tipoDocumento", tipoDocumento,
                "medioPago", medioPago,
                "items", items);
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> item(int idProducto, int cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }
}
