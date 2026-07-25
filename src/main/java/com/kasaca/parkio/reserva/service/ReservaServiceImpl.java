package com.kasaca.parkio.reserva.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.reserva.config.ReservaProperties;
import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.reserva.mapper.ReservaMapper;
import com.kasaca.parkio.reserva.repository.ReservaRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaServiceImpl implements ReservaService {

    private static final String PREFIJO_CODIGO_RESERVA = "RSV-";

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstacionamientoRepository estacionamientoRepository;
    private final CajonRepository cajonRepository;
    private final ReservaMapper reservaMapper;
    private final ReservaProperties reservaProperties;

    /**
     * Consulta las reservas activas de un usuario autenticado.
     *
     * <p>Este metodo permite que el cliente final consulte sus propias reservas
     * sin exponer reservas pertenecientes a otros usuarios.</p>
     */
    @Override
    public PageResponse<ReservaResponse> getReservasByUsuario(Long usuarioId, Pageable pageable) {
        Page<ReservaResponse> reservas = reservaRepository
                .findByUsuarioIdAndActivoTrue(usuarioId, pageable)
                .map(reservaMapper::toResponse);

        return PageResponse.from(reservas);
    }

    /**
     * Consulta una reserva activa por ID.
     *
     * <p>Si la reserva no existe o esta inactiva, se responde como recurso no encontrado.</p>
     */
    @Override
    public ReservaResponse getReservaById(Long id) {
        Reserva reserva = findReservaById(id);
        return reservaMapper.toResponse(reserva);
    }

    /**
     * Consulta una reserva activa por codigo publico.
     *
     * <p>Este codigo sera el dato que el cliente podra presentar al llegar al estacionamiento.</p>
     */
    @Override
    public ReservaResponse getReservaByCodigo(String codigo) {
        Reserva reserva = reservaRepository.findByCodigoAndActivoTrue(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", codigo));

        return reservaMapper.toResponse(reserva);
    }

    /**
     * Crea una reserva nueva para el usuario autenticado.
     *
     * <p>Valida usuario, estacionamiento, cajon, disponibilidad y reservas vigentes.
     * Despues calcula la expiracion desde configuracion y cambia el cajon a RESERVADO.</p>
     */
    @Override
    @Transactional
    public ReservaResponse crearReserva(Long usuarioId, ReservaRequest request) {
        LocalDateTime fechaActual = LocalDateTime.now();

        Usuario usuario = findUsuarioById(usuarioId);
        Estacionamiento estacionamiento = findEstacionamientoById(request.estacionamientoId());
        Cajon cajon = findCajonById(request.cajonId());

        validarCajonPerteneceAlEstacionamiento(cajon, estacionamiento.getId());
        validarCajonDisponible(cajon);
        validarSinReservaVigente(cajon.getId(), fechaActual);

        Integer minutosExpiracion = reservaProperties.expiracionMinutos();
        LocalDateTime fechaExpiracion = fechaActual.plusMinutes(minutosExpiracion);
        String codigo = generarCodigoReserva();

        Reserva reserva = reservaMapper.toEntity(
                codigo,
                request.placa(),
                fechaActual,
                fechaExpiracion,
                minutosExpiracion,
                usuario,
                estacionamiento,
                cajon
        );

        cajon.setEstado(EstadoCajon.RESERVADO);
        cajonRepository.save(cajon);

        Reserva reservaGuardada = reservaRepository.save(reserva);

        return reservaMapper.toResponse(reservaGuardada);
    }

    /**
     * Cancela una reserva activa perteneciente al usuario autenticado.
     *
     * <p>La cancelacion solo aplica para reservas en estado CREADA que aun no han
     * expirado. Al cancelar, la reserva pasa a CANCELADA y el cajon asociado se
     * libera cuando no exista otra reserva vigente para el mismo cajon.</p>
     */
    @Override
    @Transactional
    public ReservaResponse cancelarReserva(Long reservaId, Long usuarioId) {
        LocalDateTime fechaActual = LocalDateTime.now();

        Reserva reserva = findReservaById(reservaId);

        validarReservaPerteneceAlUsuario(reserva, usuarioId);
        validarReservaCreada(reserva);
        validarReservaVigente(reserva, fechaActual);

        reserva.setEstado(EstadoReserva.CANCELADA);

        liberarCajonSiNoTieneOtraReservaVigente(
                reserva.getCajon(),
                fechaActual,
                reserva.getId()
        );

        Reserva reservaGuardada = reservaRepository.save(reserva);

        return reservaMapper.toResponse(reservaGuardada);
    }

    /**
     * Expira reservas vencidas que sigan en estado CREADA.
     *
     * <p>Tambien intenta liberar sus cajones cuando ya no exista otra reserva vigente
     * sobre el mismo cajon. Mas adelante puede ejecutarse desde un scheduler.</p>
     */
    @Override
    @Transactional
    public int expirarReservasVencidas() {
        LocalDateTime fechaActual = LocalDateTime.now();

        var reservasVencidas = reservaRepository
                .findByEstadoAndFechaExpiracionBeforeAndActivoTrue(EstadoReserva.CREADA, fechaActual);

        reservasVencidas.forEach(reserva -> {
            reserva.setEstado(EstadoReserva.EXPIRADA);
            liberarCajonSiNoTieneOtraReservaVigente(
                    reserva.getCajon(),
                    fechaActual,
                    reserva.getId()
            );
        });

        reservaRepository.saveAll(reservasVencidas);

        return reservasVencidas.size();
    }

    /**
     * Busca una reserva activa por ID.
     */
    private Reserva findReservaById(Long id) {
        return reservaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
    }

    /**
     * Busca un usuario activo por ID.
     */
    private Usuario findUsuarioById(Long usuarioId) {
        return usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
    }

    /**
     * Busca un estacionamiento activo por ID.
     */
    private Estacionamiento findEstacionamientoById(Long estacionamientoId) {
        return estacionamientoRepository.findByIdAndActivoTrue(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", estacionamientoId));
    }

    /**
     * Busca un cajon activo por ID.
     */
    private Cajon findCajonById(Long cajonId) {
        return cajonRepository.findByIdAndActivoTrue(cajonId)
                .orElseThrow(() -> new ResourceNotFoundException("Cajon", cajonId));
    }

    /**
     * Valida que el cajon solicitado pertenezca al estacionamiento indicado.
     */
    private void validarCajonPerteneceAlEstacionamiento(Cajon cajon, Long estacionamientoId) {
        if (!cajon.getEstacionamiento().getId().equals(estacionamientoId)) {
            throw new ConflictException("El cajon no pertenece al estacionamiento indicado.");
        }
    }

    /**
     * Valida que el cajon este libre antes de apartarlo mediante reserva.
     */
    private void validarCajonDisponible(Cajon cajon) {
        if (cajon.getEstado() != EstadoCajon.LIBRE) {
            throw new ConflictException("El cajon no esta disponible para reservar.");
        }
    }

    /**
     * Valida que no exista una reserva vigente sobre el mismo cajon.
     */
    private void validarSinReservaVigente(Long cajonId, LocalDateTime fechaActual) {
        boolean existeReservaVigente = reservaRepository
                .existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrue(
                        cajonId,
                        EstadoReserva.CREADA,
                        fechaActual
                );

        if (existeReservaVigente) {
            throw new ConflictException("El cajon ya tiene una reserva vigente.");
        }
    }

    /**
     * Valida que la reserva pertenezca al usuario autenticado.
     *
     * <p>Si no pertenece al usuario, se responde como no encontrada para no revelar
     * la existencia de reservas ajenas.</p>
     */
    private void validarReservaPerteneceAlUsuario(Reserva reserva, Long usuarioId) {
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ResourceNotFoundException("Reserva", reserva.getId());
        }
    }

    /**
     * Valida que la reserva se encuentre en estado CREADA.
     *
     * <p>Una reserva CANCELADA, EXPIRADA o USADA ya no debe volver a cancelarse.</p>
     */
    private void validarReservaCreada(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.CREADA) {
            throw new ConflictException("Solo se pueden cancelar reservas en estado CREADA.");
        }
    }

    /**
     * Valida que la reserva no haya superado su fecha de expiracion.
     */
    private void validarReservaVigente(Reserva reserva, LocalDateTime fechaActual) {
        if (!reserva.getFechaExpiracion().isAfter(fechaActual)) {
            throw new ConflictException("La reserva ya expiro y no puede cancelarse.");
        }
    }

    /**
     * Genera un codigo publico de reserva.
     *
     * <p>El codigo se valida contra base de datos para reducir el riesgo de colision.</p>
     */
    private String generarCodigoReserva() {
        String codigo;

        do {
            codigo = PREFIJO_CODIGO_RESERVA + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (reservaRepository.findByCodigoAndActivoTrue(codigo).isPresent());

        return codigo;
    }

    /**
     * Libera el cajon si ya no existe otra reserva vigente sobre el mismo cajon.
     *
     * <p>Esta validacion evita liberar accidentalmente un cajon que ya pudo haber sido
     * reservado nuevamente despues de que una reserva anterior vencio.</p>
     */
    private void liberarCajonSiNoTieneOtraReservaVigente(
            Cajon cajon,
            LocalDateTime fechaActual,
            Long reservaId
    ) {
        boolean existeReservaVigente = reservaRepository
                .existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrueAndIdNot(
                        cajon.getId(),
                        EstadoReserva.CREADA,
                        fechaActual,
                        reservaId
                );

        if (!existeReservaVigente && cajon.getEstado() == EstadoCajon.RESERVADO) {
            cajon.setEstado(EstadoCajon.LIBRE);
            cajonRepository.save(cajon);
        }
    }
}
