package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.reserva.repository.ReservaRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import com.kasaca.parkio.tarifa.repository.TarifaEstacionamientoRepository;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.ticket.mapper.TicketMapper;
import com.kasaca.parkio.ticket.repository.TicketRepository;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_OWNER = "OWNER";
    private static final String ROL_OPERADOR = "OPERADOR";

    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajonRepository cajonRepository;
    private final TarifaEstacionamientoRepository tarifaEstacionamientoRepository;
    private final TicketCobroCalculator ticketCobroCalculator;
    private final TicketMapper ticketMapper;

    /**
     * Consulta tickets activos de forma paginada respetando el alcance del usuario autenticado.
     *
     * <p>ADMIN consulta todos los tickets. OWNER consulta tickets de sus estacionamientos.
     * OPERADOR consulta tickets de estacionamientos asignados. USER consulta solo sus propios tickets.
     * Los filtros de estado y estacionamiento se aplican encima de ese alcance.</p>
     */
    @Override
    public PageResponse<TicketResponse> getTickets(
            Long usuarioAutenticadoId,
            EstadoTicket estado,
            Long estacionamientoId,
            Pageable pageable
    ) {
        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Page<Ticket> tickets = findTicketsByAlcance(
                usuarioAutenticado,
                estado,
                estacionamientoId,
                pageable
        );

        return PageResponse.from(tickets.map(ticketMapper::toResponse));
    }

    /**
     * Consulta un ticket activo por identificador y valida que el usuario tenga alcance para verlo.
     */
    @Override
    public TicketResponse getTicketById(Long usuarioAutenticadoId, Long ticketId) {
        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Ticket ticket = findTicketById(ticketId);

        validarUsuarioPuedeConsultarTicket(usuarioAutenticado, ticket);

        return ticketMapper.toResponse(ticket);
    }

    /**
     * Registra la entrada de un vehiculo al estacionamiento.
     *
     * <p>Convierte una reserva activa, vigente y en estado CREADA en un ticket
     * ABIERTO. Tambien marca la reserva como USADA y cambia el cajon a OCUPADO.
     * ADMIN puede operar cualquier estacionamiento, OWNER solo los propios y
     * OPERADOR solo los estacionamientos asignados.</p>
     */
    @Override
    @Transactional
    public TicketResponse registrarEntrada(Long usuarioAutenticadoId, TicketEntradaRequest request) {
        LocalDateTime fechaActual = LocalDateTime.now();

        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Reserva reserva = findReservaByCodigo(request.codigoReserva());

        validarReservaCreada(reserva);
        validarReservaVigente(reserva, fechaActual);
        validarUsuarioPuedeOperarTicket(usuarioAutenticado, reserva.getEstacionamiento());
        validarReservaSinTicket(reserva.getId());
        validarCajonSinTicketAbierto(reserva.getCajon().getId());

        String codigoTicket = generarCodigoTicket();

        Ticket ticket = ticketMapper.toEntity(
                codigoTicket,
                reserva.getPlaca(),
                fechaActual,
                reserva,
                reserva.getUsuario(),
                usuarioAutenticado,
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
     * Registra la salida operativa de un vehiculo y calcula el monto a pagar.
     *
     * <p>Este metodo no libera el cajon. Cambia el ticket de ABIERTO a
     * PENDIENTE_PAGO, registra fecha de salida y calcula el cobro con la tarifa
     * activa del estacionamiento. El cajon se libera hasta que el cajero registre
     * el pago en el modulo Pago. ADMIN puede operar cualquier estacionamiento,
     * OWNER solo los propios y OPERADOR solo los estacionamientos asignados.</p>
     */
    @Override
    @Transactional
    public TicketResponse registrarSalida(Long usuarioAutenticadoId, Long ticketId) {
        LocalDateTime fechaActual = LocalDateTime.now();

        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Ticket ticket = findTicketById(ticketId);

        validarTicketAbierto(ticket);
        validarUsuarioPuedeOperarTicket(usuarioAutenticado, ticket.getEstacionamiento());

        TarifaEstacionamiento tarifa = findTarifaActivaByEstacionamientoId(ticket.getEstacionamiento().getId());
        TicketCobroResultado cobro = ticketCobroCalculator.calcular(
                ticket.getFechaEntrada(),
                fechaActual,
                tarifa
        );

        ticket.setEstado(EstadoTicket.PENDIENTE_PAGO);
        ticket.setFechaSalida(fechaActual);
        aplicarCobroAlTicket(ticket, cobro);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        return ticketMapper.toResponse(ticketGuardado);
    }

    /**
     * Busca al usuario autenticado activo por identificador.
     *
     * <p>Si el usuario no existe o esta inactivo, se responde como recurso no encontrado.</p>
     */
    private Usuario findUsuarioAutenticadoById(Long usuarioAutenticadoId) {
        return usuarioRepository.findByIdAndActivoTrue(usuarioAutenticadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioAutenticadoId));
    }

    /**
     * Busca un ticket activo por identificador interno.
     *
     * <p>Los tickets inactivos se tratan como inexistentes para la API.</p>
     */
    private Ticket findTicketById(Long ticketId) {
        return ticketRepository.findByIdAndActivoTrue(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
    }

    /**
     * Selecciona la consulta paginada correcta segun el rol y filtros solicitados.
     *
     * <p>El orden de prioridad permite que ADMIN mantenga alcance global incluso
     * si tambien tuviera otros roles asignados. Los filtros opcionales permiten
     * consultar por estado y/o estacionamiento sin crear multiples endpoints.</p>
     */
    private Page<Ticket> findTicketsByAlcance(
            Usuario usuarioAutenticado,
            EstadoTicket estado,
            Long estacionamientoId,
            Pageable pageable
    ) {
        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)) {
            if (usuarioAutenticado.getEstacionamientos().isEmpty()) {
                return Page.empty(pageable);
            }
        }

        Specification<Ticket> specification = buildTicketSpecification(
                usuarioAutenticado,
                estado,
                estacionamientoId
        );

        return ticketRepository.findAll(specification, pageable);
    }

    /**
     * Construye la especificacion dinamica de consulta para tickets.
     *
     * <p>Siempre filtra tickets activos. Despues agrega los filtros opcionales
     * solicitados por el frontend y finalmente aplica el alcance del usuario
     * autenticado para evitar que vea informacion fuera de sus permisos.</p>
     */
    private Specification<Ticket> buildTicketSpecification(
            Usuario usuarioAutenticado,
            EstadoTicket estado,
            Long estacionamientoId
    ) {
        Specification<Ticket> specification = ticketActivo();

        if (estado != null) {
            specification = specification.and(ticketConEstado(estado));
        }

        if (estacionamientoId != null) {
            specification = specification.and(ticketDeEstacionamiento(estacionamientoId));
        }

        return specification.and(ticketDentroDelAlcance(usuarioAutenticado));
    }

    /**
     * Filtra solo tickets activos.
     *
     * <p>Esto mantiene la convencion del proyecto: los registros inactivos no
     * deben aparecer en consultas normales de la API.</p>
     */
    private Specification<Ticket> ticketActivo() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("activo"));
    }

    /**
     * Filtra tickets por estado cuando el frontend envia estado=ABIERTO, PENDIENTE_PAGO o CERRADO.
     */
    private Specification<Ticket> ticketConEstado(EstadoTicket estado) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("estado"), estado);
    }

    /**
     * Filtra tickets por estacionamiento cuando el frontend envia estacionamientoId.
     */
    private Specification<Ticket> ticketDeEstacionamiento(Long estacionamientoId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("estacionamiento").get("id"), estacionamientoId);
    }

    /**
     * Aplica el alcance de seguridad del usuario autenticado sobre la consulta.
     *
     * <p>ADMIN ve todos los tickets activos. OWNER ve tickets de sus propios
     * estacionamientos. OPERADOR ve tickets de estacionamientos asignados. USER
     * ve solamente tickets donde sea el cliente asociado.</p>
     */
    private Specification<Ticket> ticketDentroDelAlcance(Usuario usuarioAutenticado) {
        if (tieneRol(usuarioAutenticado, ROL_ADMIN)) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.conjunction();
        }

        if (tieneRol(usuarioAutenticado, ROL_OWNER)) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            root.get("estacionamiento").get("owner").get("id"),
                            usuarioAutenticado.getId()
                    );
        }

        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)) {
            return (root, query, criteriaBuilder) ->
                    root.get("estacionamiento").in(usuarioAutenticado.getEstacionamientos());
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("usuario").get("id"), usuarioAutenticado.getId());
    }

    /**
     * Valida que el usuario autenticado pueda consultar el ticket solicitado.
     *
     * <p>ADMIN puede consultar todo, OWNER solo tickets de sus estacionamientos,
     * OPERADOR solo tickets de estacionamientos asignados y USER solo tickets propios.</p>
     */
    private void validarUsuarioPuedeConsultarTicket(Usuario usuarioAutenticado, Ticket ticket) {
        if (tieneRol(usuarioAutenticado, ROL_ADMIN)) {
            return;
        }

        if (tieneRol(usuarioAutenticado, ROL_OWNER)
                && esOwnerDelEstacionamiento(usuarioAutenticado, ticket.getEstacionamiento())) {
            return;
        }

        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)
                && estaAsignadoAlEstacionamiento(usuarioAutenticado, ticket.getEstacionamiento())) {
            return;
        }

        if (ticket.getUsuario().getId().equals(usuarioAutenticado.getId())) {
            return;
        }

        throw new ConflictException("El usuario autenticado no puede consultar este ticket.");
    }

    /**
     * Busca la tarifa activa del estacionamiento asociado al ticket.
     *
     * <p>El cierre requiere tarifa configurada porque el sistema necesita saber
     * cuanto cobrar antes de marcar el ticket como PENDIENTE_PAGO.</p>
     */
    private TarifaEstacionamiento findTarifaActivaByEstacionamientoId(Long estacionamientoId) {
        return tarifaEstacionamientoRepository.findByEstacionamientoIdAndActivoTrue(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa del estacionamiento", estacionamientoId));
    }

    /**
     * Copia al ticket el resultado del calculo de cobro.
     *
     * <p>Tambien conserva los parametros de tarifa usados en ese momento para
     * que cambios futuros de tarifa no modifiquen el historial del ticket.</p>
     */
    private void aplicarCobroAlTicket(Ticket ticket, TicketCobroResultado cobro) {
        ticket.setMinutosEstancia(cobro.minutosEstancia());
        ticket.setMontoTotal(cobro.montoTotal());
        ticket.setPrecioPorHoraAplicado(cobro.precioPorHoraAplicado());
        ticket.setMinutosToleranciaAplicados(cobro.minutosToleranciaAplicados());
        ticket.setCobrarFraccionAplicado(cobro.cobrarFraccionAplicado());
        ticket.setTarifaMinimaAplicada(cobro.tarifaMinimaAplicada());
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
     * Valida que el ticket se encuentre abierto antes de registrar salida.
     *
     * <p>Un ticket PENDIENTE_PAGO o CERRADO no puede registrar salida nuevamente
     * porque ya tiene fecha de salida y monto calculado.</p>
     */
    private void validarTicketAbierto(Ticket ticket) {
        if (ticket.getEstado() != EstadoTicket.ABIERTO) {
            throw new ConflictException("Solo se puede registrar salida para tickets en estado ABIERTO.");
        }
    }

    /**
     * Valida que el usuario autenticado pueda operar tickets del estacionamiento indicado.
     *
     * <p>ADMIN tiene alcance global, OWNER solo puede operar estacionamientos
     * donde sea dueño y OPERADOR solo estacionamientos asignados.</p>
     */
    private void validarUsuarioPuedeOperarTicket(
            Usuario usuarioAutenticado,
            Estacionamiento estacionamiento
    ) {
        if (tieneRol(usuarioAutenticado, ROL_ADMIN)) {
            return;
        }

        if (tieneRol(usuarioAutenticado, ROL_OWNER)
                && esOwnerDelEstacionamiento(usuarioAutenticado, estacionamiento)) {
            return;
        }

        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)
                && estaAsignadoAlEstacionamiento(usuarioAutenticado, estacionamiento)) {
            return;
        }

        throw new ConflictException("El usuario autenticado no puede operar tickets de este estacionamiento.");
    }

    /**
     * Indica si el usuario tiene asignado un rol por nombre.
     */
    private boolean tieneRol(Usuario usuario, String rolNombre) {
        return usuario.getRoles()
                .stream()
                .anyMatch(rol -> rolNombre.equals(rol.getNombre()));
    }

    /**
     * Indica si el usuario autenticado es owner del estacionamiento indicado.
     */
    private boolean esOwnerDelEstacionamiento(Usuario usuario, Estacionamiento estacionamiento) {
        return estacionamiento.getOwner() != null
                && estacionamiento.getOwner().getId().equals(usuario.getId());
    }

    /**
     * Indica si el usuario autenticado esta asignado al estacionamiento indicado.
     */
    private boolean estaAsignadoAlEstacionamiento(Usuario usuario, Estacionamiento estacionamiento) {
        return usuario.getEstacionamientos()
                .stream()
                .anyMatch(estacionamientoAsignado ->
                        estacionamientoAsignado.getId().equals(estacionamiento.getId())
                );
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
