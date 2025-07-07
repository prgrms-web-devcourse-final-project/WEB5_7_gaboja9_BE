package io.gaboja9.mockstock.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
                                        .permitAll()
                                        .anyRequest()
                                        .permitAll())
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
