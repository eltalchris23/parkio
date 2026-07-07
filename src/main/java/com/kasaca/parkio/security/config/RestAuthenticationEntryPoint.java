package com.kasaca.parkio.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Punto de entrada para errores de autenticacion producidos por Spring Security.
 *
 * <p>Se ejecuta cuando una peticion intenta acceder a un recurso protegido sin
 * token JWT o con un token invalido. Permite devolver el mismo formato
 * {@link ApiError} usado por el manejador global de excepciones.</p>
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Construye una respuesta HTTP 401 en formato JSON cuando la autenticacion falla.
     *
     * @param request peticion HTTP que produjo el error
     * @param response respuesta HTTP donde se escribe el error
     * @param authException excepcion de autenticacion generada por Spring Security
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Autenticacion requerida",
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
