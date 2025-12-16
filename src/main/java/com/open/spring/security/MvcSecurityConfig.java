package com.open.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/*
 * MvcSecurityConfig.java
 * 
 * MVC Security Configuration - handles web pages and form-based login
 * 
 * Key Points:
 * - Order(2): Processed AFTER the API security chain (Order 1)
 * - Matches: All requests not handled by API chain (/**)
 * - Authentication: Traditional form login with sessions
 * - Login: /login page, redirects to /mvc/person/read on success
 * - Logout: Deletes session cookie, redirects to homepage
 * 
 * Access Levels:
 * - permitAll(): /login, /mvc/person/create, /mvc/person/reset
 * - authenticated(): Most /mvc/** endpoints
 * - ROLE_ADMIN: /mvc/person/delete, /mvc/extract, /mvc/import
 * - ROLE_TEACHER/STUDENT: /mvc/synergy/** endpoints
 * 
 * For API security (JWT-based), see SecurityConfig.java
 */

@Configuration
public class MvcSecurityConfig {

    /**
     * MVC security: form login, session-based.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            // Everything that is NOT handled by the API chain
            .securityMatcher("/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/mvc/person/search/**").authenticated()
                .requestMatchers("/mvc/person/create/**").permitAll()
                .requestMatchers("/mvc/person/reset/**").permitAll()
                .requestMatchers("/mvc/person/read/**").authenticated()
                .requestMatchers("/mvc/person/cookie-clicker").authenticated()
                .requestMatchers(HttpMethod.GET,"/mvc/person/update/user").authenticated()
                .requestMatchers(HttpMethod.GET,"/mvc/person/update/**").authenticated()
                .requestMatchers(HttpMethod.POST,"/mvc/person/update/").authenticated()
                .requestMatchers(HttpMethod.POST,"/mvc/person/update/role").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST,"/mvc/person/update/roles").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/mvc/person/delete/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/mvc/bathroom/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/authenticateForm").permitAll()
                .requestMatchers("/mvc/synergy/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/mvc/synergy/gradebook").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_STUDENT")
                .requestMatchers(HttpMethod.GET, "/mvc/synergy/view-grade-requests").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/mvc/assignments/tracker").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/mvc/teamteach/teachergrading").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET,"/mvc/train/**").authenticated()
                .requestMatchers(HttpMethod.GET,"/mvc/extract/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST,"/mvc/extract/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST,"/mvc/import/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/mvc/grades/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/mvc/assignments/read").hasAnyAuthority("ROLE_ADMIN", "ROLE_TEACHER")
                .requestMatchers("/mvc/bank/read").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/mvc/progress/read").hasAnyAuthority("ROLE_ADMIN", "ROLE_TEACHER")

                // Fallback ---------------------------------------------------
                .requestMatchers("/**").permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/mvc/person/read"))
            .logout(logout -> logout
                .deleteCookies("sess_java_spring")
                .logoutSuccessUrl("/"));

        return http.build();
    }
}
