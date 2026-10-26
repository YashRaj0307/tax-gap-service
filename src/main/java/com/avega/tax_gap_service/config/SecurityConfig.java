package com.avega.tax_gap_service.config;

import com.avega.tax_gap_service.domain.AppUser;
import com.avega.tax_gap_service.repository.AppUserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(ObjectProvider<AppUserRepository> repositoryProvider) {
        return username -> {
            AppUserRepository users = repositoryProvider.getIfAvailable();
            // MVC slice tests do not load JPA repositories; the full application always uses DB users.
            if (users == null && "devuser".equals(username)) {
                return User.withUsername("devuser")
                        .password("{noop}LocalDevOnly-ChangeMe-2026")
                        .roles("USER").build();
            }
            if (users == null) throw new UsernameNotFoundException("User not found");
            AppUser appUser = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return User.withUsername(appUser.getUsername())
                    .password(appUser.getPasswordHash())
                    .roles(appUser.getRole())
                    .disabled(!appUser.isEnabled())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());
        return http.build();
    }
}
