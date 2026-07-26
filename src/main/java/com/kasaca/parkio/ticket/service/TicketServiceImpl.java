package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.reserva.repository.ReservaRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.ticket.mapper.TicketMapper;
import com.kasaca.parkio.ticket.repository.TicketRepository;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementacion de negocio para administrar tickets.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private static final String PREFIJO_CODIGO_TICKET = "TCK-";
    private static final String ROL_OPERADOR = "OPERADOR";

    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajonRepository cajonRepository;
    private final TicketMapper ticketMapper;

    /**
     * Registra la entrada de un vehiculo al estacionamiento.
     *
     * <p>Convierte una reserva activa, vigente y en estado CREADA en un ticket
     * ABIERTO. Tambien marca la reserva como USADA y cambia el cajon a OCUPADO.</p>
     */
    @Override
    @Transactional
    public TicketResponse registrarEntrada(Long operadorId, TicketEntradaRequest request) {
        LocalDateTime fechaActual = LocalDateTime.now();

        Usuario operador = findOperadorById(operadorId);
        Reserva reserva = findReservaByCodigo(request.codigoReserva());

        validarReservaCreada(reserva);
        validarReservaVigente(reserva, fechaActual);
        validarOperadorTieneRolOperador(operador);
        validarOperadorAsignadoAlEstacionamiento(operador, reserva.getEstacionamiento());
        validarReservaSinTicket(reserva.getId());
        validarCajonSinTicketAbierto(reserva.getCajon().getId());

        String codigoTicket = generarCodigoTicket();

        Ticket ticket = ticketMapper.toEntity(
                codigoTicket,
                reserva.getPlaca(),
                fechaActual,
                reserva,
                reserva.getUsuario(),
                operador,
                reserva.getEstacionamiento(),
                reserva.getCajon()
        );

        reserva.setEstado(EstadoReserva.USADA);

        Cajon cajon = reserva.getCajon();
        cajon.setEstado(EstadoCajon.OCUPADO);

        cajonRepository.save(cajon);
        reservaRepository.save(reserva);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        return ticketMapper.toResponse(ticketGuardado);
    }

    /**
     * Busca al operador activo por identificador.
     *
     * <p>Si el usuario no existe o esta inactivo, se responde como recurso no encontrado.</p>
     */
    private Usuario findOperadorById(Long operadorId) {
        return usuarioRepository.findByIdAndActivoTrue(operadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", operadorId));
    }

    /**
     * Busca una reserva activa por su codigo publico.
     *
     * <p>El codigo es el dato que el cliente presenta al llegar al estacionamiento.</p>
     */
    private Reserva findReservaByCodigo(String codigoReserva) {
        return reservaRepository.findByCodigoAndActivoTrue(codigoReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", codigoReserva));
    }

    /**
     * Valida que la reserva este en estado CREADA.
     *
     * <p>Una reserva CANCELADA, EXPIRADA o USADA no puede convertirse en ticket.</p>
     */
    private void validarReservaCreada(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.CREADA) {
            throw new ConflictException("Solo se puede generar ticket para reservas en estado CREADA.");
        }
    }

    /**
     * Valida que la reserva siga vigente al momento de registrar la entrada.
     */
    private void validarReservaVigente(Reserva reserva, LocalDateTime fechaActual) {
        if (!reserva.getFechaExpiracion().isAfter(fechaActual)) {
            throw new ConflictException("La reserva ya expiro y no puede convertirse en ticket.");
        }
    }

    /**
     * Valida que el usuario autenticado tenga el rol OPERADOR.
     *
     * <p>La FK del ticket solo garantiza que el usuario exista; el rol funcional
     * se valida en la capa de negocio usando la relacion usuario_rol.</p>
     */
    private void validarOperadorTieneRolOperador(Usuario operador) {
        boolean tieneRolOperador = operador.getRoles()
                .stream()
                .anyMatch(rol -> ROL_OPERADOR.equals(rol.getNombre()));

        if (!tieneRolOperador) {
            throw new ConflictException("El usuario autenticado no tiene rol OPERADOR.");
        }
    }

    /**
     * Valida que el operador este asignado al estacionamiento de la reserva.
     *
     * <p>Esto evita que un operador de otro estacionamiento registre entradas
     * sobre reservas que no le corresponden.</p>
     */
    private void validarOperadorAsignadoAlEstacionamiento(
            Usuario operador,
            Estacionamiento estacionamiento
    ) {
        boolean asignadoAlEstacionamiento = operador.getEstacionamientos()
                .stream()
                .anyMatch(estacionamientoAsignado ->
                        estacionamientoAsignado.getId().equals(estacionamiento.getId())
                );

        if (!asignadoAlEstacionamiento) {
            throw new ConflictException("El operador no esta asignado al estacionamiento de la reserva.");
        }
    }

    /**
     * Valida que la reserva no tenga ya un ticket activo.
     *
     * <p>Esta regla protege la relacion uno a uno entre reserva y ticket.</p>
     */
    private void validarReservaSinTicket(Long reservaId) {
        boolean existeTicket = ticketRepository.existsByReservaIdAndActivoTrue(reservaId);

        if (existeTicket) {
            throw new ConflictException("La reserva ya fue convertida en ticket.");
        }
    }

    /**
     * Valida que el cajon no tenga un ticket abierto.
     *
     * <p>Esto evita registrar dos vehiculos ocupando el mismo cajon al mismo tiempo.</p>
     */
    private void validarCajonSinTicketAbierto(Long cajonId) {
        boolean existeTicketAbierto = ticketRepository
                .existsByCajonIdAndEstadoAndActivoTrue(cajonId, EstadoTicket.ABIERTO);

        if (existeTicketAbierto) {
            throw new ConflictException("El cajon ya tiene un ticket abierto.");
        }
    }

    /**
     * Genera un codigo publico unico para el ticket.
     *
     * <p>El codigo se valida contra base de datos para reducir el riesgo de colision.</p>
     */
    private String generarCodigoTicket() {
        String codigo;

        do {
            codigo = PREFIJO_CODIGO_TICKET + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (ticketRepository.findByCodigoAndActivoTrue(codigo).isPresent());

        return codigo;
    }
}
