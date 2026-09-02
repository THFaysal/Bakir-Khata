package com.example.bakir_khata.config;

import com.example.bakir_khata.security.MobileOrEmailAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.example.bakir_khata.security.tab.TabAuthenticationFilter;
import com.example.bakir_khata.security.tab.TabAuthenticationSuccessHandler;
import com.example.bakir_khata.security.tab.TabLogoutHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TabAuthenticationFilter tabAuthenticationFilter;
    private final TabAuthenticationSuccessHandler tabAuthenticationSuccessHandler;
    private final TabLogoutHandler tabLogoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MobileOrEmailAuthenticationProvider provider) throws Exception {

        http.authenticationProvider(provider);
        http.securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()));
        // Authentication is persisted in the tab-auth store, not in HttpSession.
        // Keeping the browser session id stable prevents a login in Tab B from
        // invalidating Tab A's tab token while the fresh tab token still prevents fixation abuse.
        http.sessionManagement(session -> session.sessionFixation(fixation -> fixation.none()));
        http.addFilterBefore(tabAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/uploads/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        // Separate branded entry point for admins - same processing URL as /login below
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/coordinator/apply", "/coordinator/apply/**").hasRole("USER")
                        // Reviewing and approving applications, and seeing who the coordinators
                        // are, is admin-only - coordinators do not see their own peer list
                        .requestMatchers("/coordinator/applications/**", "/coordinator/list").hasRole("ADMIN")
                        // Admin panel: system administration and financial oversight
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/transactions/review", "/transactions/*/flag", "/management/**", "/coordinator/dashboard").hasAnyRole("ADMIN", "COORDINATOR")
                        // A person can still need to confirm an older cash lending obligation
                        // after their account is promoted to coordinator. These two actions
                        // remain ownership-checked in TransactionService.
                        .requestMatchers("/transactions/*/accept", "/transactions/*/reject").authenticated()
                        .requestMatchers("/lenders/**", "/loans/**", "/payments/**", "/reminders/**").hasRole("USER")
                        .requestMatchers("/transactions/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("identifier")
                        .passwordParameter("credential")
                        .successHandler(tabAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(tabLogoutHandler)
                        .invalidateHttpSession(false)
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
