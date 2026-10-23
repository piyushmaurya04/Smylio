package Tenant.Owner.Management.TOM.Security;

import java.util.List;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/assets/**", "/favicon.ico",
                                "/login", "/register", "/profile", "/help",
                                "/owner/**", "/tenant/**", "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").denyAll()
                        .requestMatchers("/api/profile/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/api/properties/**").hasRole("OWNER")
                        .requestMatchers("/api/leases/**").hasRole("OWNER")
                        .requestMatchers("/api/tenants/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/me/**").hasRole("TENANT")
                        .requestMatchers(HttpMethod.POST, "/api/rents/generate").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/rents/approvals").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/rents/*/approve", "/api/rents/*/reject")
                        .hasRole("OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/rents/*/pay/**").hasRole("TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/rents/dues").hasRole("TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/rents/lease/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.POST, "/api/maintenance").hasRole("TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/maintenance/property/**")
                        .hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/maintenance/page", "/api/maintenance/*")
                        .hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.PUT, "/api/maintenance/*").hasRole("TENANT")
                        .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/cancel").hasRole("TENANT")
                        .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/status").hasRole("OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/maintenance/*/comments")
                        .hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/maintenance/*/comments")
                        .hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.POST, "/api/notices").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/page")
                        .hasAnyRole("OWNER", "TENANT")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/owner").hasRole("OWNER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) -> response
                                .sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}