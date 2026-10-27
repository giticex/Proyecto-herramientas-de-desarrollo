package com.example.proyec_herramientas.config;

import com.example.proyec_herramientas.security.TokenFiltro;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// Reglas de acceso por rol. El rol lo trae el token (ver TokenFiltro) y sale
// de la FK id_rol de la base, no de una constante escrita a mano.
//
//   ADMIN    -> panel de administracion de SU tienda (productos, clientes, historial).
//   VENDEDOR -> punto de venta (POS): consulta el catalogo, clientes y registra
//               ventas de SU tienda, sin poder administrar el catalogo.
//   CLIENTE  -> comprar, ver sus propias compras y el catalogo de su tienda.
//   publico  -> login, registro, catalogo de roles y catalogo publico por tienda.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenFiltro tokenFiltro;
    private final List<String> origenesPermitidos;

    public SecurityConfig(TokenFiltro tokenFiltro,
            @Value("${app.cors.origins}") String origenes) {
        this.tokenFiltro = tokenFiltro;
        this.origenesPermitidos = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isBlank())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**", "/roles", "/error").permitAll()
                        // Consulta RENIEC/SUNAT usada durante el registro (sin sesion).
                        .requestMatchers(HttpMethod.GET, "/api/sunat/**").permitAll()
                        // Catalogo publico por tienda: el comprador navega sin token.
                        .requestMatchers(HttpMethod.GET, "/tiendas/*", "/tiendas/*/productos").permitAll()
                        // El comprador ve sus propias compras; el administrador tambien.
                        .requestMatchers(HttpMethod.GET, "/ventas/mias").hasAnyRole("CLIENTE", "ADMIN")
                        // El historial completo (paginado) lo ven el panel admin y el POS.
                        .requestMatchers(HttpMethod.GET, "/ventas").hasAnyRole("ADMIN", "VENDEDOR")
                        // El checkout puede hacerlo el comprador; el POS y el admin tambien.
                        .requestMatchers(HttpMethod.POST, "/ventas").hasAnyRole("CLIENTE", "ADMIN", "VENDEDOR")
                        // El panel de administracion trabaja sobre SU tienda: el
                        // idTienda sale del token, nunca del body.
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                        // Comprobantes PDF/XML: accesibles al POS y panel
                        .requestMatchers("/comprobantes/**", "/api/comprobantes/**")
                        .hasAnyAuthority("ADMIN", "VENDEDOR")
                        // El POS consulta el catalogo y los clientes, pero no los gestiona.
                        .requestMatchers(HttpMethod.GET, "/productos", "/productos/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/clientes", "/clientes/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers("/productos/**", "/clientes/**", "/ventas/**",
                                "/historial", "/historial/**", "/boletas", "/boletas/**",
                                "/boleta/**", "/factura/**", "/tiendas/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint((peticion, respuesta, error) ->
                                escribirError(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Sesion no iniciada"))
                        .accessDeniedHandler((peticion, respuesta, error) ->
                                escribirError(respuesta, HttpServletResponse.SC_FORBIDDEN,
                                        "No tienes permisos para esta operacion")))
                .addFilterBefore(tokenFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void escribirError(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("{\"status\":" + estado + ",\"error\":\"" + mensaje + "\"}");
    }
}
