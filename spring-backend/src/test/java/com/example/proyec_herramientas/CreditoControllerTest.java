package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditoControllerTest {

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
    private String idVenta;

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
        Persona persona = new Persona();
        persona.setIdPersona("PER001");
        persona.setNombre("Juan");
        persona.setApellido("Perez");
        persona.setCorreo("juan@correo.com");
        persona.setTelefono("987654321");
        persona.setUsuario("juan");
        personaRepository.save(persona);

        // Venta de 2 unidades (S/ 100) y devolucion de 1 -> credito de S/ 50.
        idVenta = objectMapper.readTree(registrarVenta(2, null)).get("id").asText();
        devolver(idVenta, 1);
    }

    @Test
    void listarDevuelveLosCreditosGenerados() throws Exception {
        mockMvc.perform(get("/api/creditos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("BOL-CR-000001")))
                .andExpect(jsonPath("$[0].numeroDocumentoOrigen").isNotEmpty())
                .andExpect(jsonPath("$[0].clienteNombre", is("Juan Perez")))
                .andExpect(jsonPath("$[0].montoTotal", is(50.0)))
                .andExpect(jsonPath("$[0].montoDisponible", is(50.0)))
                .andExpect(jsonPath("$[0].estado", is("Vigente")));
    }

    @Test
    void buscarPorCodigoDevuelveElSaldo() throws Exception {
        mockMvc.perform(get("/api/creditos/codigo/{codigo}", "bol-cr-000001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("BOL-CR-000001")))
                .andExpect(jsonPath("$.montoDisponible", is(50.0)));

        mockMvc.perform(get("/api/creditos/codigo/{codigo}", "BOL-CR-999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void canjearParcialRestaElSaldoYCanjearTodoLoMarcaUsado() throws Exception {
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "20.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoDisponible", is(30.0)))
                .andExpect(jsonPath("$.estado", is("Vigente")));

        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "30.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoDisponible", is(0.0)))
                .andExpect(jsonPath("$.estado", is("Usado")));

        // Sin saldo ya no se puede canjear.
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "1.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noPermiteCanjearMasQueElSaldoDisponible() throws Exception {
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "60.00")))
                .andExpect(status().isBadRequest());

        assertEquals(0, new BigDecimal("50.00").compareTo(
                creditoRepository.findByCodigo("BOL-CR-000001").orElseThrow().getMontoDisponible()));
    }

    @Test
    void reembolsarDejaElSaldoEnCeroYNoSePuedeRepetir() throws Exception {
        mockMvc.perform(post("/api/creditos/reembolsar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("codigo", "BOL-CR-000001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Reembolsado")))
                .andExpect(jsonPath("$.montoDisponible", is(0.0)));

        mockMvc.perform(post("/api/creditos/reembolsar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("codigo", "BOL-CR-000001"))))
                .andExpect(status().isBadRequest());

        // Un credito reembolsado ya no se puede canjear.
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "10.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ventaConCreditoConsumeElSaldoYDevuelveElResto() throws Exception {
        // Venta de 1 unidad (S/ 50) pagada integramente con el credito de S/ 50.
        String respuesta = registrarVenta(1, "BOL-CR-000001");

        assertEquals(0, new BigDecimal("50.00").compareTo(
                new BigDecimal(objectMapper.readTree(respuesta).get("creditoMontoUsado").asText())));
        assertEquals(0, new BigDecimal("0.00").compareTo(
                new BigDecimal(objectMapper.readTree(respuesta).get("creditoSaldoRestante").asText())));
        assertEquals("Usado",
                creditoRepository.findByCodigo("BOL-CR-000001").orElseThrow().getEstado());
    }

    @Test
    void siElCreditoNoCubreElTotalElRestoSePagaConOtroMetodo() throws Exception {
        // Venta de 2 unidades (S/ 100) con un credito de S/ 50: cubre la mitad.
        String respuesta = registrarVenta(2, "BOL-CR-000001");

        assertEquals(0, new BigDecimal("50.00").compareTo(
                new BigDecimal(objectMapper.readTree(respuesta).get("creditoMontoUsado").asText())));
        assertEquals(0, new BigDecimal("0.00").compareTo(
                new BigDecimal(objectMapper.readTree(respuesta).get("creditoSaldoRestante").asText())));
        // El comprobante mantiene el importe completo (el saldo resta se cobra aparte).
        assertEquals(0, new BigDecimal("100.00").compareTo(
                new BigDecimal(objectMapper.readTree(respuesta).get("totalVenta").asText())));
    }

    @Test
    void unCreditoInexistenteNoPermiteEmitirLaVenta() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idCliente", "CLI-0001",
                                "tipoDocumento", "Boleta",
                                "medioPago", "Efectivo",
                                "creditoCodigo", "BOL-CR-999999",
                                "items", List.of(Map.of("idProducto", 1, "cantidad", 1))))))
                .andExpect(status().isNotFound());

        // La transaccion completa se revierte: el stock sigue en 9 (10 - 1 venta).
        assertEquals(9, productoRepository.findById(1).orElseThrow().getStock());
    }

    @Test
    void clienteNoPuedeAccederALosCreditos() throws Exception {
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenCliente = CuentasTest.token(mockMvc, objectMapper,
                CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE);

        mockMvc.perform(get("/api/creditos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(canjeBody("BOL-CR-000001", "10.00")))
                .andExpect(status().isForbidden());
    }

    private String registrarVenta(int cantidad, String creditoCodigo) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "idCliente", "CLI-0001",
                "tipoDocumento", "Boleta",
                "medioPago", "Efectivo",
                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad))));
        if (creditoCodigo != null) {
            body.put("creditoCodigo", creditoCodigo);
        }
        return mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void devolver(String idVenta, int cantidad) throws Exception {
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idVenta", idVenta,
                                "motivo", "Producto defectuoso",
                                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad))))))
                .andExpect(status().isCreated());
    }

    private String canjeBody(String codigo, String monto) throws Exception {
        return objectMapper.writeValueAsString(Map.of("codigo", codigo, "monto", new BigDecimal(monto)));
    }
}
