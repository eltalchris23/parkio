package com.kasaca.parkio.reserva.scheduler;

import com.kasaca.parkio.reserva.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaScheduler {

    private final ReservaService reservaService;

    /**
     * Expira automaticamente reservas vencidas.
     *
     * <p>Busca reservas en estado CREADA cuya fecha de expiracion ya paso.
     * El service las marca como EXPIRADA y libera sus cajones cuando corresponde.</p>
     */
    @Scheduled(
            fixedDelayString = "${parkio.reserva.expiracion-check-ms:60000}",
            initialDelayString = "${parkio.reserva.expiracion-check-ms:60000}"
    )
    public void expirarReservasVencidas() {
        log.info("INICIO - Expiracion automatica de reservas vencidas");

        int totalExpiradas = reservaService.expirarReservasVencidas();

        log.info(
                "FIN - Expiracion automatica de reservas vencidas. totalExpiradas={}",
                totalExpiradas
        );
    }
}
