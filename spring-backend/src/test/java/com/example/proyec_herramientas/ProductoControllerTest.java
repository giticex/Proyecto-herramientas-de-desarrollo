package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private int idTienda;
    private String tokenAdmin;

    @BeforeEach
    void limpiar() throws Exception {
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        tiendaRepository.deleteAll();
        personaRepository.deleteAll();
        clienteRepository.deleteAll();

        RegistroTienda registro = registrarTienda("Tienda Uno", "tienda-uno", "uno@correo.com");
        idTienda = registro.idTienda();
        tokenAdmin = registro.token();
    }

    @Test
    void listarProductosCatalogo() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));
        productoRepository.save(new Producto(2, "Teclado Mecánico", 5, 120.0, "Accesorios", idTienda));

        mockMvc.perform(get("/productos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Mouse Gamer")))
                .andExpect(jsonPath("$[1].precio", is(120.0)));
    }

    @Test
    void listarProductosSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearProducto() throws Exception {
        mockMvc.perform(post("/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(10, "Webcam HD", 3, 75.0, "Accesorios"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto", is(10)))
                .andExpect(jsonPath("$.nombre", is("Webcam HD")))
                .andExpect(jsonPath("$.stock", is(3)));
    }

    @Test
    void crearProductoComoClienteDevuelve403() throws Exception {
        String tokenCliente = registrarCliente("Juan", "Pérez", "juan@correo.com");

        mockMvc.perform(post("/productos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(10, "Webcam HD", 3, 75.0, "Accesorios"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearProductoSinIdAsignaSiguiente() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));

        mockMvc.perform(post("/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(0, "Audífonos BT", 12, 89.9, "Audio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto", is(2)));
    }

    @Test
    void actualizarProducto() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));

        mockMvc.perform(put("/productos/1")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(1, "Mouse Pro", 20, 60.0, "Accesorios"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Mouse Pro")))
                .andExpect(jsonPath("$.stock", is(20)))
                .andExpect(jsonPath("$.precio", is(60.0)));
    }

    @Test
    void actualizarProductoNoExistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/productos/99")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(99, "Inexistente", 1, 1.0, "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProducto() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));

        mockMvc.perform(delete("/productos/1").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/productos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void eliminarProductoNoExistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/productos/1").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeTiendaANoPuedeEditarNiBorrarProductoDeTiendaB() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));
        RegistroTienda otraTienda = registrarTienda("Tienda Dos", "tienda-dos", "dos@correo.com");

        mockMvc.perform(put("/productos/1")
                        .header("Authorization", "Bearer " + otraTienda.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(1, "Hackeado", 1, 1.0, "X"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/productos/1").header("Authorization", "Bearer " + otraTienda.token()))
                .andExpect(status().isNotFound());

        // El producto sigue intacto para la tienda dueña.
        mockMvc.perform(get("/productos/1").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Mouse Gamer")));
    }

    @Test
    void crearProductoConAtributosYListarlosDevuelveLosMismosAtributos() throws Exception {
        Map<String, String> atributos = new HashMap<>();
        atributos.put("talla", "M");
        atributos.put("color", "negro");

        mockMvc.perform(post("/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                crearProductoConAtributos(20, "Polo Deportivo", 15, 45.0, "Ropa", atributos))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atributos.talla", is("M")))
                .andExpect(jsonPath("$.atributos.color", is("negro")));

        mockMvc.perform(get("/productos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atributos.talla", is("M")))
                .andExpect(jsonPath("$[0].atributos.color", is("negro")));
    }

    @Test
    void actualizarProductoActualizaAtributos() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));

        Map<String, String> atributos = new HashMap<>();
        atributos.put("conexion", "inalámbrico");

        mockMvc.perform(put("/productos/1")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                crearProductoConAtributos(1, "Mouse Pro", 20, 60.0, "Accesorios", atributos))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atributos.conexion", is("inalámbrico")));
    }

    @Test
    void productoSinAtributosDevuelveMapaVacio() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, 50.0, "Accesorios", idTienda));

        mockMvc.perform(get("/productos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atributos", is(anEmptyMap())));
    }

    private RegistroTienda registrarTienda(String nombre, String slug, String correo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", nombre, "rubro", "moda", "slug", slug,
                                "correo", correo, "contrasena", "clave1234"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        String token = json.get("token").asText();
        int id = tiendaRepository.findBySlug(slug).orElseThrow().getIdTienda();
        return new RegistroTienda(token, id);
    }

    private String registrarCliente(String nombre, String apellido, String correo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", nombre, "apellido", apellido, "correo", correo,
                                "contrasena", "clave1234", "telefono", "987654321"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private Map<String, Object> crearProducto(int id, String nombre, int stock, double precio, String categoria) {
        return Map.of(
                "idProducto", id,
                "nombre", nombre,
                "stock", stock,
                "precio", precio,
                "categoria", categoria);
    }

    private Map<String, Object> crearProductoConAtributos(int id, String nombre, int stock, double precio,
            String categoria, Map<String, String> atributos) {
        Map<String, Object> producto = new HashMap<>(crearProducto(id, nombre, stock, precio, categoria));
        producto.put("atributos", atributos);
        return producto;
    }

    private record RegistroTienda(String token, int idTienda) {
    }
}
