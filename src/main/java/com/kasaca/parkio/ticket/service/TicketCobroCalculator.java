package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calculador responsable de determinar el cobro de un ticket.
 *
 * <p>Se separa del service para mantener la regla de negocio aislada,
 * facil de probar y facil de modificar cuando cambien las politicas de cobro.</p>
 */
@Component
public class TicketCobroCalculator {

    private static final BigDecimal MINUTOS_POR_HORA = BigDecimal.valueOf(60);

    /**
     * Calcula el importe final de una estancia usando la tarifa activa del estacionamiento.
     *
     * <p>Regla actual: la tarifa minima se aplica desde el primer minuto. Si la
     * estancia esta dentro de la tolerancia, el cobro por tiempo es cero, pero
     * aun asi se respeta la tarifa minima configurada.</p>
     *
     * @param fechaEntrada fecha y hora real de entrada del ticket
     * @param fechaSalida fecha y hora real de salida del ticket
     * @param tarifa tarifa activa del estacionamiento al momento del cierre
     * @return resultado con monto final y parametros aplicados
     */
    public TicketCobroResultado calcular(
            LocalDateTime fechaEntrada,
            LocalDateTime fechaSalida,
            TarifaEstacionamiento tarifa
    ) {
        int minutosEstancia = calcularMinutosEstancia(fechaEntrada, fechaSalida);
        BigDecimal montoPorTiempo = calcularMontoPorTiempo(minutosEstancia, tarifa);
        BigDecimal montoTotal = aplicarTarifaMinima(montoPorTiempo, tarifa.getTarifaMinima());

        return new TicketCobroResultado(
                minutosEstancia,
                montoTotal,
                tarifa.getPrecioPorHora().setScale(2, RoundingMode.HALF_UP),
                tarifa.getMinutosTolerancia(),
                tarifa.getCobrarFraccion(),
                tarifa.getTarifaMinima().setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Calcula los minutos entre entrada y salida garantizando al menos un minuto.
     *
     * <p>Esto evita un cobro en cero cuando la entrada y salida ocurren dentro
     * del mismo minuto, caso comun en pruebas o registros muy rapidos.</p>
     */
    private int calcularMinutosEstancia(LocalDateTime fechaEntrada, LocalDateTime fechaSalida) {
        long minutos = Duration.between(fechaEntrada, fechaSalida).toMinutes();
        return Math.max(1, Math.toIntExact(minutos));
    }

    /**
     * Calcula el monto generado por tiempo antes de aplicar la tarifa minima.
     */
    private BigDecimal calcularMontoPorTiempo(int minutosEstancia, TarifaEstacionamiento tarifa) {
        if (minutosEstancia <= tarifa.getMinutosTolerancia()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (Boolean.TRUE.equals(tarifa.getCobrarFraccion())) {
            return calcularMontoConFraccionComoHoraCompleta(minutosEstancia, tarifa.getPrecioPorHora());
        }

        return calcularMontoProporcional(minutosEstancia, tarifa.getPrecioPorHora());
    }

    /**
     * Calcula el monto redondeando cualquier fraccion como hora completa.
     */
    private BigDecimal calcularMontoConFraccionComoHoraCompleta(int minutosEstancia, BigDecimal precioPorHora) {
        long horasACobrar = (long) Math.ceil(minutosEstancia / 60.0);
        return precioPorHora
                .multiply(BigDecimal.valueOf(horasACobrar))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el monto proporcional exacto por minutos consumidos.
     */
    private BigDecimal calcularMontoProporcional(int minutosEstancia, BigDecimal precioPorHora) {
        return precioPorHora
                .multiply(BigDecimal.valueOf(minutosEstancia))
                .divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica la tarifa minima desde el primer minuto de estancia.
     */
    private BigDecimal aplicarTarifaMinima(BigDecimal montoPorTiempo, BigDecimal tarifaMinima) {
        BigDecimal tarifaMinimaEscalada = tarifaMinima.setScale(2, RoundingMode.HALF_UP);
        return montoPorTiempo.max(tarifaMinimaEscalada).setScale(2, RoundingMode.HALF_UP);
    }
}
