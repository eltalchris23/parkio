package com.kasaca.parkio.security.cors;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion central de CORS para permitir que el frontend consuma la API.
 *
 * <p>CORS es una proteccion del navegador. No reemplaza JWT ni autorizacion por roles.
 * Solo define que origenes web pueden llamar al backend desde JavaScript.</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private final CorsProperties corsProperties;

    /**
     * Construye la configuracion CORS usada por Spring Security.
     *
     * <p>Se registra para todas las rutas porque la API completa vive bajo /api/v1
     * y las peticiones preflight OPTIONS deben poder resolverse antes de llegar
     * a la logica de autenticacion.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Define que frontends pueden consumir el backend.
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());

        // Define los metodos HTTP permitidos desde el navegador.
        configuration.setAllowedMethods(corsProperties.allowedMethods());

        // Define los headers que el frontend puede enviar, como Authorization y X-Transaction-Id.
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());

        // Define los headers que el frontend puede leer desde la respuesta.
        configuration.setExposedHeaders(corsProperties.exposedHeaders());

        // No se usan cookies de sesion porque la API trabaja con JWT Bearer.
        configuration.setAllowCredentials(false);

        // Permite cachear la respuesta preflight para reducir llamadas OPTIONS repetidas.
        configuration.setMaxAge(corsProperties.maxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Aplica esta configuracion CORS a todos los endpoints.
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
