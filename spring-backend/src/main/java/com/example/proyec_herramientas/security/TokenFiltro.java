package com.example.proyec_herramientas.security;

import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
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
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    public TokenFiltro(TokenService tokenService, TiendaRepository tiendaRepository,
            UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        TokenService.Token token = tokenService.validar(leerToken(request));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // La suspension se verifica en cada peticion: el token vigente de
            // una tienda suspendida deja de servir de inmediato, sin esperar
            // a que expire.
            if (accesoBloqueado(token.idTienda())) {
                escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                        "El acceso de esta tienda está suspendido");
                return;
            }
            // Mismo criterio para el bloqueo individual de una cuenta de staff.
            if (cuentaBloqueada(token)) {
                escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                        "La cuenta está suspendida");
                return;
            }
            var principal = new SesionPrincipal(token.usuario(), token.rol(), token.idCliente(), token.idTienda());
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority(PREFIJO_ROL + token.rol())));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }
        chain.doFilter(request, response);
    }

    // Solo las cuentas de staff viven en la tabla usuario; el token de un
    // comprador usa su correo como identificador y no debe cruzarse con una
    // cuenta de panel que la use como nombre de usuario.
    private boolean cuentaBloqueada(TokenService.Token token) {
        boolean staff = Rol.ADMIN.equalsIgnoreCase(token.rol())
                || Rol.VENDEDOR.equalsIgnoreCase(token.rol());
        if (!staff) {
            return false;
        }
        return usuarioRepository.findByUsuario(token.usuario())
                .map(usuario -> usuario.estaSuspendido() || !token.rol().equalsIgnoreCase(usuario.getIdRol()))
                .orElse(true);
    }

    private boolean accesoBloqueado(Integer idTienda) {
        if (idTienda == null) {
            return false;
        }
        return tiendaRepository.findById(idTienda)
                .map(Tienda::estaSuspendida)
                .orElse(true);
    }

    private String leerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return cabecera.substring(PREFIJO.length()).trim();
        }
        return null;
    }

    private void escribirError(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("{\"status\":" + estado + ",\"error\":\"" + mensaje + "\"}");
    }
}
