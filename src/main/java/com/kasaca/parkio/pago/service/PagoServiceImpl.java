package com.kasaca.parkio.pago.service;

import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.entity.Pago;
import com.kasaca.parkio.pago.mapper.PagoMapper;
import com.kasaca.parkio.pago.repository.PagoRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.ticket.repository.TicketRepository;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Implementacion de negocio para registrar pagos de tickets.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_OWNER = "OWNER";
    private static final String ROL_OPERADOR = "OPERADOR";

    private final PagoRepository pagoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajonRepository cajonRepository;
    private final PagoMapper pagoMapper;

    /**
     * Consulta pagos activos de forma paginada respetando el alcance del usuario autenticado.
     *
     * <p>ADMIN consulta todos los pagos. OWNER consulta pagos de sus estacionamientos.
     * OPERADOR consulta pagos de estacionamientos asignados. El listado general no se
     * expone para USER porque el cliente conserva la consulta puntual por ticket.</p>
     */
    @Override
    public PageResponse<PagoResponse> getPagos(
            Long usuarioAutenticadoId,
            Long estacionamientoId,
            MetodoPago metodoPago,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Pageable pageable
    ) {
        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);

        validarRangoFechas(fechaInicio, fechaFin);

        Page<Pago> pagos = findPagosByAlcance(
                usuarioAutenticado,
                estacionamientoId,
                metodoPago,
                fechaInicio,
                fechaFin,
                pageable
        );

        return PageResponse.from(pagos.map(pagoMapper::toResponse));
    }

    /**
     * Registra el pago de un ticket en estado PENDIENTE_PAGO.
     *
     * <p>Cuando el pago es valido, el ticket pasa a CERRADO y el cajon se libera.
     * El cambio se calcula en backend para que el frontend no altere importes.</p>
     */
    @Override
    @Transactional
    public PagoResponse registrarPago(Long usuarioAutenticadoId, PagoRequest request) {
        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Ticket ticket = findTicketById(request.ticketId());

        validarUsuarioPuedeOperarPago(usuarioAutenticado, ticket.getEstacionamiento());
        validarTicketPendientePago(ticket);
        validarTicketConMontoCalculado(ticket);
        validarTicketSinPago(ticket.getId());
        validarMontoRecibidoSuficiente(request.montoRecibido(), ticket.getMontoTotal());

        BigDecimal cambio = calcularCambio(request.montoRecibido(), ticket.getMontoTotal());
        LocalDateTime fechaActual = LocalDateTime.now();

        Pago pago = pagoMapper.toEntity(
                ticket,
                usuarioAutenticado,
                request.montoRecibido(),
                cambio,
                request.metodoPago(),
                fechaActual
        );

        ticket.setEstado(EstadoTicket.CERRADO);
        ticket.getCajon().setEstado(EstadoCajon.LIBRE);

        cajonRepository.save(ticket.getCajon());
        ticketRepository.save(ticket);

        Pago pagoGuardado = pagoRepository.save(pago);

        return pagoMapper.toResponse(pagoGuardado);
    }

    /**
     * Consulta el pago activo de un ticket respetando el alcance del usuario autenticado.
     */
    @Override
    public PagoResponse getPagoByTicketId(Long usuarioAutenticadoId, Long ticketId) {
        Usuario usuarioAutenticado = findUsuarioAutenticadoById(usuarioAutenticadoId);
        Ticket ticket = findTicketById(ticketId);

        validarUsuarioPuedeConsultarPago(usuarioAutenticado, ticket);

        Pago pago = pagoRepository.findByTicketIdAndActivoTrue(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago del ticket", ticketId));

        return pagoMapper.toResponse(pago);
    }

    /**
     * Busca el usuario autenticado activo usando el identificador extraido del JWT.
     */
    private Usuario findUsuarioAutenticadoById(Long usuarioAutenticadoId) {
        return usuarioRepository.findByIdAndActivoTrue(usuarioAutenticadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioAutenticadoId));
    }

    /**
     * Busca un ticket activo por identificador.
     */
    private Ticket findTicketById(Long ticketId) {
        return ticketRepository.findByIdAndActivoTrue(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
    }

    /**
     * Valida que el rango de fechas sea cronologicamente correcto cuando se envian ambos limites.
     */
    private void validarRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new ConflictException("La fecha de inicio no puede ser posterior a la fecha fin.");
        }
    }

    /**
     * Ejecuta la consulta paginada de pagos aplicando filtros dinamicos y alcance de seguridad.
     */
    private Page<Pago> findPagosByAlcance(
            Usuario usuarioAutenticado,
            Long estacionamientoId,
            MetodoPago metodoPago,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Pageable pageable
    ) {
        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)
                && usuarioAutenticado.getEstacionamientos().isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<Pago> specification = buildPagoSpecification(
                usuarioAutenticado,
                estacionamientoId,
                metodoPago,
                fechaInicio,
                fechaFin
        );

        return pagoRepository.findAll(specification, pageable);
    }

    /**
     * Construye la consulta dinamica para pagos.
     *
     * <p>Siempre se filtran pagos activos. Encima se agregan filtros opcionales
     * enviados por el frontend y finalmente se limita el resultado al alcance
     * permitido para el usuario autenticado.</p>
     */
    private Specification<Pago> buildPagoSpecification(
            Usuario usuarioAutenticado,
            Long estacionamientoId,
            MetodoPago metodoPago,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        Specification<Pago> specification = pagoActivo();

        if (estacionamientoId != null) {
            specification = specification.and(pagoDeEstacionamiento(estacionamientoId));
        }

        if (metodoPago != null) {
            specification = specification.and(pagoConMetodo(metodoPago));
        }

        if (fechaInicio != null) {
            specification = specification.and(pagoDesdeFecha(fechaInicio));
        }

        if (fechaFin != null) {
            specification = specification.and(pagoHastaFecha(fechaFin));
        }

        return specification.and(pagoDentroDelAlcance(usuarioAutenticado));
    }

    /**
     * Filtra solo pagos activos para respetar el borrado logico del proyecto.
     */
    private Specification<Pago> pagoActivo() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("activo"));
    }

    /**
     * Filtra pagos por estacionamiento usando la relacion Pago -> Ticket -> Estacionamiento.
     */
    private Specification<Pago> pagoDeEstacionamiento(Long estacionamientoId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("ticket").get("estacionamiento").get("id"), estacionamientoId);
    }

    /**
     * Filtra pagos por metodo de pago cuando el frontend envia EFECTIVO, TARJETA o TRANSFERENCIA.
     */
    private Specification<Pago> pagoConMetodo(MetodoPago metodoPago) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("metodoPago"), metodoPago);
    }

    /**
     * Filtra pagos registrados desde el inicio del dia indicado.
     */
    private Specification<Pago> pagoDesdeFecha(LocalDate fechaInicio) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("fechaPago"), fechaInicio.atStartOfDay());
    }

    /**
     * Filtra pagos registrados hasta el final del dia indicado.
     *
     * <p>Se usa menor que el inicio del dia siguiente para incluir cualquier
     * hora de la fecha final sin depender de milisegundos o nanos exactos.</p>
     */
    private Specification<Pago> pagoHastaFecha(LocalDate fechaFin) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("fechaPago"), fechaFin.plusDays(1).atStartOfDay());
    }

    /**
     * Aplica el alcance de seguridad sobre el listado de pagos.
     *
     * <p>ADMIN ve todo. OWNER ve pagos de estacionamientos donde sea owner.
     * OPERADOR ve pagos de estacionamientos asignados. USER no tiene listado
     * general para evitar exponerle pagos de otros clientes.</p>
     */
    private Specification<Pago> pagoDentroDelAlcance(Usuario usuarioAutenticado) {
        if (tieneRol(usuarioAutenticado, ROL_ADMIN)) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.conjunction();
        }

        if (tieneRol(usuarioAutenticado, ROL_OWNER)) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            root.get("ticket").get("estacionamiento").get("owner").get("id"),
                            usuarioAutenticado.getId()
                    );
        }

        if (tieneRol(usuarioAutenticado, ROL_OPERADOR)) {
            return (root, query, criteriaBuilder) ->
                    root.get("ticket").get("estacionamiento").in(usuarioAutenticado.getEstacionamientos());
        }

        throw new ConflictException("El usuario autenticado no puede listar pagos.");
    }

    /**
     * Valida que el ticket este listo para cobrarse.
     */
    private void validarTicketPendientePago(Ticket ticket) {
        if (ticket.getEstado() != EstadoTicket.PENDIENTE_PAGO) {
            throw new ConflictException("Solo se pueden pagar tickets en estado PENDIENTE_PAGO.");
        }
    }

    /**
     * Valida que el ticket tenga un monto calculado por el flujo de salida.
     */
    private void validarTicketConMontoCalculado(Ticket ticket) {
        if (ticket.getMontoTotal() == null) {
            throw new ConflictException("El ticket no tiene monto calculado para registrar el pago.");
        }
    }

    /**
     * Valida que no exista otro pago activo para el mismo ticket.
     */
    private void validarTicketSinPago(Long ticketId) {
        if (pagoRepository.existsByTicketIdAndActivoTrue(ticketId)) {
            throw new ConflictException("El ticket ya tiene un pago registrado.");
        }
    }

    /**
     * Valida que el monto recibido cubra el monto total del ticket.
     */
    private void validarMontoRecibidoSuficiente(BigDecimal montoRecibido, BigDecimal montoTotal) {
        if (montoRecibido.compareTo(montoTotal) < 0) {
            throw new ConflictException("El monto recibido es menor al monto total del ticket.");
        }
    }

    /**
     * Calcula el cambio que se debe regresar al cliente.
     */
    private BigDecimal calcularCambio(BigDecimal montoRecibido, BigDecimal montoTotal) {
        return montoRecibido.subtract(montoTotal);
    }

    /**
     * Valida que ADMIN, OWNER u OPERADOR pueda registrar pagos sobre el estacionamiento del ticket.
     */
    private void validarUsuarioPuedeOperarPago(Usuario usuarioAutenticado, Estacionamiento estacionamiento) {
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

        throw new ConflictException("El usuario autenticado no puede registrar pagos de este estacionamiento.");
    }

    /**
     * Valida que el usuario autenticado pueda consultar el pago del ticket.
     */
    private void validarUsuarioPuedeConsultarPago(Usuario usuarioAutenticado, Ticket ticket) {
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

        throw new ConflictException("El usuario autenticado no puede consultar este pago.");
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
}
