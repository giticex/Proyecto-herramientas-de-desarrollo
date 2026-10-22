package com.example.proyec_herramientas.security;

import com.example.proyec_herramientas.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Lee el token de la cabecera Authorization y arma la sesion de Spring con el
// rol que viene firmado en el token. Si el token no existe o no es valido,
// la peticion sigue sin autenticar y ya decide SecurityConfig.
@Component
public class TokenFiltro extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";
    private static final String PREFIJO_ROL = "ROLE_";

    private final TokenService tokenService;

    public TokenFiltro(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        TokenService.Token token = tokenService.validar(leerToken(request));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var principal = new SesionPrincipal(token.usuario(), token.rol(), token.idCliente(), token.idTienda());
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority(PREFIJO_ROL + token.rol())));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }
        chain.doFilter(request, response);
    }

    private String leerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return cabecera.substring(PREFIJO.length()).trim();
        }
        return null;
    }
}
