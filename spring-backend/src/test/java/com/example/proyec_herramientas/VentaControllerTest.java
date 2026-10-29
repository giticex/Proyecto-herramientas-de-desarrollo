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
import com.example.proyec_herramientas.repository.VentaRepository;
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

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
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
    private JdbcTemplate jdbcTemplate;

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

    // Registrar y consultar ventas es del ADMIN: las pruebas entran con su token.
    private String token;

    private Tienda tienda;

    @BeforeEach
    void limpiar() throws Exception {
        // Devoluciones y creditos de otras clases de prueba referencian ventas.
        jdbcTemplate.update("DELETE FROM credito");
        jdbcTemplate.update("DELETE FROM devolucion_detalle");
        jdbcTemplate.update("DELETE FROM devolucion");
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        // Las cuentas apuntan a una persona: se borran antes que las personas.
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();

        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        tienda = tiendaRepository.findBySlug(CuentasTest.SLUG_TIENDA).orElseThrow();
        token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);

        guardarProducto(1, "Mouse Gamer", 8, "50.00");
        guardarProducto(2, "Teclado Mecánico", 5, "120.00");
        clienteRepository.save(new Cliente("CLI-0001", "Juan", "Pérez", "987654321", "juan@correo.com"));
        personaRepository.save(persona("PER001", "Juan", "Pérez", "juan@correo.com"));
    }

    private void guardarProducto(int id, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
    }

    @Test
    void totalDeBoletaEsSumaDeItems() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(
                                item(1, 2), item(2, 1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoDocumento", is("TD001")))
                .andExpect(jsonPath("$.tipoDocumento", is("Boleta")))
                .andExpect(jsonPath("$.serie", is("B001")))
                .andExpect(jsonPath("$.numeroDocumento", matchesPattern("^B001-\\d{6}$")))
                .andExpect(jsonPath("$.idMedioPago", is("MP004")))
                .andExpect(jsonPath("$.medioPago", is("Yape")))
                .andExpect(jsonPath("$.cliente", is("Juan Pérez")))
                .andExpect(jsonPath("$.total", is(220.0)))
                // El IGV se calcula sobre el porcentaje configurado (18%): el
                // precio unitario incluye impuesto y la base es precio / 1.18.
                .andExpect(jsonPath("$.totalGravada", is(186.44)))
                .andExpect(jsonPath("$.totalIgv", is(33.56)))
                .andExpect(jsonPath("$.detalles", hasSize(2)))
                .andExpect(jsonPath("$.detalles[0].producto", is("Mouse Gamer")))
                .andExpect(jsonPath("$.detalles[0].subtotal", is(100.0)))
                .andExpect(jsonPath("$.detalles[0].igvItem", is(15.25)));
    }

    @Test
    void boletasSoloDevuelveTipoBoleta() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 1)))));
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Factura", "Efectivo", List.of(item(1, 1)))));

        mockMvc.perform(get("/api/boletas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoDocumento", is("Boleta")))
                .andExpect(jsonPath("$[0].total", is(50.0)));
    }

    @Test
    void historialDevuelveVentasDelCliente() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 2)))))
                .andExpect(status().isCreated())
                .andReturn();

        String idVenta = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/historial").param("clienteId", "CLI-0001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idVenta)))
                .andExpect(jsonPath("$[0].cliente", is("Juan Pérez")))
                .andExpect(jsonPath("$[0].detalles[0].producto", is("Mouse Gamer")));
    }

    @Test
    void historialGeneralDevuelveTodasLasVentas() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Efectivo", List.of(item(1, 1)))));

        mockMvc.perform(get("/api/historial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoDocumento", is("Boleta")));
    }

    @Test
    void ventaSinClienteDevuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("  ", "Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ventaConClienteDesconocidoDevuelve404() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-9999", "Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void ventaDescartaElStock() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 2)))));

        mockMvc.perform(get("/api/productos/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(6)));
    }

    @Test
    void ventaConStockInsuficienteDevuelve409() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 99)))))
                .andExpect(status().isConflict());
    }

    @Test
    void generaPdfDeBoleta() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        String idVenta = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        MvcResult pdf = mockMvc.perform(get("/api/boleta/" + idVenta + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 1000, "El PDF debe tener contenido");
        assertTrue(new String(bytes, 0, 4).startsWith("%PDF"), "Debe ser un PDF válido");
    }

    @Test
    void elClienteVeSusPropiasComprasPorVentasMias() throws Exception {
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);

        // El ADMIN registra una compra a nombre del comprador de las pruebas.
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito(CuentasTest.ID_CLIENTE, "Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isCreated());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        mockMvc.perform(get("/api/ventas/mias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].detalles[0].producto", is("Mouse Gamer")));
    }

    @Test
    void elClienteCompraSiempreANombrePropioIgnorandoElBody() throws Exception {
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        // El cuerpo intenta comprar a nombre de CLI-0001, pero el CLIENTE solo
        // puede comprar como el mismo: se registra como CLI-PRUEBA.
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("CLI-0001", "Boleta", "Yape", List.of(item(1, 1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente", is(CuentasTest.ID_CLIENTE)));
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

    private String carrito(String idCliente, String tipoDocumento, String medioPago, List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = Map.of(
                "idCliente", idCliente,
                "tipoDocumento", tipoDocumento,
                "medioPago", medioPago,
                "items", items);
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> item(int idProducto, int cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }
}
