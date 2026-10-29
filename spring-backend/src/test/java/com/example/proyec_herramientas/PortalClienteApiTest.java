package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.CreditoRepository;
import com.example.proyec_herramientas.repository.DevolucionRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El portal del CLIENTE debe ver solo lo suyo. Se crean dos clientes con una
// devolucion cada uno: el login de uno no puede ver el credito del otro.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalClienteApiTest {

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

    private String tokenAdmin;
    private String tokenCliente;
    private String idVentaCliente;

    @AfterEach
    void limpiarDespues() {
        // Las ventas de este test dejarian filas en detalle_venta que impiden
        // a las demas clases de prueba borrar sus productos.
        creditoRepository.deleteAll();
        devolucionRepository.deleteAll();
        ventaRepository.deleteAll();
    }

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
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        clienteRepository.save(new Cliente("CLI-OTRO", "Otro", "Cliente", "912345678", "otro@correo.com"));
        Persona personaOtro = new Persona();
        personaOtro.setIdPersona("PER-OTRO");
        personaOtro.setNombre("Otro");
        personaOtro.setApellido("Cliente");
        personaOtro.setCorreo("otro@correo.com");
        personaOtro.setTelefono("912345678");
        personaOtro.setUsuario("otro");
        personaRepository.save(personaOtro);

        var tienda = tiendaRepository.findBySlug(CuentasTest.SLUG_TIENDA).orElseThrow();
        tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);
        tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE);

        Producto producto = new Producto(1, "Mouse Gamer", 10, new BigDecimal("50.00"), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);

        idVentaCliente = idDe(registrarVenta(CuentasTest.ID_CLIENTE, 2));
        devolver(idVentaCliente, 1);

        String idVentaOtro = idDe(registrarVenta("CLI-OTRO", 1));
        devolver(idVentaOtro, 1);
    }

    @Test
    void elClienteSoloVeSusPropiosCreditos() throws Exception {
        mockMvc.perform(get("/api/creditos/mios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("BOL-CR-000001")))
                .andExpect(jsonPath("$[0].clienteNombre", is("Comprador Prueba")))
                .andExpect(jsonPath("$[0].montoTotal", is(50.0)))
                .andExpect(jsonPath("$[0].estado", is("Vigente")));

        // El staff sigue viendo todos los creditos de la tienda.
        mockMvc.perform(get("/api/creditos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void elClienteVeSuPerfilConLasDireccionesDeSusComprobantes() throws Exception {
        Venta venta = ventaRepository.findById(idVentaCliente).orElseThrow();
        venta.setReceptorDireccion("Av. Siempre Viva 742");
        venta.setEntregaDireccion("Jr. Los Alamos 123");
        ventaRepository.save(venta);

        mockMvc.perform(get("/api/clientes/mi-perfil")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente", is(CuentasTest.ID_CLIENTE)))
                .andExpect(jsonPath("$.nombre", is("Comprador")))
                .andExpect(jsonPath("$.apellido", is("Prueba")))
                .andExpect(jsonPath("$.correo", is(CuentasTest.CORREO_CLIENTE)))
                .andExpect(jsonPath("$.telefono", is("987654321")))
                .andExpect(jsonPath("$.direcciones", hasSize(2)))
                .andExpect(jsonPath("$.direcciones[0].tipo", is("Dirección de facturación")))
                .andExpect(jsonPath("$.direcciones[0].direccion", is("Av. Siempre Viva 742")))
                .andExpect(jsonPath("$.direcciones[1].tipo", is("Dirección de entrega")))
                .andExpect(jsonPath("$.direcciones[1].direccion", is("Jr. Los Alamos 123")));
    }

    @Test
    void elStaffNoPuedeUsarLosEndpointsDelPortal() throws Exception {
        mockMvc.perform(get("/api/creditos/mios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clientes/mi-perfil")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void elClienteSigueSinAccederALaGestionDeCreditos() throws Exception {
        mockMvc.perform(get("/api/creditos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/creditos/canjear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("codigo", "BOL-CR-000001", "monto", new BigDecimal("10.00")))))
                .andExpect(status().isForbidden());
    }

    private String registrarVenta(String idCliente, int cantidad) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of(
                "idCliente", idCliente,
                "tipoDocumento", "Boleta",
                "medioPago", "Efectivo",
                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad))));
        return mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String idDe(String respuesta) throws Exception {
        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private void devolver(String idVenta, int cantidad) throws Exception {
        mockMvc.perform(post("/api/devoluciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idVenta", idVenta,
                                "motivo", "Producto defectuoso",
                                "items", List.of(Map.of("idProducto", 1, "cantidad", cantidad))))))
                .andExpect(status().isCreated());
    }
}
