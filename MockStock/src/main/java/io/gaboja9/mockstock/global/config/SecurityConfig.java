package io.gaboja9.mockstock.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import io.gaboja9.mockstock.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                    "/", "/swagger-ui/**","/v3/api-docs/**", "/actuator/**", "/login/**", "/oauth2/**", "/error").permitAll()
                                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 ->
                oauth2
                    // .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    // .failureUrl("/login?error=true")
                    .userInfoEndpoint(userInfo ->
                        userInfo.userService(authService)
                    )
            )
            .logout(logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            )
            .formLogin(form -> form.disable())
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
