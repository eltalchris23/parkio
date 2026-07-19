package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.mapper.EstacionamientoMapper;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstacionamientoServiceImpl
        implements EstacionamientoService {

    private final EstacionamientoRepository estacionamientoRepository;
    private final CajonRepository cajonRepository;
    private final EstacionamientoMapper estacionamientoMapper;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene estacionamientos activos según el alcance del usuario autenticado.
     *
     * <p>ADMIN consulta todos los estacionamientos activos. OWNER consulta solo
     * los estacionamientos donde es dueño. OPERADOR consulta solo los
     * estacionamientos asignados a su usuario. USER conserva por ahora la regla
     * previa de consulta general.</p>
     */
    @Override
    public PageResponse<EstacionamientoResponse> getEstacionamientos(
            Pageable pageable,
            Jwt jwt
    ) {
        Page<Estacionamiento> estacionamientos;

        if (isAdmin(jwt)) {
            estacionamientos = estacionamientoRepository.findByActivoTrue(pageable);
        } else if (isOwner(jwt)) {
            estacionamientos = estacionamientoRepository.findByOwnerIdAndActivoTrue(
                    extractUsuarioId(jwt),
                    pageable
            );
        } else if (isOperador(jwt)) {
            estacionamientos = estacionamientoRepository.findByUsuariosIdAndActivoTrue(
                    extractUsuarioId(jwt),
                    pageable
            );
        } else {
            estacionamientos = estacionamientoRepository.findByActivoTrue(pageable);
        }

        return PageResponse.from(
                estacionamientos.map(estacionamientoMapper::toResponse)
        );
    }

    /**
     * Consulta un estacionamiento activo por identificador respetando el alcance
     * del usuario autenticado.
     */
    @Override
    public EstacionamientoResponse getEstacionamientoById(Long id, Jwt jwt) {
        return estacionamientoMapper.toResponse(
                findEstacionamientoById(id, jwt)
        );
    }

    /**
     * Crea un estacionamiento nuevo y asigna owner cuando el usuario autenticado
     * tiene rol OWNER.
     *
     * <p>El owner se toma del claim usuarioId del JWT validado por Spring
     * Security. ADMIN conserva la capacidad de crear estacionamientos sin owner
     * hasta implementar un flujo administrativo para asignar dueño explícito.</p>
     */
    @Override
    @Transactional
    public EstacionamientoResponse addEstacionamiento(
            EstacionamientoRequest request,
            Jwt jwt
    ) {
        Usuario owner = isOwner(jwt) && !isAdmin(jwt)
                ? findUsuarioActivoById(extractUsuarioId(jwt))
                : null;

        Estacionamiento estacionamiento =
                estacionamientoMapper.toEntity(request, owner);

        Estacionamiento savedEstacionamiento =
                estacionamientoRepository.save(estacionamiento);

        return estacionamientoMapper.toResponse(savedEstacionamiento);
    }

    /**
     * Actualiza los datos de un estacionamiento activo respetando el alcance
     * del usuario autenticado.
     */
    @Override
    @Transactional
    public EstacionamientoResponse updateEstacionamiento(
            Long id,
            EstacionamientoRequest request,
            Jwt jwt
    ) {
        Estacionamiento estacionamiento =
                findEstacionamientoById(id, jwt);

        estacionamientoMapper.updateEntity(
                request,
                estacionamiento
        );

        Estacionamiento updatedEstacionamiento =
                estacionamientoRepository.save(estacionamiento);

        return estacionamientoMapper.toResponse(
                updatedEstacionamiento
        );
    }

    /**
     * Realiza el borrado lógico del estacionamiento y de sus cajones activos
     * respetando el alcance del usuario autenticado.
     *
     * <p>Esto conserva la trazabilidad y evita dejar cajones activos dentro de
     * un estacionamiento desactivado.</p>
     */
    @Override
    @Transactional
    public void deleteEstacionamiento(Long id, Jwt jwt) {
        Estacionamiento estacionamiento =
                findEstacionamientoById(id, jwt);

        List<Cajon> cajones = cajonRepository.findByEstacionamientoIdAndActivoTrue(id);

        cajones.forEach(cajon -> cajon.setActivo(false));
        estacionamiento.setActivo(false);

        cajonRepository.saveAll(cajones);
        estacionamientoRepository.save(estacionamiento);
    }

    /**
     * Busca internamente un estacionamiento activo aplicando el alcance del JWT.
     *
     * <p>ADMIN puede resolver cualquier estacionamiento activo. OWNER solo puede
     * resolver estacionamientos donde su usuario sea el owner. OPERADOR solo
     * puede resolver estacionamientos asignados a su usuario. USER conserva la
     * consulta general por ahora.</p>
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
     * Busca un usuario activo por identificador para asignarlo como owner.
     */
    private Usuario findUsuarioActivoById(Long usuarioId) {
        return usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario",
                                usuarioId
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
