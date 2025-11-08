package com.uniflow.academic.config;

import com.uniflow.academic.shared.infrastructure.security.GoogleTokenAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Spring Security 6.5.6 REST API configuration
 * Valida tokens de Google como Azure APIM
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GoogleTokenAuthenticationFilter googleTokenFilter;

    public SecurityConfig(GoogleTokenAuthenticationFilter googleTokenFilter) {
        this.googleTokenFilter = googleTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                // Autorización
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/",
                                "/health",
                                "/students/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/students/{studentId}"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Agregar filtro de validación de token de Google
                // Ejecuta ANTES que UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        googleTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                // CORS
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                // CSRF - disabled for REST API
                .csrf(AbstractHttpConfigurer::disable
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:8080"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}