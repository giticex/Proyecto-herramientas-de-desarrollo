package com.example.proyec_herramientas;

import com.example.proyec_herramientas.service.AuthServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SolicitudTiendaTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthServiceImpl auth;
    @Autowired JdbcTemplate jdbc;

    @Test
    void solicitaRespondeSeApruebaYPublicaConLaMismaCuenta() throws Exception {
        String correo = "dueno.solicitud@example.test";
        String clave = CuentasTest.CLAVE.repeat(3);
        auth.aprovisionarMaestro("maestro.solicitud@example.test", clave);
        JsonNode bienvenida = login("maestro.solicitud@example.test", clave);
        assertEquals("SUPER_ADMIN", bienvenida.path("rol").asText());
        String maestro = "Bearer " + bienvenida.path("token").asText();

        String id = crear(correo, "solicitud-demo", clave);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tienda WHERE slug = 'solicitud-demo'", Integer.class));
        assertEquals("5678", jdbc.queryForObject("SELECT dni_ultimos4 FROM solicitud_tienda WHERE id_solicitud = ?", String.class, id));
        assertTrue(jdbc.queryForObject("SELECT contrasena FROM usuario WHERE usuario = ?", String.class, correo).startsWith("$2"));

        JsonNode solicitante = login(correo, clave);
        assertEquals("SOLICITANTE", solicitante.path("rol").asText());
        String pendiente = "Bearer " + solicitante.path("token").asText();
        mvc.perform(get("/api/solicitudes/mia").header("Authorization", pendiente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(get("/api/solicitudes/maestro").header("Authorization", pendiente)).andExpect(status().isForbidden());
        mvc.perform(get("/productos").header("Authorization", pendiente)).andExpect(status().isForbidden());

        mvc.perform(put("/api/solicitudes/maestro/{id}", id).header("Authorization", maestro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"REQUIERE_DATOS\",\"mensaje\":\"Confirma tu rubro\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/solicitudes/mia/respuesta").header("Authorization", pendiente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensaje\":\"Moda y accesorios\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/solicitudes/maestro/{id}", id).header("Authorization", maestro)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"APROBADA\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"));
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tienda WHERE slug = 'solicitud-demo'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE usuario = ?", Integer.class, correo));
        mvc.perform(get("/api/solicitudes/mia").header("Authorization", pendiente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"));
        mvc.perform(get("/productos").header("Authorization", pendiente)).andExpect(status().isForbidden());

        JsonNode aprobada = login(correo, clave);
        assertEquals("ADMIN", aprobada.path("rol").asText());
        assertFalse(aprobada.path("tienda").path("publicada").asBoolean());
        String comerciante = "Bearer " + aprobada.path("token").asText();
        mvc.perform(get("/tiendas/solicitud-demo")).andExpect(status().isNotFound());
        mvc.perform(get("/tiendas/solicitud-demo/productos")).andExpect(status().isNotFound());
        mvc.perform(get("/productos").header("Authorization", comerciante)).andExpect(status().isOk());
        mvc.perform(put("/tiendas/solicitud-demo/publicacion").header("Authorization", comerciante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publicada").value(true));
        mvc.perform(get("/tiendas/solicitud-demo")).andExpect(status().isOk());
        mvc.perform(get("/api/solicitudes/maestro").header("Authorization", comerciante)).andExpect(status().isForbidden());
    }

    @Test
    void visitanteNoPuedeCrearUnaTiendaDirectamenteNiConsultarSolicitudes() throws Exception {
        mvc.perform(post("/auth/registro-tienda").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/solicitudes/mia")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/solicitudes/maestro")).andExpect(status().isUnauthorized());
    }

    private JsonNode login(String usuario, String clave) throws Exception {
        return mapper.readTree(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String crear(String correo, String slug, String clave) throws Exception {
        var datos = Map.ofEntries(Map.entry("nombres", "Ana"), Map.entry("apellidos", "Pruebas"),
                Map.entry("negocio", "Moda Demo"), Map.entry("rubro", "Moda"), Map.entry("slug", slug),
                Map.entry("correo", correo), Map.entry("dni", "12345678"), Map.entry("telefono", "900000001"),
                Map.entry("telefonoAlternativo", "900000002"), Map.entry("contrasena", clave),
                Map.entry("pagoSimulado", true));
        return mapper.readTree(mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datos)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andReturn().getResponse().getContentAsString()).path("id").asText();
    }
}
