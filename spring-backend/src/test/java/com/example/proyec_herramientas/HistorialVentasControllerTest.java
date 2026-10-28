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
class HistorialVentasControllerTest {

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

        Producto producto = new Producto(1, "Mouse Gamer", 10, new BigDecimal("50.00"), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);

        clienteRepository.save(new Cliente("CLI-0001", "Juan", "Perez", "987654321", "juan@correo.com"));
        personaRepository.save(persona("PER001", "Juan", "Perez", "juan@correo.com"));
    }

    @Test
    void historialListaVentasDeLaTiendaConDatosDeLaTabla() throws Exception {
        String idVenta = registrarVenta(2);

        mockMvc.perform(get("/api/ventas")
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
        String numero = jdbcTemplate.queryForObject(
                "SELECT numero_documento FROM venta WHERE id_venta = ?", String.class, idVenta);

        mockMvc.perform(get("/api/ventas")
                        .param("q", numero)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));

        // En el flujo real el documento del adquirente lo completa la emision
        // del comprobante; aqui se simula esa venta ya emitida.
        jdbcTemplate.update("UPDATE venta SET receptor_num_documento = ? WHERE id_venta = ?",
                "12345678", idVenta);

        mockMvc.perform(get("/api/ventas")
                        .param("q", "12345678")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].documentoCliente", is("12345678")));

        mockMvc.perform(get("/api/ventas")
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

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
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
