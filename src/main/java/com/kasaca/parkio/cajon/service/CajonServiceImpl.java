package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
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

    /**
     * Obtiene únicamente cajones activos para ocultar registros desactivados
     * mediante borrado lógico.
     */
    @Override
    public List<CajonResponse> getCajones() {

        return cajonRepository.findByActivoTrue()
                .stream()
                .map(cajonMapper::toResponseCajon)
                .toList();
    }

    /**
     * Obtiene los cajones activos de un estacionamiento activo.
     */
    @Override
    public List<CajonResponse> getCajonesByEstacionamientoId(Long estacionamientoId) {
        findEstacionamientoById(estacionamientoId);

        return cajonRepository
                .findByEstacionamientoIdAndActivoTrue(estacionamientoId)
                .stream()
                .map(cajonMapper::toResponseCajon)
                .toList();
    }

    /**
     * Consulta un cajón activo por identificador.
     */
    @Override
    public CajonResponse getCajon(Long id) {
        return cajonMapper.toResponseCajon(findCajonById(id));
    }

    /**
     * Crea un cajón dentro de un estacionamiento activo validando duplicados.
     */
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

    /**
     * Actualiza un cajón activo y valida que el nuevo número no esté duplicado
     * dentro del estacionamiento indicado.
     */
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

    /**
     * Actualiza el estado operativo de un cajón activo.
     */
    @Override
    @Transactional
    public CajonResponse updateEstado(Long id,CajonEstadoRequest request) {
        Cajon cajon = findCajonById(id);
        cajon.setEstado(request.estado());

        Cajon updatedCajon = cajonRepository.save(cajon);

        return cajonMapper.toResponseCajon(updatedCajon);
    }

    /**
     * Realiza el borrado lógico de un cajón activo cambiando su bandera activo a
     * false para conservar el registro por auditoría.
     */
    @Override
    @Transactional
    public void deleteCajon(Long id) {
        Cajon cajon = findCajonById(id);

        cajon.setActivo(false);
        cajonRepository.save(cajon);
    }

    /**
     * Busca internamente un cajón activo o lanza una excepción 404 cuando no existe
     * o fue desactivado mediante borrado lógico.
     */
    private Cajon findCajonById(Long id) {
        return cajonRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cajón",
                                id
                        )
                );
    }

    /**
     * Busca internamente un estacionamiento activo para impedir operar cajones
     * sobre estacionamientos desactivados.
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
