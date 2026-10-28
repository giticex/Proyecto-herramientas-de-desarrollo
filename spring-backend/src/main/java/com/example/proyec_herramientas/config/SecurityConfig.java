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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenFiltro tokenFiltro;
    private final List<String> origenesPermitidos;

    public SecurityConfig(TokenFiltro tokenFiltro,
            @Value("${app.cors.origins:}") String origenes) {
        this.tokenFiltro = tokenFiltro;
        this.origenesPermitidos = origenes == null || origenes.isBlank()
                ? List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:8080")
                : Arrays.stream(origenes.split(","))
                        .map(String::trim)
                        .filter(origen -> !origen.isBlank())
                        .toList();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Todo endpoint publico vive bajo /api; las rutas sin
                        // prefijo se eliminaron para que no evadan estas reglas.
                        .requestMatchers("/api/auth/**", "/api/roles", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sunat/**").permitAll()
                        // Catalogo publico por slug (y portada de la tienda demo).
                        .requestMatchers(HttpMethod.GET, "/api/tiendas/mi-tienda",
                                "/api/tiendas/*", "/api/tiendas/*/productos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ventas/mias").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ventas").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/ventas")
                        .hasAnyRole("CLIENTE", "ADMIN", "VENDEDOR")
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/comprobantes/**")
                        .hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                        // Las devoluciones y los créditos los gestiona el personal de la tienda.
                        .requestMatchers("/api/devoluciones", "/api/devoluciones/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers("/api/creditos", "/api/creditos/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        // El POS consulta el catalogo; solo el ADMIN lo modifica.
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/productos", "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tiendas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clientes", "/api/clientes/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers("/api/historial", "/api/historial/**",
                                "/api/boletas", "/api/boletas/**", "/api/boleta/**", "/api/factura/**",
                                "/api/tiendas/**").hasAnyRole("ADMIN", "VENDEDOR")
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