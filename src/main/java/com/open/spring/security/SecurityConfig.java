package com.open.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/*
 * THIS FILE IS IMPORTANT
 * 
 * API Security Configuration
 * 
 * This file configures security for all API endpoints (/api/**) and the JWT authentication endpoint (/authenticate).
 * It uses JWT token-based authentication with stateless sessions.
 * 
 * Key Configuration:
 * - Order(1): This filter chain is processed FIRST before MvcSecurityConfig
 * - Security Matcher: Only handles requests to /api/** and /authenticate
 * - Authentication: Uses JWT tokens via JwtRequestFilter
 * - CSRF: Disabled (standard for stateless JWT APIs)
 * - CORS: Enabled with custom headers for cross-origin requests
 * - Rate Limiting: Applied via RateLimitFilter to prevent abuse
 * 
 * Endpoint Access Levels:
 * - permitAll(): Anyone can access (e.g., /authenticate, /api/person/create)
 * - authenticated(): Requires valid JWT token (e.g., /api/people/**, /api/assets/**)
 * - hasAuthority("ROLE_ADMIN"): Requires admin role (e.g., DELETE /api/person/**)
 * - hasAnyAuthority(...): Requires one of the specified roles (e.g., /api/synergy/**)
 * 
 * IMPORTANT: 
 * - Always set authentication endpoints to permitAll() so users can login without being logged in
 * - Always set account creation endpoints to permitAll() so users can create accounts
 * - For MVC endpoint security (form-based login), see MvcSecurityConfig.java
 * 
 * Filter Chain Order:
 * 1. RateLimitFilter - Rate limiting
 * 2. JwtRequestFilter - JWT token validation
 * 3. Standard Spring Security filters
 */

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;

    // Inject the RateLimitFilter
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtRequestFilter jwtRequestFilter,
                          RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
        this.rateLimitFilter = rateLimitFilter; 
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/api/**", "/authenticate")
                // JWT related configuration
                .csrf(csrf -> csrf.disable())
                // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) OBSOLETE, OVERWRITTEN BY BELOW
                .authorizeHttpRequests(auth -> auth

                        // ========== AUTHENTICATION & USER MANAGEMENT ==========
                        // Public endpoints - no authentication required, support user login and account creation
                        .requestMatchers(HttpMethod.POST, "/authenticate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/person/create").permitAll()
                        // Admin-only endpoints, beware of DELETE operations and impact to cascading relational data 
                        .requestMatchers(HttpMethod.DELETE, "/api/person/**").hasAuthority("ROLE_ADMIN")
                        // All other /api/person/** and /api/people/** operations handled by default rule
                        // ======================================================

                        // ========== PUBLIC API ENDPOINTS ==========
                        // Intentionally public - used for polling and public features
                        .requestMatchers("/api/jokes/**").permitAll()
                        // ==========================================
                        .requestMatchers("/api/exports/**").permitAll()
                        .requestMatchers("/api/imports/**").permitAll()
                        // ========== SYNERGY (ROLE-BASED ACCESS, Legacy system) ==========
                        // Specific endpoint with student/teacher/admin access
                        .requestMatchers(HttpMethod.POST, "/api/synergy/grades/requests").hasAnyAuthority("ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/synergy/saigai/").hasAnyAuthority("ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMIN")
                        // Teacher and admin access for other POST operations
                        .requestMatchers(HttpMethod.POST, "/api/synergy/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                        // =================================================

                        // ========== PUBLIC API ENDPOINTS (Legacy - TODO: Review for security) ==========
                        // These endpoints are currently wide open - consider if they should require authentication
                        .requestMatchers("/api/analytics/**").permitAll()
                        .requestMatchers("/api/plant/**").permitAll()
                        .requestMatchers("/api/groups/**").permitAll()
                        .requestMatchers("/api/academic-progress/**").permitAll()
                        .requestMatchers("/api/grades/**").permitAll()
                        .requestMatchers("/api/progress/**").permitAll()
                        .requestMatchers("/api/calendar/**").permitAll()
                        // ================================================================================

                        // ========== DEFAULT: ALL OTHER API ENDPOINTS ==========
                        // Secure by default - any endpoint not explicitly listed above requires authentication
                        .requestMatchers("/api/**").authenticated()
                        // ======================================================
                       
                )
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Credentials", "true"))
                        .addHeaderWriter(
                                new StaticHeadersWriter("Access-Control-Allow-ExposedHeaders", "*", "Authorization"))
                        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "Content-Type",
                                "Authorization", "x-csrf-token"))
                        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-MaxAge", "600"))
                        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "POST", "GET",
                                "PUT", "DELETE", "OPTIONS", "HEAD")))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))


                // Session related configuration
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtRequestFilter.class);

        return http.build();
    }
}
