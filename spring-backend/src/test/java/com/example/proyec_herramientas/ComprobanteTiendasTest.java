package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ComprobanteTiendasTest {
    private static final String CONFIGURACION = "/api/facturacion/emisor";
    private static final String RUC_A = "20000000001";
    private static final String RUC_B = "20000000002";
    private static final String TIENDA_B = "negocio-fiscal-b";

    @Autowired
    private com.example.proyec_herramientas.service.TokenService tokens;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private EntityManager entityManager;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PersonaRepository personas;
    @Autowired private TiendaRepository tiendas;
    @Autowired private ClienteRepository clientes;
    @Autowired private ProductoRepository productos;

    private String adminA;
    private String adminB;
    private String comprador;

    @BeforeEach
    void preparar() throws Exception {
        CuentasTest.crearAdmin(usuarios, personas, tiendas);
        CuentasTest.crearCliente(usuarios, personas, tiendas);
        CuentasTest.crearFichaCliente(clientes);
        adminA = CuentasTest.token(mvc, json, CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE);
        comprador = CuentasTest.token(mvc, json, CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE);
        String registro = mvc.perform(post("/auth/registro-tienda")
                        .header("Authorization", "Bearer " + tokens.generar("maestro-pruebas", "SUPER_ADMIN", "", null)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", "Tienda fiscal B", "rubro", "Textil",
                                "slug", TIENDA_B, "correo", "fiscal.b@example.com",
                                "contrasena", CuentasTest.CLAVE.repeat(2)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        adminB = json.readTree(registro).get("token").asText();
        guardarProducto(101, CuentasTest.SLUG_TIENDA);
        guardarProducto(102, TIENDA_B);
    }

    @Test
    void dosEmisoresUsanSusPropiasSeriesYElXmlNoCambiaAlEditarDatos() throws Exception {
        configurar(adminA, RUC_A, "Comercio A").andExpect(status().isOk());
        configurar(adminB, RUC_B, "Comercio B").andExpect(status().isOk());
        UUID clave = UUID.randomUUID();

        JsonNode boletaA = emitirBoleta(CuentasTest.SLUG_TIENDA, 101, 1, clave, "B001");
        JsonNode boletaB = emitirBoleta(TIENDA_B, 102, 1, clave, "B001");
        assertNotEquals(boletaA.get("idVenta"), boletaB.get("idVenta"));
        assertEquals(RUC_A, boletaA.at("/emisor/ruc").asText());
        assertEquals(RUC_B, boletaB.at("/emisor/ruc").asText());

        String idVenta = boletaA.get("idVenta").asText();
        String xmlOriginal = mvc.perform(get("/comprobantes/{id}/xml", idVenta).header(HttpHeaders.AUTHORIZATION, bearer(comprador)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Comercio A")))
                .andExpect(content().string(containsString("schemeID=\"1\">12345678</cbc:ID>")))
                .andReturn().getResponse().getContentAsString();
        configurar(adminA, RUC_A, "Comercio A renovado").andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/comprobantes/{id}/xml", idVenta).header(HttpHeaders.AUTHORIZATION, bearer(comprador)))
                .andExpect(status().isOk()).andExpect(content().string(xmlOriginal));
        mvc.perform(get("/comprobantes/{id}", idVenta).header(HttpHeaders.AUTHORIZATION, bearer(adminA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emisor.razonSocial", is("Comercio A")));
        mvc.perform(get("/comprobantes/{id}/pdf", idVenta).header(HttpHeaders.AUTHORIZATION, bearer(adminB)))
                .andExpect(status().isNotFound());

        emitir(TIENDA_B, "01", "6", "20000000003", 102, 1, UUID.randomUUID(), null)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.serie", is("F001")))
                .andExpect(jsonPath("$.correlativo", is(1))).andExpect(jsonPath("$.estadoSunat", is("GENERADO")));
    }

    @Test
    void reintentoConLaMismaClaveConservaVentaCorrelativoYStock() throws Exception {
        configurar(adminA, RUC_A, "Comercio A").andExpect(status().isOk());
        UUID clave = UUID.randomUUID();
        JsonNode inicial = emitirBoleta(CuentasTest.SLUG_TIENDA, 101, 2, clave, null);
        JsonNode reintento = emitirBoleta(CuentasTest.SLUG_TIENDA, 101, 2, clave, null);
        assertEquals(inicial.get("idVenta"), reintento.get("idVenta"));
        assertEquals(inicial.get("numeroDocumento"), reintento.get("numeroDocumento"));

        entityManager.flush();
        entityManager.clear();
        assertEquals(8, productos.findById(101).orElseThrow().getStock());
        emitir(CuentasTest.SLUG_TIENDA, "03", "1", "12345678", 101, 1, clave, null)
                .andExpect(status().isConflict());
    }

    @Test
    void laConfiguracionEsPrivadaYUnaTiendaSinEmisorNoPuedeEmitir() throws Exception {
        configurar(adminA, RUC_A, "Comercio A").andExpect(status().isOk());
        mvc.perform(get(CONFIGURACION).header(HttpHeaders.AUTHORIZATION, bearer(adminB)))
                .andExpect(status().isNoContent());
        mvc.perform(get(CONFIGURACION).header(HttpHeaders.AUTHORIZATION, bearer(comprador)))
                .andExpect(status().isForbidden());
        emitir(TIENDA_B, "03", "1", "12345678", 102, 1, UUID.randomUUID(), null)
                .andExpect(status().isConflict());
    }

    private JsonNode emitirBoleta(String tienda, int producto, int cantidad, UUID clave, String serie) throws Exception {
        String cuerpo = emitir(tienda, "03", "1", "12345678", producto, cantidad, clave, serie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento", is("B001-000001")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    private ResultActions emitir(String tienda, String tipo, String documento, String numero,
            int producto, int cantidad, UUID clave, String serie) throws Exception {
        var datos = new java.util.LinkedHashMap<String, Object>();
        datos.put("tipoComprobante", tipo);
        datos.put("receptor", Map.of("tipoDocumento", documento, "numeroDocumento", numero,
                "razonSocial", "Comprador de prueba"));
        datos.put("items", List.of(Map.of("idProducto", producto, "cantidad", cantidad)));
        if (serie != null) datos.put("serie", serie);
        return mvc.perform(post("/comprobantes").param("tienda", tienda)
                .header(HttpHeaders.AUTHORIZATION, bearer(comprador)).header("Idempotency-Key", clave.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)));
    }

    private ResultActions configurar(String token, String ruc, String nombre) throws Exception {
        return mvc.perform(put(CONFIGURACION).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("emisor", Map.of("ruc", ruc, "razonSocial", nombre, "nombreComercial", nombre,
                                "domicilioFiscal", "Dirección de prueba", "ubigeo", "150101",
                                "distrito", "Lima", "provincia", "Lima", "departamento", "Lima", "pais", "PE"),
                                "serieBoleta", "B001", "serieFactura", "F001"))));
    }

    private void guardarProducto(int id, String slug) {
        Producto producto = new Producto(id, "Producto de prueba", 10, new BigDecimal("25.00"), "General");
        producto.setIdTienda(tiendas.findBySlug(slug).orElseThrow().getIdTienda());
        productos.save(producto);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
