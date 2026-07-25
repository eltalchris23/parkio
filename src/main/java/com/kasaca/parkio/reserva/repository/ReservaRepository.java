package com.kasaca.parkio.reserva.repository;

import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para reservas.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Consulta reservas activas de forma paginada.
     */
    Page<Reserva> findByActivoTrue(Pageable pageable);

    /**
     * Consulta una reserva activa por identificador.
     */
    Optional<Reserva> findByIdAndActivoTrue(Long id);

    /**
     * Consulta una reserva activa por su codigo publico.
     */
    Optional<Reserva> findByCodigoAndActivoTrue(String codigo);

    /**
     * Consulta reservas activas de un usuario.
     */
    Page<Reserva> findByUsuarioIdAndActivoTrue(Long usuarioId, Pageable pageable);

    /**
     * Consulta reservas activas de un estacionamiento.
     */
    Page<Reserva> findByEstacionamientoIdAndActivoTrue(Long estacionamientoId, Pageable pageable);

    /**
     * Valida si existe una reserva activa y vigente para el cajon.
     *
     * <p>Esta consulta ayudara a evitar que dos usuarios aparten el mismo cajon
     * al mismo tiempo mientras la reserva siga creada y no haya expirado.</p>
     */
    boolean existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrue(
            Long cajonId,
            EstadoReserva estado,
            LocalDateTime fechaActual
    );

    /**
     * Valida si existe otra reserva activa y vigente para el cajon excluyendo una reserva concreta.
     *
     * <p>Esta consulta se usa al cancelar o expirar una reserva para evitar liberar
     * un cajon que ya tenga otra reserva vigente diferente.</p>
     */
    boolean existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrueAndIdNot(
            Long cajonId,
            EstadoReserva estado,
            LocalDateTime fechaActual,
            Long reservaId
    );

    /**
     * Busca reservas creadas que ya vencieron.
     *
     * <p>Esta consulta sera util cuando implementemos la expiracion automatica
     * o manual de reservas pendientes.</p>
     */
    List<Reserva> findByEstadoAndFechaExpiracionBeforeAndActivoTrue(
            EstadoReserva estado,
            LocalDateTime fechaActual
    );
}
