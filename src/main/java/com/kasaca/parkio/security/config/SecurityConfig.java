package com.kasaca.parkio.security.config;

import com.kasaca.parkio.security.jwt.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuracion central de Spring Security para Parkio.
 *
 * <p>Define que endpoints son publicos, que endpoints requieren autenticacion
 * y como se validan los tokens JWT recibidos por la API.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Configura la cadena principal de filtros HTTP.
     *
     * <p>El login y la creacion de usuarios quedan publicos para permitir el
     * arranque inicial del sistema. El resto de endpoints requiere un token JWT
     * valido. La sesion se desactiva porque la API trabaja de forma stateless.</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Devuelve la configuracion final de seguridad que Spring usara para procesar cada peticion HTTP.
        return http
                // Activa CORS usando el bean CorsConfigurationSource definido en CorsConfig.
                .cors(Customizer.withDefaults())
                // Desactiva CSRF porque la API es stateless y se autentica con JWT, no con cookies de sesion.
                .csrf(csrf -> csrf.disable())
                // Indica que Spring Security no debe crear ni usar sesiones HTTP para guardar autenticacion.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define las reglas de autorizacion para los endpoints de la API.
                .authorizeHttpRequests(auth -> auth
                        // Permite consultar el estado general de la aplicacion sin JWT.
                        // Este endpoint es usado por monitoreo, balanceadores o contenedores.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Permite consultar si el proceso de la aplicacion sigue vivo.
                        .requestMatchers(HttpMethod.GET, "/actuator/health/liveness").permitAll()
                        // Permite consultar si la aplicacion esta lista para recibir trafico.
                        .requestMatchers(HttpMethod.GET, "/actuator/health/readiness").permitAll()
                        // Permite consultar el contrato OpenAPI en ambientes donde Springdoc este habilitado.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs").permitAll()
                        // Permite consultar recursos complementarios del contrato OpenAPI.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                        // Permite consultar OpenAPI cuando Springdoc queda expuesto bajo el servlet path /api/v1.
                        .requestMatchers(HttpMethod.GET, "/api/v1/v3/api-docs").permitAll()
                        // Permite consultar recursos OpenAPI bajo /api/v1 cuando aplica el prefijo global MVC.
                        .requestMatchers(HttpMethod.GET, "/api/v1/v3/api-docs/**").permitAll()
                        // Permite abrir Swagger UI desde navegador cuando este habilitado por perfil.
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
                        // Permite cargar los recursos internos de Swagger UI como JavaScript y CSS.
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
                        // Permite abrir Swagger UI cuando Springdoc queda expuesto bajo el servlet path /api/v1.
                        .requestMatchers(HttpMethod.GET, "/api/v1/swagger-ui.html").permitAll()
                        // Permite cargar recursos internos de Swagger UI bajo /api/v1.
                        .requestMatchers(HttpMethod.GET, "/api/v1/swagger-ui/**").permitAll()
                        // Permite acceder al login sin token, porque este endpoint genera el JWT.
                        .requestMatchers("/auth/login").permitAll()
                        // Permite crear usuarios sin token para facilitar el registro inicial del sistema.
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        // Exige autenticacion JWT valida para cualquier otro endpoint no declarado como publico.
                        .anyRequest().authenticated()
                )
                // Usa una respuesta JSON personalizada cuando una peticion no esta autenticada correctamente.
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                // Activa OAuth2 Resource Server para validar JWT y convertir roles del token en authorities de Spring.
                // Agrega el usuario autenticado al contexto de Spring.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Construye la cadena de filtros; internamente aqui queda incluido el filtro que pobla el SecurityContext.
                .build();
    }

    /**
     * Convierte el JWT validado en un objeto Authentication entendible por Spring Security.
     *
     * <p>Este converter toma el claim "roles" del token y lo transforma en authorities.
     * Por ejemplo, ADMIN se convierte en ROLE_ADMIN.</p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Crea el converter encargado de leer authorities desde un claim especifico del JWT.
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Indica que los roles se deben leer desde el claim "roles" generado en JwtService.
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        // Agrega el prefijo ROLE_ para poder usar hasRole('ADMIN') o @PreAuthorize("hasRole('ADMIN')").
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        // Crea el converter principal que construye el Authentication usado por Spring Security.
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

        // Conecta el converter de roles al converter principal de autenticacion.
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        // Devuelve el converter listo para que OAuth2 Resource Server lo use al procesar cada JWT valido.
        return authenticationConverter;
    }

    /**
     * Crea el codificador encargado de firmar los JWT emitidos por la aplicacion.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretBytes()));
    }

    /**
     * Crea el decodificador encargado de validar los JWT recibidos en peticiones.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecretBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Convierte el secreto configurado a bytes para firmar y validar tokens.
     */
    private byte[] jwtSecretBytes() {
        return jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    }
}
