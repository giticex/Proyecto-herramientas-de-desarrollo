package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void limpiar() {
        productoRepository.deleteAll();
    }

    @Test
    void listarProductosCatalogo() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));
        productoRepository.save(new Producto(2, "Teclado Mecánico", 5, new BigDecimal("120.00"), "Accesorios"));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Mouse Gamer")))
                .andExpect(jsonPath("$[1].precio", is(120.0)));
    }

    @Test
    void crearProducto() throws Exception {
        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(10, "Webcam HD", 3, 75.0, "Accesorios"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto", is(10)))
                .andExpect(jsonPath("$.nombre", is("Webcam HD")))
                .andExpect(jsonPath("$.stock", is(3)));
    }

    @Test
    void crearProductoSinIdAsignaSiguiente() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(0, "Audífonos BT", 12, 89.9, "Audio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto", is(2)));
    }

    @Test
    void actualizarProducto() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));

        mockMvc.perform(put("/productos/1")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProducto(99, "Inexistente", 1, 1.0, "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProducto() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));

        mockMvc.perform(delete("/productos/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void eliminarProductoNoExistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/productos/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearProductoConAtributosYListarlosDevuelveLosMismosAtributos() throws Exception {
        Map<String, String> atributos = new HashMap<>();
        atributos.put("talla", "M");
        atributos.put("color", "negro");

        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                crearProductoConAtributos(20, "Polo Deportivo", 15, 45.0, "Ropa", atributos))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atributos.talla", is("M")))
                .andExpect(jsonPath("$.atributos.color", is("negro")));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atributos.talla", is("M")))
                .andExpect(jsonPath("$[0].atributos.color", is("negro")));
    }

    @Test
    void actualizarProductoActualizaAtributos() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));

        Map<String, String> atributos = new HashMap<>();
        atributos.put("conexion", "inalámbrico");

        mockMvc.perform(put("/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                crearProductoConAtributos(1, "Mouse Pro", 20, 60.0, "Accesorios", atributos))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atributos.conexion", is("inalámbrico")));
    }

    @Test
    void productoSinAtributosDevuelveMapaVacio() throws Exception {
        productoRepository.save(new Producto(1, "Mouse Gamer", 8, new BigDecimal("50.00"), "Accesorios"));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atributos", is(anEmptyMap())));
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
}
