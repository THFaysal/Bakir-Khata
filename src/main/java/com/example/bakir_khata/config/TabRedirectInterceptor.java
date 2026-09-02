package com.example.bakir_khata.config;

import com.example.bakir_khata.security.tab.TabAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TabRedirectInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView == null || modelAndView.getViewName() == null) return;
        String view = modelAndView.getViewName();
        String token = request.getParameter(TabAuthenticationFilter.PARAM);
        if (token == null || token.isBlank() || !view.startsWith("redirect:") || view.contains("__tab=")) return;
        String separator = view.contains("?") ? "&" : "?";
        modelAndView.setViewName(view + separator + TabAuthenticationFilter.PARAM + "=" + token);
    }
}
