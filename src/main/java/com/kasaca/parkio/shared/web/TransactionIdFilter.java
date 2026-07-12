package com.kasaca.parkio.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro transversal encargado de asignar un identificador unico a cada peticion.
 *
 * <p>El identificador permite relacionar una respuesta del backend con sus logs,
 * facilitando soporte, depuracion y trazabilidad entre frontend y backend.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    public static final String TRANSACTION_ID_ATTRIBUTE = "transactionId";
    public static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    /**
     * Ejecuta una sola vez por peticion HTTP para garantizar que exista un
     * identificador de transaccion disponible durante todo el flujo.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Obtiene el identificador enviado por el cliente o genera uno nuevo si no existe.
        String transactionId = getOrCreateTransactionId(request);

        // Guarda el identificador como atributo del request para que controllers y errores puedan leerlo.
        request.setAttribute(TRANSACTION_ID_ATTRIBUTE, transactionId);

        // Agrega el identificador al MDC para que pueda aparecer en logs si el patron de logging lo utiliza.
        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);

        // Devuelve el identificador tambien como header para que el frontend pueda mostrarlo o registrarlo.
        response.setHeader(TRANSACTION_ID_HEADER, transactionId);

        try {
            // Continua con el resto de filtros, seguridad, controllers y manejo de excepciones.
            filterChain.doFilter(request, response);
        } finally {
            // Limpia el MDC al terminar la peticion para evitar que otro hilo reutilice el mismo identificador.
            MDC.remove(TRANSACTION_ID_MDC_KEY);
        }
    }

    /**
     * Obtiene el identificador de transaccion actual o crea uno nuevo cuando la
     * peticion no trae un valor valido.
     */
    public static String getOrCreateTransactionId(HttpServletRequest request) {
        Object currentTransactionId = request.getAttribute(TRANSACTION_ID_ATTRIBUTE);

        if (currentTransactionId instanceof String value && !value.isBlank()) {
            return value;
        }

        String headerTransactionId = request.getHeader(TRANSACTION_ID_HEADER);

        if (headerTransactionId != null && !headerTransactionId.isBlank()) {
            request.setAttribute(TRANSACTION_ID_ATTRIBUTE, headerTransactionId);
            return headerTransactionId;
        }

        String generatedTransactionId = UUID.randomUUID().toString();
        request.setAttribute(TRANSACTION_ID_ATTRIBUTE, generatedTransactionId);

        return generatedTransactionId;
    }
}
