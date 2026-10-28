package com.example.proyec_herramientas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El catalogo de roles vive en la base (tabla "rol") y se expone en /roles.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarRolesDevuelveAdminClienteYVendedor() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].idRol", is("ADMIN")))
                .andExpect(jsonPath("$[0].nombre", is("Administrador")))
                .andExpect(jsonPath("$[1].idRol", is("CLIENTE")))
                .andExpect(jsonPath("$[1].nombre", is("Cliente")))
                .andExpect(jsonPath("$[2].idRol", is("VENDEDOR")))
                .andExpect(jsonPath("$[2].nombre", is("Vendedor")));
    }

    @Test
    void elCatalogoDeRolesEsPublico() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk());
    }
}
