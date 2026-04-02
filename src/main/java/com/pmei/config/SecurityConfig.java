package com.pmei.config;

import com.pmei.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration class responsible for defining authentication and authorization rules.
 *
 * This class configures:
 * - Stateless session management (JWT-based authentication)
 * - Endpoint access control (public and protected routes)
 * - Integration of the JWT authentication filter
 * - Password encoding strategy
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the main security filter chain.
     *
     * Key configurations:
     * - Disables CSRF (not needed for stateless APIs)
     * - Sets session management to STATELESS (JWT-based authentication)
     * - Defines public and protected endpoints
     * - Adds JWT filter before Spring Security authentication filter
     *
     * @param http HttpSecurity configuration object
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF since we are using JWT (stateless API)
                .csrf(csrf -> csrf.disable())

                // Configure session as stateless (no session will be created or used)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define authorization rules for HTTP requests
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Protected endpoint: requires USER or ADMIN role
                        .requestMatchers("/finance/**").hasAnyRole("USER", "ADMIN")

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )

                // Add JWT filter before the default authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines the password encoder used for hashing user passwords.
     *
     * BCrypt is a strong hashing algorithm recommended for password storage.
     *
     * @return PasswordEncoder implementation
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager bean used for authentication processes.
     *
     * This is required to perform manual authentication (e.g., in AuthService).
     *
     * @param config AuthenticationConfiguration provided by Spring
     * @return AuthenticationManager instance
     * @throws Exception if retrieval fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}