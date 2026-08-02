package com.kasaca.parkio.ticket.service;

import java.math.BigDecimal;

/**
 * Resultado inmutable del calculo de cobro de un ticket.
 *
 * <p>Incluye el monto final y una copia de los parametros de tarifa usados
 * para dejar trazabilidad historica dentro del ticket cerrado.</p>
 */
public record TicketCobroResultado(
        Integer minutosEstancia,
        BigDecimal montoTotal,
        BigDecimal precioPorHoraAplicado,
        Integer minutosToleranciaAplicados,
        Boolean cobrarFraccionAplicado,
        BigDecimal tarifaMinimaAplicada
) {
}
