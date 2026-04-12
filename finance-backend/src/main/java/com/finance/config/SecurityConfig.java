package com.finance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SecurityConfig - Configures Spring Security for the application.
 *
 * What this does:
 * 1. Uses HTTP Basic Authentication (simple username + password in request header)
 * 2. Protects all /api/** endpoints - you must login to access them
 * 3. Stores passwords securely using BCrypt encryption
 * 4. Enables @PreAuthorize annotations on controllers for role-based access
 *
 * Roles and their access:
 * - ADMIN   : Full access to all endpoints
 * - MANAGER : Can view/approve transactions, view summaries
 * - USER    : Can create and view own transactions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize in controllers
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // ---- Password Encoder: BCrypt is the industry standard ----
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ---- Authentication Provider: tells Spring HOW to validate credentials ----
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // Where to find the user
        provider.setPasswordEncoder(passwordEncoder());     // How to check the password
        return provider;
    }

    // ---- AuthenticationManager: used to programmatically authenticate ----
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ---- Main Security Filter Chain ----
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOrigin("*");   // allow all
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF (not needed for REST APIs)
            .csrf(csrf -> csrf.disable())

            // Configure which requests need authentication
            .authorizeHttpRequests(auth -> auth
                // Allow root URL without login (shows API info page)
                .requestMatchers("/").permitAll()

                // Allow access to H2 console (for testing) - remove in production
                .requestMatchers("/h2-console/**").permitAll()

                // All API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // Any other request also needs authentication
                .anyRequest().authenticated()
            )

            // Use HTTP Basic Auth (send username:password in Authorization header)
            // In Postman: Authorization tab → Basic Auth → enter username and password
            .httpBasic(httpBasic -> {})

            // Stateless sessions (REST APIs don't use server-side sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom authentication provider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
