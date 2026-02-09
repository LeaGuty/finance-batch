package cl.duoc.finance_batch.config;
import cl.duoc.finance_batch.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Aquí no hay login público, todo es privado
                .requestMatchers("/api/v1/**").authenticated() 
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        // Registramos TODOS los usuarios posibles para poder validar sus tokens
        UserDetails userWeb = User.withDefaultPasswordEncoder()
            .username("usuario_web").password("unused").roles("CLIENTE_WEB").build();
            
        UserDetails userMovil = User.withDefaultPasswordEncoder()
            .username("usuario_movil").password("unused").roles("CLIENTE_MOVIL").build();
            
        UserDetails userAtm = User.withDefaultPasswordEncoder()
            .username("cajero_atm_01").password("unused").roles("CAJERO_AUT").build();

        return new InMemoryUserDetailsManager(userWeb, userMovil, userAtm);
    }
}