package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.mapper.CajonMapper;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajonServiceImpl implements CajonService {

    private final CajonRepository cajonRepository;
    private final EstacionamientoRepository estacionamientoRepository;
    private final CajonMapper cajonMapper;

    @Override
    public List<CajonResponse> getCajones() {

        return cajonRepository.findAll()
                .stream()
                .map(cajonMapper::toResponseCajon)
                .toList();
    }

    @Override
    public List<CajonResponse> getCajonesByEstacionamientoId(Long estacionamientoId) {
        findEstacionamientoById(estacionamientoId);

        return cajonRepository
                .findByEstacionamientoId(estacionamientoId)
                .stream()
                .map(cajonMapper::toResponseCajon)
                .toList();
    }

    @Override
    public CajonResponse getCajon(Long id) {
        return cajonMapper.toResponseCajon(findCajonById(id));
    }

    @Override
    @Transactional
    public CajonResponse addCajon(CajonRequest request) {

        Estacionamiento estacionamiento = findEstacionamientoById(
                request.estacionamientoId()
        );

        // Se valida que no exista el cajon en el estacionamiento, de lo contario manda excepcion
        if (cajonRepository.existsByEstacionamientoIdAndNumero(request.estacionamientoId(),  request.numero())) {
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

    @Override
    @Transactional
    public CajonResponse updateCajon(Long id, CajonRequest request) {

        Cajon cajon = findCajonById(id);

        Estacionamiento estacionamiento = findEstacionamientoById(
                request.estacionamientoId()
        );

        if (cajonRepository
                .existsByEstacionamientoIdAndNumeroAndIdNot(
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

    @Override
    @Transactional
    public void deleteCajon(Long id) {
        Cajon cajon = findCajonById(id);
        cajonRepository.delete(cajon);
    }

    private Cajon findCajonById(Long id) {
        return cajonRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cajón",
                                id
                        )
                );
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
