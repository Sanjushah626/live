package com.myshareapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PhpCompatiblePasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler roleRedirectSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            response.sendRedirect(isAdmin ? "/dashboard.php" : "/hello.html");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/dashboard.php", "/users.php", "/newss.php",
                    "/add_news.php", "/edit_news.php", "/delete_news.php",
                    "/contacts.php", "/view_contact.php", "/delete_contact.php",
                    "/api/admin/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/portfolio.php", "/portfolio", "/add_portfolio.php",
                    "/edit_portfolio.php", "/delete_portfolio.php",
                    "/export_portfolio.php", "/export_portfolio",
                    "/add_stock.php", "/edit_stock.php", "/delete_stock.php", "/sell_stock.php",
                    "/api/portfolios/**", "/api/stocks/**"
                ).authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login.php")
                .loginProcessingUrl("/login.php")
                .successHandler(roleRedirectSuccessHandler())
                .failureUrl("/login.php?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout.php")
                .logoutSuccessUrl("/login.php")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );
        return http.build();
    }
}
