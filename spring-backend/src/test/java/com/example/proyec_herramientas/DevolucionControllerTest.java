package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Credito;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.CreditoRepository;
import com.example.proyec_herramientas.repository.DevolucionRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevolucionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DevolucionRepository devolucionRepository;

    @Autowired
    private CreditoRepository creditoRepository;

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
    void limpiar() throws Exception {
        creditoRepository.deleteAll();
        devolucionRepository.deleteAll();
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();

        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        var tienda = tiendaRepository.findBySlug(CuentasTest.SLUG_TIENDA).orElseThrow();
        token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);

        Producto producto = new Producto(1, "Mouse Gamer", 10, new BigDecimal("50.00"), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);

        clienteRepository.save(new Cliente("CLI-0001", "Juan", "Perez", "987654321", "juan@correo.com"));

        // El registro de venta exige la persona de mostrador (PER001).
        Persona persona = new Persona();
        persona.setIdPersona("PER001");
        persona.setNombre("Juan");
        persona.setApellido("Perez");
        persona.setCorreo("juan@correo.com");
        persona.setTelefono("987654321");
        persona.setUsuario("juan");
        personaRepository.save(persona);
    }

    @Test
    void flujoCompletoRestauraStockGeneraCreditoYNoTocaLaVenta() throws Exception {
        String idVenta = registrarVenta(2);

        // Detalle para devolucion: linea vendida sin unidades devueltas aun.
        mockMvc.perform(get("/api/devoluciones/ventas/{id}", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idVenta)))
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].idProducto", is(1)))
                .andExpect(jsonPath("$.detalles[0].cantidad", is(2)))
                .andExpect(jsonPath("$.detalles[0].cantidadDevuelta", is(0)));

        // Devolucion parcial: 1 unidad. Stock 8 -> 9 y credito por S/ 50.
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devolucionBody(idVenta, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVenta", is(idVenta)))
                .andExpect(jsonPath("$.total", is(50.0)))
                .andExpect(jsonPath("$.creditoCodigo", is("BOL-CR-000001")))
                .andExpect(jsonPath("$.detalles[0].cantidad", is(1)));

        assertEquals(9, productoRepository.findById(1).orElseThrow().getStock());

        // La venta original queda intacta (integridad documental).
        var venta = ventaRepository.findById(idVenta).orElseThrow();
        assertEquals("LEGACY", venta.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(venta.getTotalVenta()));

        // El credito nace Vigente por el total devuelto.
        Credito credito = creditoRepository.findByCodigo("BOL-CR-000001").orElseThrow();
        assertEquals("Vigente", credito.getEstado());
        assertEquals(0, new BigDecimal("50.00").compareTo(credito.getMontoTotal()));
        assertEquals(0, new BigDecimal("50.00").compareTo(credito.getMontoDisponible()));
        assertEquals(idVenta, credito.getIdVentaOrigen());
        assertEquals("Juan Perez", credito.getClienteNombre());
        // La boleta del mostrador no llevo DNI: el credito queda sin documento.
        assertEquals("", credito.getDniCliente());

        // El detalle ya refleja la unidad devuelta.
        mockMvc.perform(get("/api/devoluciones/ventas/{id}", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].cantidadDevuelta", is(1)));

        // Devolucion del resto: stock 9 -> 10 y segundo credito secuencial.
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devolucionBody(idVenta, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditoCodigo", is("BOL-CR-000002")));

        assertEquals(10, productoRepository.findById(1).orElseThrow().getStock());
        assertEquals("LEGACY", ventaRepository.findById(idVenta).orElseThrow().getEstado());

        // No se puede devolver mas de lo vendido.
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devolucionBody(idVenta, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaDevolverMasUnidadesDeLasVendidas() throws Exception {
        String idVenta = registrarVenta(2);

        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devolucionBody(idVenta, 3)))
                .andExpect(status().isBadRequest());

        // Ni el stock ni el credito se movieron.
        assertEquals(8, productoRepository.findById(1).orElseThrow().getStock());
        assertTrue(creditoRepository.findAll().isEmpty());
    }

    @Test
    void rechazaProductoQueNoEstaEnLaVenta() throws Exception {
        String idVenta = registrarVenta(1);
        Map<String, Object> body = Map.of(
                "idVenta", idVenta,
                "motivo", "Cambio",
                "items", List.of(Map.of("idProducto", 999, "cantidad", 1)));

        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vendedorPuedeDevolverYClienteNo() throws Exception {
        String idVenta = registrarVenta(1);

        CuentasTest.crearVendedor(usuarioRepository, personaRepository, tiendaRepository);
        String tokenVendedor = CuentasTest.token(mockMvc, objectMapper,
                CuentasTest.USUARIO_VENDEDOR, CuentasTest.CLAVE);
        mockMvc.perform(get("/api/devoluciones/ventas/{id}", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor))
                .andExpect(status().isOk());

        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenCliente = CuentasTest.token(mockMvc, objectMapper,
                CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE);
        mockMvc.perform(get("/api/devoluciones/ventas/{id}", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devolucionBody(idVenta, 1)))
                .andExpect(status().isForbidden());
    }

    private String registrarVenta(int cantidad) throws Exception {
        Map<String, Object> body = Map.of(
                "idCliente", "CLI-0001",
                "tipoDocumento", "Boleta",
                "medioPago", "Yape",
                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad)));
        String respuesta = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private String devolucionBody(String idVenta, int cantidad) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "idVenta", idVenta,
                "motivo", "Producto defectuoso",
                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad))));
    }
}
