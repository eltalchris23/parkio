package com.kasaca.parkio.security.cors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades configurables para CORS.
 *
 * <p>Permiten definir desde application.yaml o variables de entorno que frontends
 * pueden consumir la API y que headers/metodos HTTP estan permitidos.</p>
 */
@ConfigurationProperties(prefix = "parkio.cors")
public record CorsProperties(

        /**
         * Origenes permitidos para consumir la API desde navegador.
         *
         * <p>Ejemplos locales comunes:
         * http://localhost:4200 para Angular,
         * http://localhost:5173 para Vite/React/Vue.</p>
         */
        List<String> allowedOrigins,

        /**
         * Metodos HTTP permitidos para llamadas CORS.
         */
        List<String> allowedMethods,

        /**
         * Headers que el frontend puede enviar al backend.
         *
         * <p>Authorization permite enviar JWT.
         * X-Transaction-Id permite enviar un identificador de trazabilidad.</p>
         */
        List<String> allowedHeaders,

        /**
         * Headers que el navegador podra leer desde la respuesta.
         *
         * <p>Por defecto los navegadores no exponen todos los headers al JavaScript del frontend.</p>
         */
        List<String> exposedHeaders,

        /**
         * Tiempo en segundos que el navegador puede cachear la configuracion CORS.
         */
        long maxAgeSeconds

) {
}
