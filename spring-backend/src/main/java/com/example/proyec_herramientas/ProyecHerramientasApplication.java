package com.example.proyec_herramientas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// La sesion se valida con el token firmado, no con un usuario en memoria:
// se excluye la cuenta "user" con clave aleatoria que Boot crearia solo.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ProyecHerramientasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyecHerramientasApplication.class, args);
	}

}
