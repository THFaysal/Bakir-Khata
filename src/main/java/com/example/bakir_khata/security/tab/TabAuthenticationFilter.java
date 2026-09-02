package com.example.bakir_khata.security.tab;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TabAuthenticationFilter extends OncePerRequestFilter {
    public static final String PARAM = "__tab";
    private final TabAuthenticationStore store;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getParameter(PARAM);
        HttpSession session = request.getSession(false);

        if (token != null && !token.isBlank()) {

            Optional<Authentication> resolved =
                    session == null
                            ? Optional.empty()
                            : store.resolve(token, session.getId());

            if (resolved.isPresent()) {

                SecurityContext context =
                        SecurityContextHolder.createEmptyContext();

                context.setAuthentication(resolved.get());
                SecurityContextHolder.setContext(context);

            } else {

                String path = request.getRequestURI();

                boolean publicRequest =
                        path.equals("/login")
                                || path.equals("/admin/login")
                                || path.equals("/register")
                                || path.equals("/error")
                                || path.startsWith("/css/")
                                || path.startsWith("/js/")
                                || path.startsWith("/uploads/")
                                || path.equals("/favicon.ico");

                if (!publicRequest) {

                    SecurityContextHolder.clearContext();

                    if (path.equals("/logout")) {
                        response.sendRedirect("/login?logout=true");
                    } else {
                        response.sendRedirect("/login?expired=true");
                    }

                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
