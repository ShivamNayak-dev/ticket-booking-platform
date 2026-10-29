
package com.ticketbooking.config;

import com.ticketbooking.repository.UserRepository;
import com.ticketbooking.security.JwtAuthenticationFilter;
import com.ticketbooking.security.JwtService;
import com.ticketbooking.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final String cookieName;

    public SecurityConfig(
            JwtService jwtService,
            UserRepository userRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            @Value("${app.cookie.name}") String cookieName) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.cookieName = cookieName;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository,
                        cookieName
                );

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/error").permitAll()

                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout"
                        ).permitAll()

                        // Anyone can browse events
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events",
                                "/api/events/**"
                        ).permitAll()

                        // Only ORGANIZER and ADMIN can create events
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/events",
                                "/api/events/**"
                        ).hasAnyRole("ORGANIZER", "ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
