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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Obtiene únicamente estacionamientos activos para ocultar registros
     * desactivados mediante borrado lógico.
     */
    @Override
    public PageResponse<EstacionamientoResponse> getEstacionamientos(Pageable pageable) {
        Page<EstacionamientoResponse> estacionamientos =
                estacionamientoRepository.findByActivoTrue(pageable)
                        .map(estacionamientoMapper::toResponse);

        return PageResponse.from(estacionamientos);
    }

    /**
     * Consulta un estacionamiento activo por identificador.
     */
    @Override
    public EstacionamientoResponse getEstacionamientoById(Long id) {
        return estacionamientoMapper.toResponse(
                findEstacionamientoById(id)
        );
    }

    /**
     * Crea un estacionamiento nuevo a partir del DTO de entrada.
     */
    @Override
    @Transactional
    public EstacionamientoResponse addEstacionamiento(
            EstacionamientoRequest request
    ) {
        Estacionamiento estacionamiento =
                estacionamientoMapper.toEntity(request);

        Estacionamiento savedEstacionamiento =
                estacionamientoRepository.save(estacionamiento);

        return estacionamientoMapper.toResponse(savedEstacionamiento);
    }

    /**
     * Actualiza los datos de un estacionamiento activo.
     */
    @Override
    @Transactional
    public EstacionamientoResponse updateEstacionamiento(
            Long id,
            EstacionamientoRequest request
    ) {
        Estacionamiento estacionamiento =
                findEstacionamientoById(id);

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
     * Realiza el borrado lógico del estacionamiento y de sus cajones activos.
     * Esto conserva la trazabilidad y evita dejar cajones activos dentro de un
     * estacionamiento desactivado.
     */
    @Override
    @Transactional
    public void deleteEstacionamiento(Long id) {
        Estacionamiento estacionamiento =
                findEstacionamientoById(id);

        List<Cajon> cajones = cajonRepository.findByEstacionamientoIdAndActivoTrue(id);

        cajones.forEach(cajon -> cajon.setActivo(false));
        estacionamiento.setActivo(false);

        cajonRepository.saveAll(cajones);
        estacionamientoRepository.save(estacionamiento);
    }

    /**
     * Busca internamente un estacionamiento activo o lanza una excepción 404
     * cuando no existe o fue desactivado mediante borrado lógico.
     */
    private Estacionamiento findEstacionamientoById(Long id) {
        return estacionamientoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estacionamiento",
                                id
                        )
                );
    }
}
