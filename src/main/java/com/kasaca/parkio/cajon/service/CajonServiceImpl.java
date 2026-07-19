package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.mapper.CajonMapper;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajonServiceImpl implements CajonService {

    private final CajonRepository cajonRepository;
    private final EstacionamientoRepository estacionamientoRepository;
    private final CajonMapper cajonMapper;

    /**
     * Obtiene cajones activos según el alcance del usuario autenticado.
     *
     * <p>ADMIN consulta todos los cajones activos. OWNER consulta solo los
     * cajones de sus estacionamientos. OPERADOR consulta solo los cajones de
     * estacionamientos asignados a su usuario. USER conserva por ahora la
     * consulta general.</p>
     */
    @Override
    public PageResponse<CajonResponse> getCajones(Pageable pageable, Jwt jwt) {
        Page<Cajon> cajones;

        if (isAdmin(jwt)) {
            cajones = cajonRepository.findByActivoTrue(pageable);
        } else if (isOwner(jwt)) {
            cajones = cajonRepository.findByEstacionamientoOwnerIdAndActivoTrue(
                    extractUsuarioId(jwt),
                    pageable
            );
        } else if (isOperador(jwt)) {
            cajones = cajonRepository.findByEstacionamientoUsuariosIdAndActivoTrue(
                    extractUsuarioId(jwt),
                    pageable
            );
        } else {
            cajones = cajonRepository.findByActivoTrue(pageable);
        }

        return PageResponse.from(
                cajones.map(cajonMapper::toResponseCajon)
        );
    }

    /**
     * Obtiene los cajones activos de un estacionamiento activo respetando el
     * alcance del usuario autenticado.
     */
    @Override
    public PageResponse<CajonResponse> getCajonesByEstacionamientoId(
            Long estacionamientoId,
            Pageable pageable,
            Jwt jwt
    ) {
        findEstacionamientoById(estacionamientoId, jwt);

        Page<CajonResponse> cajones = cajonRepository
                .findByEstacionamientoIdAndActivoTrue(estacionamientoId, pageable)
                .map(cajonMapper::toResponseCajon);

        return PageResponse.from(cajones);
    }

    /**
     * Consulta un cajón activo por identificador respetando el alcance del
     * usuario autenticado.
     */
    @Override
    public CajonResponse getCajon(Long id, Jwt jwt) {
        return cajonMapper.toResponseCajon(
                findCajonById(id, jwt)
        );
    }

    /**
     * Crea un cajón dentro de un estacionamiento activo validando permisos y
     * duplicados dentro del mismo estacionamiento.
     */
    @Override
    @Transactional
    public CajonResponse addCajon(CajonRequest request, Jwt jwt) {
        Estacionamiento estacionamiento = findEstacionamientoById(
                request.estacionamientoId(),
                jwt
        );

        if (cajonRepository.existsByEstacionamientoIdAndNumero(
                request.estacionamientoId(),
                request.numero()
        )) {
            throw new ConflictException(
                    "Ya existe el cajón '%s' en el estacionamiento '%s'"
                            .formatted(
                                    request.numero(),
                                    request.estacionamientoId()
                            )
            );
        }

        Cajon cajon = cajonMapper.toEntity(request, estacionamiento);
        Cajon savedCajon = cajonRepository.save(cajon);

        return cajonMapper.toResponseCajon(savedCajon);
    }

    /**
     * Actualiza un cajón activo validando permisos, estacionamiento destino y
     * duplicados dentro del estacionamiento indicado.
     */
    @Override
    @Transactional
    public CajonResponse updateCajon(Long id, CajonRequest request, Jwt jwt) {
        Cajon cajon = findCajonById(id, jwt);

        Estacionamiento estacionamiento = findEstacionamientoById(
                request.estacionamientoId(),
                jwt
        );

        if (cajonRepository.existsByEstacionamientoIdAndNumeroAndIdNot(
                request.estacionamientoId(),
                request.numero(),
                id
        )) {
            throw new ConflictException(
                    "Ya existe el cajón '%s' en el estacionamiento '%s'"
                            .formatted(
                                    request.numero(),
                                    request.estacionamientoId()
                            )
            );
        }

        cajonMapper.updateEntity(
                request,
                cajon,
                estacionamiento
        );

        Cajon updatedCajon = cajonRepository.save(cajon);

        return cajonMapper.toResponseCajon(updatedCajon);
    }

    /**
     * Actualiza el estado operativo de un cajón activo respetando el alcance del
     * usuario autenticado.
     */
    @Override
    @Transactional
    public CajonResponse updateEstado(Long id, CajonEstadoRequest request, Jwt jwt) {
        Cajon cajon = findCajonById(id, jwt);
        cajon.setEstado(request.estado());

        Cajon updatedCajon = cajonRepository.save(cajon);

        return cajonMapper.toResponseCajon(updatedCajon);
    }

    /**
     * Realiza el borrado lógico de un cajón activo respetando el alcance del
     * usuario autenticado.
     */
    @Override
    @Transactional
    public void deleteCajon(Long id, Jwt jwt) {
        Cajon cajon = findCajonById(id, jwt);

        cajon.setActivo(false);
        cajonRepository.save(cajon);
    }

    /**
     * Busca internamente un cajón activo aplicando el alcance del JWT.
     *
     * <p>ADMIN puede resolver cualquier cajón activo. OWNER solo puede resolver
     * cajones de estacionamientos donde su usuario sea owner. OPERADOR solo
     * puede resolver cajones de estacionamientos asignados a su usuario. USER
     * conserva la consulta general por ahora.</p>
     */
    private Cajon findCajonById(Long id, Jwt jwt) {
        if (isAdmin(jwt)) {
            return cajonRepository.findByIdAndActivoTrue(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cajón",
                                    id
                            )
                    );
        }

        if (isOwner(jwt)) {
            return cajonRepository.findByIdAndEstacionamientoOwnerIdAndActivoTrue(
                            id,
                            extractUsuarioId(jwt)
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cajón",
                                    id
                            )
                    );
        }

        if (isOperador(jwt)) {
            return cajonRepository.findByIdAndEstacionamientoUsuariosIdAndActivoTrue(
                            id,
                            extractUsuarioId(jwt)
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cajón",
                                    id
                            )
                    );
        }

        return cajonRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cajón",
                                id
                        )
                );
    }

    /**
     * Busca internamente un estacionamiento activo aplicando el alcance del JWT.
     *
     * <p>OWNER solo puede operar cajones dentro de sus propios estacionamientos.
     * OPERADOR solo puede operar sobre estacionamientos asignados.</p>
     */
    private Estacionamiento findEstacionamientoById(Long id, Jwt jwt) {
        if (isAdmin(jwt)) {
            return estacionamientoRepository.findByIdAndActivoTrue(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Estacionamiento",
                                    id
                            )
                    );
        }

        if (isOwner(jwt)) {
            return estacionamientoRepository.findByIdAndOwnerIdAndActivoTrue(
                            id,
                            extractUsuarioId(jwt)
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Estacionamiento",
                                    id
                            )
                    );
        }

        if (isOperador(jwt)) {
            return estacionamientoRepository.findByIdAndUsuariosIdAndActivoTrue(
                            id,
                            extractUsuarioId(jwt)
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Estacionamiento",
                                    id
                            )
                    );
        }

        return estacionamientoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estacionamiento",
                                id
                        )
                );
    }

    /**
     * Extrae el claim usuarioId del JWT emitido por Parkio.
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
     * Indica si el JWT contiene el rol OPERADOR.
     */
    private boolean isOperador(Jwt jwt) {
        return hasRole(jwt, "OPERADOR");
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
