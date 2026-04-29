package com.team15.tripplanning.userservice.security;

import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.handler.SignatureValidationHandler;
import com.team15.tripplanning.userservice.security.handler.TokenExtractionHandler;
import com.team15.tripplanning.userservice.security.handler.UserLoaderHandler;
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
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        AuthContext ctx = new AuthContext(request, response);

        TokenExtractionHandler extraction = new TokenExtractionHandler();
        SignatureValidationHandler validation = new SignatureValidationHandler(jwtService);
        UserLoaderHandler userLoader = new UserLoaderHandler(userRepository);

        extraction.setNext(validation);
        validation.setNext(userLoader);

        boolean passed = extraction.handle(ctx);

        if (!passed) {
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
