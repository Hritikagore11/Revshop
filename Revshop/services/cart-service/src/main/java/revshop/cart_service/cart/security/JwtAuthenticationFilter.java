package revshop.cart_service.cart.security;

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
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("========== CART JWT FILTER ==========");
        System.out.println("REQUEST: " + request.getMethod()
                + " " + request.getRequestURI());
        System.out.println("AUTH HEADER PRESENT: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("NO BEARER TOKEN");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            if (!jwtService.isTokenValid(token)) {
                System.out.println("JWT INVALID");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            System.out.println("JWT VALID");

            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            System.out.println("USER ID: " + userId);
            System.out.println("EMAIL: " + email);
            System.out.println("ROLE FROM JWT: [" + role + "]");

            if (userId == null || email == null || role == null) {
                System.out.println("MISSING JWT CLAIM");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            role = role.trim().toUpperCase();

            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }

            String authority = "ROLE_" + role;

            System.out.println("FINAL AUTHORITY: [" + authority + "]");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority(authority)
                            )
                    );

            authentication.setDetails(userId);

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "SECURITY CONTEXT: "
                            + SecurityContextHolder.getContext()
                            .getAuthentication()
            );

            System.out.println(
                    "AUTHORITIES: "
                            + SecurityContextHolder.getContext()
                            .getAuthentication()
                            .getAuthorities()
            );

        } catch (Exception e) {
            System.out.println("========== JWT ERROR ==========");
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}