package com.kasaca.parkio.shared.dto;

import com.kasaca.parkio.shared.web.TransactionIdFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * Representa la respuesta estandar para operaciones exitosas de la API.
 *
 * <p>Permite entregar al cliente metadatos comunes como codigo HTTP,
 * mensaje funcional, identificador de transaccion, fecha de respuesta y
 * datos de negocio.</p>
 */
public record ApiResponse<T>(
        LocalDateTime timestamp,
        int status,
        String message,
        String transactionId,
        T data
) {

    /**
     * Construye una respuesta exitosa tomando el identificador de transaccion
     * generado para la peticion actual.
     */
    public static <T> ApiResponse<T> of(
            HttpServletRequest request,
            int status,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                LocalDateTime.now(),
                status,
                message,
                TransactionIdFilter.getOrCreateTransactionId(request),
                data
        );
    }
}
