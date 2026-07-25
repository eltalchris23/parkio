package com.kasaca.parkio.reserva.scheduler;

import com.kasaca.parkio.reserva.service.ReservaService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservaSchedulerTest {

    /**
     * Verifica que el scheduler delegue al service la expiracion de reservas vencidas.
     */
    @Test
    void debeEjecutarExpiracionDeReservasVencidas() {
        ReservaService reservaService = mock(ReservaService.class);
        ReservaScheduler reservaScheduler = new ReservaScheduler(reservaService);

        when(reservaService.expirarReservasVencidas()).thenReturn(2);

        reservaScheduler.expirarReservasVencidas();

        verify(reservaService).expirarReservasVencidas();
    }
}
