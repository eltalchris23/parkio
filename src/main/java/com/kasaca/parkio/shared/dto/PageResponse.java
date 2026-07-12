package com.kasaca.parkio.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Representa una pagina de resultados con una estructura JSON controlada por Parkio.
 *
 * <p>Este DTO evita exponer directamente la serializacion interna de
 * {@link Page}, manteniendo estable el contrato de la API para el frontend.</p>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {

    /**
     * Convierte una pagina de Spring Data en el formato paginado estandar de Parkio.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
