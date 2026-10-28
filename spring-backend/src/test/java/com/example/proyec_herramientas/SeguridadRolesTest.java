package com.example.proyec_herramientas;

import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
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

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cada endpoint exige el rol que corresponde: el panel es del ADMIN y el
// catalogo se puede leer sin sesion.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadRolesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @BeforeEach
    void prepararCuentas() {
        productoRepository.deleteAll();
        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearVendedor(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
    }

    @Test
    void crearProductoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearProductoConTokenDeClienteDevuelve403() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearProductoConTokenDeAdminDevuelve201() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre", is("Mouse Pro")));
    }

    @Test
    void listarProductosExigeRolAdmin() throws Exception {
        // El panel de productos trabaja sobre la tienda del token: es del ADMIN.
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        String tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void elVendedorEntraAlPosPeroNoAdministraElCatalogo() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_VENDEDOR,
                CuentasTest.CLAVE);

        // POS: catalogo, clientes e historial de ventas de su tienda.
        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ventas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // El catalogo se consulta, pero solo el ADMIN lo modifica.
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(51, "Teclado POS")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elVendedorPuedeRegistrarVentasPeroNoCambiarLaTienda() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_VENDEDOR,
                CuentasTest.CLAVE);

        // Cuerpo invalido (sin items): si el rol no tuviera acceso seria 403;
        // el 400 confirma que la peticion llega al controller.
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"Boleta\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/tiendas/" + CuentasTest.SLUG_TIENDA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCatalogoPublicoPorTiendaNoExigeSesion() throws Exception {
        // El comprador navega el catalogo por slug sin token.
        mockMvc.perform(get("/api/tiendas/" + CuentasTest.SLUG_TIENDA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tiendas/" + CuentasTest.SLUG_TIENDA + "/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void ventasMiasExigeSesion() throws Exception {
        mockMvc.perform(get("/api/ventas/mias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizarTiendaExigeRolAdmin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/tiendas/" + CuentasTest.SLUG_TIENDA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/tiendas/" + CuentasTest.SLUG_TIENDA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarClientesExigeRolAdmin() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        String tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void historialExigeRolAdmin() throws Exception {
        mockMvc.perform(get("/api/historial"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/historial").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        String tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/api/historial").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void tokenManipuladoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/historial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.inventado.abc"))
                .andExpect(status().isUnauthorized());
    }

    private String producto(int idProducto, String nombre) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "idProducto", idProducto,
                "nombre", nombre,
                "stock", 10,
                "precio", 25.0,
                "categoria", "Accesorios"));
    }
}
