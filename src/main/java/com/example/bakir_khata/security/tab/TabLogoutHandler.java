package com.example.bakir_khata.security.tab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TabLogoutHandler implements LogoutHandler {
    private final TabAuthenticationStore store;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getSession(false) != null) {
            store.remove(request.getParameter(TabAuthenticationFilter.PARAM), request.getSession(false).getId());
        }
    }
}
