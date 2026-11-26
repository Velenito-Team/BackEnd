package co.edu.udistrital.backend.config;

import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            User user = users.findByUsuarioNormalizado(username.trim().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
            return org.springframework.security.core.userdetails.User.withUsername(user.getUsuarioNormalizado())
                    .password(user.getContrasenaHash())
                    .authorities(user.getRol().name())
                    .disabled(!user.isActivo())
                    .build();
        };
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Inicia sesión para acceder.\"}");
                        })
                        .accessDeniedHandler((request, response, error) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Tu rol no tiene permiso para acceder a este módulo.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invoices/receipt/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cash-closings/open")
                        .hasAuthority("administrador")
                        .requestMatchers(HttpMethod.POST, "/api/providers/**")
                        .hasAuthority("administrador")
                        .requestMatchers(HttpMethod.GET, "/api/providers/**")
                        .hasAnyAuthority("administrador", "tendero", "cajero")
                        .requestMatchers("/api/reports/**", "/api/sales/**", "/api/balance/**",
                                "/api/cash-closings/**", "/api/expenses/**", "/api/fixed-expenses/**",
                                "/api/inventory/receipts/**")
                        .hasAnyAuthority("administrador", "tendero")
                        .requestMatchers("/api/balance/monthly/**", "/api/fixed-expenses/summary/**")
                        .hasAnyAuthority("administrador", "tendero")
                        .requestMatchers("/api/users/**")
                        .hasAuthority("administrador")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }
}
