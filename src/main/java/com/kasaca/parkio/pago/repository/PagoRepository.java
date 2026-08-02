package com.kasaca.parkio.pago.repository;

import com.kasaca.parkio.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para pagos.
 */
public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    /**
     * Verifica si un ticket ya tiene un pago activo registrado.
     */
    boolean existsByTicketIdAndActivoTrue(Long ticketId);

    /**
     * Consulta el pago activo asociado a un ticket.
     */
    Optional<Pago> findByTicketIdAndActivoTrue(Long ticketId);
}
