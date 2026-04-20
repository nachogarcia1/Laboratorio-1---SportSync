package com.sportsync.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class Security {

    private final JwtFilter jwtFilter;

    public Security(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        /*todos*/
                        .requestMatchers("/usuarios/login", "/usuarios/register").permitAll()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/sedes/**", "/canchas/**", "/equipamiento", "/reservas/disponibilidad"
                        ).permitAll()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/criticas/**"
                        ).permitAll()

                        /*autenticados*/
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/usuarios/*/acreditar", "/usuarios/*/cancelar-socio"
                        ).authenticated()

                        /*ADMIN*/
                        .requestMatchers("/sedes/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/sedes/**", "/canchas/**", "/equipamiento/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/sedes/**", "/canchas/**", "/equipamiento/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/usuarios").hasRole("ADMIN")
                        .requestMatchers("/usuarios/buscar").hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/usuarios/*/suspender", "/usuarios/*/rehabilitar"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/usuarios/*"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/criticas/usuarios"
                        ).hasRole("ADMIN")

                        /*cualquier otra autenticados*/
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
