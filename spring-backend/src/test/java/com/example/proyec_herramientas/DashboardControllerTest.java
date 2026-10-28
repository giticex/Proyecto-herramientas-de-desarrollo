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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
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
class DashboardControllerTest {

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

    private String token;

    @BeforeEach
    void limpiar() throws Exception {
        // Devoluciones y creditos de otras clases de prueba referencian ventas.
        jdbcTemplate.update("DELETE FROM credito");
        jdbcTemplate.update("DELETE FROM devolucion_detalle");
        jdbcTemplate.update("DELETE FROM devolucion");
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();

        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        Tienda tienda = tiendaRepository.findBySlug(CuentasTest.SLUG_TIENDA).orElseThrow();
        token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);

        guardarProducto(tienda, 1, "Mouse Gamer", 8, "50.00");
        guardarProducto(tienda, 2, "Teclado Mecanico", 5, "120.00");

        clienteRepository.save(new Cliente("CLI-0001", "Juan", "Perez", "987654321", "juan@correo.com"));
        personaRepository.save(persona("PER001", "Juan", "Perez", "juan@correo.com"));
    }

    @Test
    void resumenSeparaVentasDeHoyYDelMes() throws Exception {
        // Dos ventas de hoy: 100 (2 x 50) + 120 (1 x 120).
        registrarVenta(List.of(item(1, 2)));
        registrarVenta(List.of(item(2, 1)));

        // Una venta del mes anterior: cuenta para el top historico, no para
        // "ventas de hoy" ni para "ingresos del mes".
        String idVieja = registrarVenta(List.of(item(1, 1)));
        LocalDate mesAnterior = LocalDate.now().minusMonths(1);
        jdbcTemplate.update("UPDATE venta SET fecha_emision = ? WHERE id_venta = ?",
                Timestamp.from(mesAnterior.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()),
                idVieja);

        mockMvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasHoy", is(220.0)))
                .andExpect(jsonPath("$.ingresosMes", is(220.0)))
                .andExpect(jsonPath("$.topProductos", hasSize(2)))
                .andExpect(jsonPath("$.topProductos[0].nombre", is("Mouse Gamer")))
                .andExpect(jsonPath("$.topProductos[0].cantidad", is(3)))
                .andExpect(jsonPath("$.topProductos[0].total", is(150.0)))
                .andExpect(jsonPath("$.topProductos[1].nombre", is("Teclado Mecanico")))
                .andExpect(jsonPath("$.topProductos[1].cantidad", is(1)));
    }

    @Test
    void resumenSinVentasDevuelveCeros() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasHoy", is(0.0)))
                .andExpect(jsonPath("$.ingresosMes", is(0.0)))
                .andExpect(jsonPath("$.topProductos", hasSize(0)));
    }

    @Test
    void resumenEsSoloParaAdmin() throws Exception {
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        mockMvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    private String registrarVenta(List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = Map.of(
                "idCliente", "CLI-0001",
                "tipoDocumento", "Boleta",
                "medioPago", "Yape",
                "items", items);
        String respuesta = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private void guardarProducto(Tienda tienda, int id, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
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

    private Map<String, Object> item(int idProducto, int cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }
}
