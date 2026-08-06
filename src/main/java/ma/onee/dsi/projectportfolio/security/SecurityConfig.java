package ma.onee.dsi.projectportfolio.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/dashboard").authenticated()
                        .requestMatchers("/roles/**").hasRole("ADMIN")
                        .requestMatchers("/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/projets/*/risques").authenticated()
                        .requestMatchers(HttpMethod.POST, "/projets/*/risques")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.GET, "/risques/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/risques/**")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.DELETE, "/risques/**")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.GET, "/projets/*/couts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/projets/*/couts")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.GET, "/couts/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/couts/**")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.DELETE, "/couts/**")
                        .hasRole("RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.GET, "/projets/*/affectations-ressources")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/projets/*/affectations-ressources/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/projets/*/affectations-ressources")
                        .hasAnyRole("ADMIN", "RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.POST, "/projets/*/affectations-ressources/**")
                        .hasAnyRole("ADMIN", "RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.PUT, "/projets/*/affectations-ressources/**")
                        .hasAnyRole("ADMIN", "RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.DELETE, "/projets/*/affectations-ressources/**")
                        .hasAnyRole("ADMIN", "RESPONSABLE_PROJET")
                        .requestMatchers(HttpMethod.GET, "/ressources/*/affectations")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/ressources/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ressources/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ressources/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ressources/**").hasRole("ADMIN")
                        .requestMatchers("/projets/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );
        configuration.setExposedHeaders(
                List.of("Content-Disposition")
        );
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
