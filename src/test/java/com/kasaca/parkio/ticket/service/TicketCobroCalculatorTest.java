package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketCobroCalculatorTest {

    private TicketCobroCalculator ticketCobroCalculator;

    /**
     * Crea el calculador real para probar la regla de cobro sin dependencias externas.
     */
    @BeforeEach
    void setUp() {
        ticketCobroCalculator = new TicketCobroCalculator();
    }

    /**
     * Verifica que la tarifa minima se cobre incluso cuando la estancia esta dentro de la tolerancia.
     */
    @Test
    void debeAplicarTarifaMinimaDesdeElPrimerMinuto() {
        TarifaEstacionamiento tarifa = crearTarifa(new BigDecimal("25.00"), 10, true, new BigDecimal("15.00"));

        TicketCobroResultado resultado = ticketCobroCalculator.calcular(
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 5),
                tarifa
        );

        assertThat(resultado.minutosEstancia()).isEqualTo(5);
        assertThat(resultado.montoTotal()).isEqualByComparingTo("15.00");
    }

    /**
     * Verifica que una fraccion de hora se cobre como hora completa cuando la tarifa lo indica.
     */
    @Test
    void debeCobrarFraccionComoHoraCompleta() {
        TarifaEstacionamiento tarifa = crearTarifa(new BigDecimal("25.00"), 0, true, new BigDecimal("15.00"));

        TicketCobroResultado resultado = ticketCobroCalculator.calcular(
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 11, 10),
                tarifa
        );

        assertThat(resultado.minutosEstancia()).isEqualTo(70);
        assertThat(resultado.montoTotal()).isEqualByComparingTo("50.00");
        assertThat(resultado.precioPorHoraAplicado()).isEqualByComparingTo("25.00");
        assertThat(resultado.minutosToleranciaAplicados()).isZero();
        assertThat(resultado.cobrarFraccionAplicado()).isTrue();
        assertThat(resultado.tarifaMinimaAplicada()).isEqualByComparingTo("15.00");
    }

    /**
     * Verifica el cobro proporcional cuando la tarifa no cobra fracciones como hora completa.
     */
    @Test
    void debeCobrarMontoProporcionalCuandoNoCobraFraccion() {
        TarifaEstacionamiento tarifa = crearTarifa(new BigDecimal("30.00"), 0, false, new BigDecimal("10.00"));

        TicketCobroResultado resultado = ticketCobroCalculator.calcular(
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 30),
                tarifa
        );

        assertThat(resultado.minutosEstancia()).isEqualTo(30);
        assertThat(resultado.montoTotal()).isEqualByComparingTo("15.00");
    }

    /**
     * Verifica que una entrada y salida dentro del mismo minuto sigan generando un minuto minimo.
     */
    @Test
    void debeGarantizarMinimoUnMinutoDeEstancia() {
        TarifaEstacionamiento tarifa = crearTarifa(new BigDecimal("30.00"), 0, false, BigDecimal.ZERO);

        TicketCobroResultado resultado = ticketCobroCalculator.calcular(
                LocalDateTime.of(2026, 8, 1, 10, 0, 10),
                LocalDateTime.of(2026, 8, 1, 10, 0, 50),
                tarifa
        );

        assertThat(resultado.minutosEstancia()).isEqualTo(1);
        assertThat(resultado.montoTotal()).isEqualByComparingTo("0.50");
    }

    /**
     * Construye una tarifa minima para probar combinaciones de reglas de cobro.
     */
    private TarifaEstacionamiento crearTarifa(
            BigDecimal precioPorHora,
            Integer minutosTolerancia,
            Boolean cobrarFraccion,
            BigDecimal tarifaMinima
    ) {
        TarifaEstacionamiento tarifa = new TarifaEstacionamiento();
        tarifa.setPrecioPorHora(precioPorHora);
        tarifa.setMinutosTolerancia(minutosTolerancia);
        tarifa.setCobrarFraccion(cobrarFraccion);
        tarifa.setTarifaMinima(tarifaMinima);
        return tarifa;
    }
}
