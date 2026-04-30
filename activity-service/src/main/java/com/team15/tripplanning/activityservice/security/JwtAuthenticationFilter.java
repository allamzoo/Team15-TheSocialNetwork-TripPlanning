package com.team15.tripplanning.activityservice.security;

import com.team15.tripplanning.activityservice.security.handler.SignatureValidationHandler;
import com.team15.tripplanning.activityservice.security.handler.UserLoaderHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthContext ctx = new AuthContext(request, response);
        ctx.setToken(header.substring(7));

        SignatureValidationHandler validation = new SignatureValidationHandler(jwtService);
        UserLoaderHandler userLoader = new UserLoaderHandler();
        validation.setNext(userLoader);

        boolean passed = validation.handle(ctx);

        if (!passed) {
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":" + response.getStatus() + ",\"message\":\"Unauthorized\"}");
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                ctx.getUserEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ctx.getRole()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
