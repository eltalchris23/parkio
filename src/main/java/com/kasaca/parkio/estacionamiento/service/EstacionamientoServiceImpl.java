package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.mapper.EstacionamientoMapper;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstacionamientoServiceImpl
        implements EstacionamientoService {

    private final EstacionamientoRepository estacionamientoRepository;
    private final EstacionamientoMapper estacionamientoMapper;

    @Override
    public List<EstacionamientoResponse> getEstacionamientos() {
        return estacionamientoRepository.findAll()
                .stream()
                .map(estacionamientoMapper::toResponse)
                .toList();
    }

    @Override
    public EstacionamientoResponse getEstacionamientoById(Long id) {
        return estacionamientoMapper.toResponse(
                findEstacionamientoById(id)
        );
    }

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

    @Override
    @Transactional
    public void deleteEstacionamiento(Long id) {
        Estacionamiento estacionamiento =
                findEstacionamientoById(id);

        estacionamientoRepository.delete(estacionamiento);
    }

    private Estacionamiento findEstacionamientoById(Long id) {
        return estacionamientoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estacionamiento",
                                id
                        )
                );
    }
}
