package com.example.bakir_khata.security.tab;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TabAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final TabAuthenticationStore store;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String token = store.create(request.getSession(true).getId(), authentication);
        String target = UriComponentsBuilder.fromPath("/dashboard")
                .queryParam(TabAuthenticationFilter.PARAM, token)
                .queryParam("tabLogin", "true")
                .build().toUriString();
        response.sendRedirect(target);
    }
}
