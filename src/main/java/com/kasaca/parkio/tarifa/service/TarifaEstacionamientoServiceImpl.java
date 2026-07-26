package com.kasaca.parkio.tarifa.service;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import com.kasaca.parkio.tarifa.mapper.TarifaEstacionamientoMapper;
import com.kasaca.parkio.tarifa.repository.TarifaEstacionamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion del servicio de tarifas por estacionamiento.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarifaEstacionamientoServiceImpl implements TarifaEstacionamientoService {

    private final TarifaEstacionamientoRepository tarifaEstacionamientoRepository;
    private final EstacionamientoRepository estacionamientoRepository;
    private final TarifaEstacionamientoMapper tarifaEstacionamientoMapper;

    /**
     * Consulta la tarifa activa de un estacionamiento respetando el alcance del usuario autenticado.
     *
     * <p>ADMIN puede consultar cualquier tarifa activa. OWNER solo puede consultar
     * tarifas de estacionamientos donde sea dueño.</p>
     */
    @Override
    public TarifaEstacionamientoResponse getTarifaByEstacionamientoId(Long estacionamientoId, Jwt jwt) {
        validarAccesoAEstacionamiento(estacionamientoId, jwt);

        TarifaEstacionamiento tarifa = tarifaEstacionamientoRepository
                .findByEstacionamientoIdAndActivoTrue(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa del estacionamiento", estacionamientoId));

        return tarifaEstacionamientoMapper.toResponse(tarifa);
    }

    /**
     * Crea una tarifa activa para un estacionamiento.
     *
     * <p>Valida que el estacionamiento exista, que el usuario tenga acceso y que
     * no exista ya una tarifa activa para ese estacionamiento.</p>
     */
    @Override
    @Transactional
    public TarifaEstacionamientoResponse addTarifa(TarifaEstacionamientoRequest request, Jwt jwt) {
        Estacionamiento estacionamiento = validarAccesoAEstacionamiento(request.estacionamientoId(), jwt);

        if (tarifaEstacionamientoRepository.existsByEstacionamientoIdAndActivoTrue(request.estacionamientoId())) {
            throw new ConflictException(
                    "El estacionamiento con identificador '%s' ya tiene una tarifa activa"
                            .formatted(request.estacionamientoId())
            );
        }

        TarifaEstacionamiento tarifa = tarifaEstacionamientoMapper.toEntity(request, estacionamiento);
        TarifaEstacionamiento savedTarifa = tarifaEstacionamientoRepository.save(tarifa);

        return tarifaEstacionamientoMapper.toResponse(savedTarifa);
    }

    /**
     * Actualiza la tarifa activa asociada a un estacionamiento.
     *
     * <p>No permite cambiar la tarifa hacia otro estacionamiento. Por eso se valida
     * que el estacionamiento recibido en el body coincida con el path.</p>
     */
    @Override
    @Transactional
    public TarifaEstacionamientoResponse updateTarifa(
            Long estacionamientoId,
            TarifaEstacionamientoRequest request,
            Jwt jwt
    ) {
        if (!estacionamientoId.equals(request.estacionamientoId())) {
            throw new ConflictException(
                    "El estacionamiento del path no coincide con el estacionamiento del cuerpo de la solicitud"
            );
        }

        validarAccesoAEstacionamiento(estacionamientoId, jwt);

        TarifaEstacionamiento tarifa = tarifaEstacionamientoRepository
                .findByEstacionamientoIdAndActivoTrue(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa del estacionamiento", estacionamientoId));

        tarifaEstacionamientoMapper.updateEntity(request, tarifa);

        TarifaEstacionamiento updatedTarifa = tarifaEstacionamientoRepository.save(tarifa);

        return tarifaEstacionamientoMapper.toResponse(updatedTarifa);
    }

    /**
     * Desactiva logicamente la tarifa activa de un estacionamiento.
     *
     * <p>No elimina fisicamente el registro para conservar trazabilidad historica
     * y permitir futuras auditorias.</p>
     */
    @Override
    @Transactional
    public void deleteTarifa(Long estacionamientoId, Jwt jwt) {
        validarAccesoAEstacionamiento(estacionamientoId, jwt);

        TarifaEstacionamiento tarifa = tarifaEstacionamientoRepository
                .findByEstacionamientoIdAndActivoTrue(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa del estacionamiento", estacionamientoId));

        tarifa.setActivo(false);
        tarifaEstacionamientoRepository.save(tarifa);
    }

    /**
     * Valida que el estacionamiento exista y que el JWT tenga permiso para operar sobre el.
     *
     * <p>ADMIN tiene alcance global. OWNER solo tiene alcance sobre estacionamientos
     * donde su usuario sea el owner. Otros roles no pueden administrar tarifas.</p>
     */
    private Estacionamiento validarAccesoAEstacionamiento(Long estacionamientoId, Jwt jwt) {
        if (isAdmin(jwt)) {
            return estacionamientoRepository.findByIdAndActivoTrue(estacionamientoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", estacionamientoId));
        }

        if (isOwner(jwt)) {
            return estacionamientoRepository
                    .findByIdAndOwnerIdAndActivoTrue(estacionamientoId, extractUsuarioId(jwt))
                    .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", estacionamientoId));
        }

        throw new AccessDeniedException("No tienes permisos para administrar tarifas");
    }

    /**
     * Extrae el identificador del usuario autenticado desde el claim usuarioId del JWT.
     */
    private Long extractUsuarioId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("usuarioId") == null) {
            throw new AccessDeniedException("JWT sin claim usuarioId");
        }

        Number usuarioId = jwt.getClaim("usuarioId");
        return usuarioId.longValue();
    }

    /**
     * Indica si el JWT contiene el rol ADMIN.
     */
    private boolean isAdmin(Jwt jwt) {
        return hasRole(jwt, "ADMIN");
    }

    /**
     * Indica si el JWT contiene el rol OWNER.
     */
    private boolean isOwner(Jwt jwt) {
        return hasRole(jwt, "OWNER");
    }

    /**
     * Verifica si el claim roles contiene el rol solicitado.
     */
    private boolean hasRole(Jwt jwt, String role) {
        if (jwt == null || jwt.getClaimAsStringList("roles") == null) {
            return false;
        }

        return jwt.getClaimAsStringList("roles").contains(role);
    }
}
