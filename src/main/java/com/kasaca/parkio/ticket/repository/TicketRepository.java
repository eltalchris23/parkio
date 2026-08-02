package com.kasaca.parkio.ticket.repository;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para tickets.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /**
     * Consulta tickets activos de forma paginada.
     */
    Page<Ticket> findByActivoTrue(Pageable pageable);

    /**
     * Consulta un ticket activo por identificador interno.
     */
    Optional<Ticket> findByIdAndActivoTrue(Long id);

    /**
     * Consulta un ticket activo por codigo publico.
     */
    Optional<Ticket> findByCodigoAndActivoTrue(String codigo);

    /**
     * Consulta tickets activos de un usuario cliente.
     */
    Page<Ticket> findByUsuarioIdAndActivoTrue(Long usuarioId, Pageable pageable);

    /**
     * Consulta tickets activos de un estacionamiento.
     */
    Page<Ticket> findByEstacionamientoIdAndActivoTrue(Long estacionamientoId, Pageable pageable);

    /**
     * Consulta tickets activos de estacionamientos pertenecientes a un OWNER.
     */
    Page<Ticket> findByEstacionamientoOwnerIdAndActivoTrue(Long ownerId, Pageable pageable);

    /**
     * Consulta tickets activos de los estacionamientos asignados a un OPERADOR.
     */
    Page<Ticket> findByEstacionamientoInAndActivoTrue(
            Collection<Estacionamiento> estacionamientos,
            Pageable pageable
    );

    /**
     * Valida si una reserva ya fue convertida en ticket activo.
     *
     * <p>Esta consulta evita generar mas de un ticket para la misma reserva.</p>
     */
    boolean existsByReservaIdAndActivoTrue(Long reservaId);

    /**
     * Valida si un cajon ya tiene un ticket activo en estado ABIERTO.
     *
     * <p>Esta consulta evita ocupar un cajon que ya tiene una estancia en curso.</p>
     */
    boolean existsByCajonIdAndEstadoAndActivoTrue(Long cajonId, EstadoTicket estado);
}
